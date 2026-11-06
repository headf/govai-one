package com.govai.citizen.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.govai.citizen.common.BizException;
import com.govai.citizen.common.Constants;
import com.govai.citizen.common.JwtUtils;
import com.govai.citizen.common.Result;
import com.govai.citizen.config.GovaiProperties;
import com.govai.citizen.dto.LoginReq;
import com.govai.citizen.dto.LoginVO;
import com.govai.citizen.dto.WxLoginReq;
import com.govai.citizen.entity.CitizenUser;
import com.govai.citizen.entity.SiteMessage;
import com.govai.citizen.feign.SupervisionClient;
import com.govai.citizen.mapper.CitizenUserMapper;
import com.govai.citizen.mapper.SiteMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CitizenUserMapper userMapper;
    private final GovaiProperties props;
    private final SupervisionClient supervisionClient;
    private final SiteMessageMapper siteMessageMapper;

    public LoginVO login(LoginReq req) {
        // SSO 模式(接入监管端认证中心): 改走短信登录拿统一 token, 不再本地自签
        if (!props.isMockLogin()) {
            return ssoLogin(req);
        }
        CitizenUser user = userMapper.selectOne(new LambdaQueryWrapper<CitizenUser>()
                .eq(CitizenUser::getPhone, req.getPhone())
                .last("limit 1"));
        if (user == null) {
            user = new CitizenUser();
            user.setPhone(req.getPhone());
            user.setRealName(req.getRealName());
            user.setIdCard(req.getIdCard());
            user.setAvatarUrl(req.getAvatarUrl());
            user.setStatus(1);
            userMapper.insert(user);
        } else if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException("账号已被禁用");
        }
        return issueToken(user);
    }

    public LoginVO wxLogin(WxLoginReq req) {
        // SSO 模式下本地无 sys_user 映射, 微信端暂不支持(仅手机号短信登录)
        if (!props.isMockLogin()) {
            throw new BizException("请使用手机号 + 验证码登录");
        }
        if (req.getOpenid() == null || req.getOpenid().isBlank()) {
            throw new BizException(400, "openid 不能为空");
        }
        CitizenUser user = userMapper.selectOne(new LambdaQueryWrapper<CitizenUser>()
                .eq(CitizenUser::getOpenid, req.getOpenid())
                .last("limit 1"));
        if (user == null) {
            user = new CitizenUser();
            user.setOpenid(req.getOpenid());
            user.setRealName(req.getNickname());
            user.setAvatarUrl(req.getAvatarUrl());
            user.setStatus(1);
            userMapper.insert(user);
        } else if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException("账号已被禁用");
        }
        return issueToken(user);
    }

    /**
     * SSO 模式登录: 调监管端认证中心短信登录, 拿到统一 token(userId=监管端 sys_user.id) 原样透传,
     * 本地 citizen_user 仅维护 sys_user_id 映射, 供 JwtInterceptor 回映射本地业务主键。
     */
    private LoginVO ssoLogin(LoginReq req) {
        Map<String, Object> body = new HashMap<>();
        body.put("phone", req.getPhone());
        body.put("code", req.getCode() == null ? "000000" : req.getCode());
        Result<Map<String, Object>> resp;
        try {
            resp = supervisionClient.smsLogin(body);
        } catch (Exception e) {
            throw new BizException("认证中心暂不可用，请稍后重试");
        }
        if (resp == null || resp.getCode() != 200 || resp.getData() == null) {
            throw new BizException(resp == null ? "认证中心登录失败" : resp.getMessage());
        }
        Map<String, Object> data = resp.getData();
        Object token = data.get("token");
        Object sysUid = data.get("userId");
        if (token == null || sysUid == null) {
            throw new BizException("认证中心返回异常，请重试");
        }
        Long sysUserId = Long.valueOf(String.valueOf(sysUid));

        CitizenUser user = userMapper.selectOne(new LambdaQueryWrapper<CitizenUser>()
                .eq(CitizenUser::getSysUserId, sysUserId)
                .last("limit 1"));
        if (user == null) {
            // 老手机号本地已有记录时复用, 补充 sys_user_id 映射
            user = userMapper.selectOne(new LambdaQueryWrapper<CitizenUser>()
                    .eq(CitizenUser::getPhone, req.getPhone())
                    .last("limit 1"));
        }
        if (user == null) {
            user = new CitizenUser();
            user.setPhone(req.getPhone());
            user.setSysUserId(sysUserId);
            user.setRealName(req.getRealName());
            user.setIdCard(req.getIdCard());
            user.setStatus(1);
            userMapper.insert(user);
        } else {
            if (user.getStatus() != null && user.getStatus() == 0) {
                throw new BizException("账号已被禁用");
            }
            if (user.getSysUserId() == null) {
                user.setSysUserId(sysUserId);
                userMapper.updateById(user);
            }
        }
        long expireIn = data.get("expireIn") == null
                ? props.getJwt().getExpireSeconds()
                : Long.parseLong(String.valueOf(data.get("expireIn")));
        LoginVO vo = new LoginVO();
        vo.setToken(String.valueOf(token)); // 透传监管端统一 token
        vo.setUserId(user.getId());
        vo.setUserName(user.getRealName() == null ? user.getPhone() : user.getRealName());
        vo.setRole(Constants.ROLE_CITIZEN);
        vo.setExpireIn(expireIn);
        vo.setPhone(user.getPhone());
        vo.setAvatarUrl(user.getAvatarUrl());
        sendWelcome(user.getId());
        return vo;
    }

    private LoginVO issueToken(CitizenUser user) {
        String token = JwtUtils.createToken(
                props.getJwt().getSecret(),
                String.valueOf(user.getId()),
                user.getRealName() == null ? user.getPhone() : user.getRealName(),
                Constants.ROLE_CITIZEN,
                props.getJwt().getExpireSeconds());
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUserName(user.getRealName() == null ? user.getPhone() : user.getRealName());
        vo.setRole(Constants.ROLE_CITIZEN);
        vo.setExpireIn(props.getJwt().getExpireSeconds());
        vo.setPhone(user.getPhone());
        vo.setAvatarUrl(user.getAvatarUrl());
        sendWelcome(user.getId());
        return vo;
    }

    /** 登录成功写一条系统欢迎消息(幂等弱化: 仅每账号首次登录发送一次, 避免每次打扰) */
    private void sendWelcome(Long uid) {
        long cnt = siteMessageMapper.selectCount(new LambdaQueryWrapper<SiteMessage>()
                .eq(SiteMessage::getUserId, uid)
                .eq(SiteMessage::getMessageType, Constants.MSG_SYSTEM));
        if (cnt > 0) {
            return;
        }
        SiteMessage m = new SiteMessage();
        m.setUserId(uid);
        m.setMessageType(Constants.MSG_SYSTEM);
        m.setTitle("欢迎使用智慧政务");
        m.setContent("您可以在线申报、跟踪办理进度；建议先完成实名认证，获取更精准的政策推荐。");
        m.setIsRead(0);
        siteMessageMapper.insert(m);
    }
}
