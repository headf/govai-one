package com.govai.enterprise.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.govai.enterprise.common.BusinessException;
import com.govai.enterprise.common.JwtUtils;
import com.govai.enterprise.dto.LoginReq;
import com.govai.enterprise.dto.LoginResp;
import com.govai.enterprise.entity.EnterpriseInfo;
import com.govai.enterprise.entity.EnterpriseUser;
import com.govai.enterprise.mapper.EnterpriseInfoMapper;
import com.govai.enterprise.mapper.EnterpriseUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 企业认证服务(登录/账号查询)
 * 统一 SSO：骨架契约 #1 由监管端签发 JWT(loginType=ENTERPRISE)。本端登录改为：
 *   1) 调监管端 POST /api/v1/auth/login 校验账号密码并拿 token(凭证主库=监管端 sys_user)；
 *   2) 本地按 username 回查 enterprise_user(角色档案: ADMIN/OPERATOR/实名/所属企业)组装登录响应。
 * enterprise_user 不再负责密码校验,仅作企业内角色档案与经办人管理数据源。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final EnterpriseUserMapper userMapper;
    private final EnterpriseInfoMapper enterpriseInfoMapper;
    private final JwtUtils jwtUtils;

    /** 监管端地址(统一 SSO 签发) */
    @Value("${govai.feign.supervision-url:http://localhost:8081}")
    private String supervisionUrl;

    /**
     * 登录：账号密码交由监管端校验,成功返回本端角色档案组装
     */
    public LoginResp login(LoginReq req) {
        // 1. 调监管端统一登录(loginType=ENTERPRISE 校验账号确实属于企业端)
        String token = callSupervisionLogin(req.getUsername(), req.getPassword());

        // 2. 回查本地企业角色档案
        EnterpriseUser user = userMapper.selectOne(
                new LambdaQueryWrapper<EnterpriseUser>()
                        .eq(EnterpriseUser::getUsername, req.getUsername()));
        if (user == null) {
            // 凭证在 sys_user 存在但本端无档案 → 阻断,避免幽灵账号
            throw new BusinessException("该企业账号未在本端建档,请联系企业法人");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用,请联系企业法人");
        }
        return assembleResp(user, token);
    }

    /**
     * 获取当前登录用户信息(用于拦截器校验后返回用户详情)
     * token 已是监管端签发;这里仅凭 sysUserId? → 改为按 userName 回查档案重建(拦截器已保证 token 合法)
     */
    public LoginResp current(Long userId) {
        EnterpriseUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        return assembleResp(user, null);
    }

    /**
     * 调监管端统一登录拿 token
     */
    private String callSupervisionLogin(String username, String password) {
        JSONObject body = new JSONObject();
        body.set("username", username);
        body.set("password", password);
        body.set("loginType", "ENTERPRISE");
        String url = supervisionUrl + "/api/v1/auth/login";
        String respBody;
        try {
            HttpResponse resp = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(5000)
                    .execute();
            respBody = resp.body();
        } catch (Exception e) {
            log.error("监管端统一登录服务不可用: {}", url, e);
            throw new BusinessException("认证服务不可用,请稍后再试");
        }
        JSONObject root = JSONUtil.parseObj(respBody);
        int code = root.getInt("code", -1);
        if (code != 200) {
            String msg = root.getStr("message");
            throw new BusinessException(StrUtil.blankToDefault(msg, "登录失败"));
        }
        JSONObject data = root.getJSONObject("data");
        if (data == null) {
            throw new BusinessException("登录响应异常");
        }
        return data.getStr("token");
    }

    /**
     * 组装登录响应(保持前端 LoginResp 字段语义: userId=enterprise_user.id, role=ADMIN/OPERATOR)
     */
    private LoginResp assembleResp(EnterpriseUser user, String token) {
        LoginResp resp = new LoginResp();
        resp.setUserId(user.getId());
        resp.setAccount(user.getUsername());
        resp.setUserName(user.getRealName());
        resp.setRole(user.getRole());
        resp.setEnterpriseId(user.getEnterpriseId());
        // token: 登录场景用监管端签发;current(/sys/me) 无新 token 时返回原(供刷新用户信息用,不覆盖本地 token)
        if (token != null) {
            resp.setToken(token);
        }
        resp.setExpireIn(604800L);
        if (user.getEnterpriseId() != null) {
            EnterpriseInfo info = enterpriseInfoMapper.selectById(user.getEnterpriseId());
            if (info != null) {
                // 企业已注销(enterprise_info.status=0): 阻断登录/用户信息查询
                if (Integer.valueOf(0).equals(info.getStatus())) {
                    throw new BusinessException(403, "该企业已注销,无法使用企业端服务");
                }
                resp.setEnterpriseName(info.getEnterpriseName());
            }
        }
        return resp;
    }

    /** 密码加密(供 DataInitializer 本地档案建档用——注意:登录凭证密码以监管端 sys_user 为准) */
    private static final org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder ENCODER =
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

    public String encodePassword(String raw) {
        return ENCODER.encode(raw);
    }
}
