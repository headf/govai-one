package com.govai.supervision.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.govai.supervision.common.BusinessException;
import com.govai.supervision.dto.auth.LoginReq;
import com.govai.supervision.dto.auth.LoginVO;
import com.govai.supervision.dto.auth.SmsLoginReq;
import com.govai.supervision.dto.auth.VerifyVO;
import com.govai.supervision.entity.SysRole;
import com.govai.supervision.entity.SysUser;
import com.govai.supervision.mapper.SysRoleMapper;
import com.govai.supervision.mapper.SysRolePermissionMapper;
import com.govai.supervision.mapper.SysUserMapper;
import com.govai.supervision.util.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SSO 认证服务
 * 契约 #1 统一登录 / 契约 #2 Token 验证
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final JwtUtils jwtUtils;

    /** 登录端类型与角色码一致对照 */
    private static final Map<String, String> LOGIN_TYPE_ROLE = Map.of(
            "ADMIN", "ADMIN",
            "SUPERVISOR", "SUPERVISOR",
            "APPROVER", "APPROVER",
            "CITIZEN", "CITIZEN",
            "ENTERPRISE", "ENTERPRISE");

    /** 密码编码器（BCrypt） */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 公民端短信快捷登录演示固定验证码 */
    private static final String SMS_MOCK_CODE = "000000";

    @Value("${govai.jwt.expire:604800}")
    private long expire;

    /** 统一登录：校验用户 + 密码，签发 JWT */
    public LoginVO login(LoginReq req) {
        // 1. 按用户名查用户
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, req.getUsername()));

        if (user == null) {
            throw new BusinessException(400, "用户名或密码错误");
        }
        // 2. 校验状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }
        // 3. 校验密码
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(400, "用户名或密码错误");
        }
        // 4. 查询角色编码
        String roleCode = resolveRoleCode(user.getRoleId());

        // 4.1 若指定登录端类型，校验与账号角色一致，防止跨端互登
        if (req.getLoginType() != null && !req.getLoginType().isBlank()) {
            String expect = LOGIN_TYPE_ROLE.get(req.getLoginType().trim().toUpperCase());
            if (expect == null) {
                throw new BusinessException(400, "非法的登录端类型: " + req.getLoginType());
            }
            if (!expect.equals(roleCode)) {
                throw new BusinessException(403, "该账号不属于所选登录端，请检查");
            }
        }

        // 5. 签发 Token(企业账号额外携带 enterpriseId,供企业端定位企业)
        java.util.Map<String, Object> extra = null;
        if ("ENTERPRISE".equals(roleCode) && user.getEnterpriseId() != null) {
            extra = new java.util.HashMap<>();
            extra.put("enterpriseId", user.getEnterpriseId());
        }
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), roleCode, extra);

        // 6. 携带该角色权限码 + 企业信息，供前端动态菜单/按钮/企业身份
        LoginVO vo = new LoginVO(token, user.getId(), user.getUsername(), roleCode, expire);
        vo.setAccount(user.getUsername());
        vo.setEnterpriseId(user.getEnterpriseId());
        vo.setPermissions(new java.util.ArrayList<>(permissionCodesOf(user.getRoleId())));
        return vo;
    }

    /** 公民端短信快捷登录：手机号 + 固定验证码(演示) → 仅 CITIZEN 账号 → 签发统一 token */
    public LoginVO smsLogin(SmsLoginReq req) {
        if (req.getCode() == null || !SMS_MOCK_CODE.equals(req.getCode().trim())) {
            throw new BusinessException(400, "验证码错误");
        }
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getPhone, req.getPhone())
                        .last("limit 1"));
        if (user == null) {
            throw new BusinessException(400, "该手机号未注册公民账号");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }
        String roleCode = resolveRoleCode(user.getRoleId());
        if (!"CITIZEN".equals(roleCode)) {
            throw new BusinessException(403, "该手机号未开通公民端登录权限");
        }
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), roleCode, null);
        LoginVO vo = new LoginVO(token, user.getId(), user.getUsername(), roleCode, expire);
        vo.setAccount(user.getUsername());
        vo.setPermissions(new java.util.ArrayList<>(permissionCodesOf(user.getRoleId())));
        return vo;
    }
    public Set<String> permissionCodesOf(Long roleId) {
        if (roleId == null) {
            return Collections.emptySet();
        }
        List<String> codes = sysRolePermissionMapper.selectPermissionCodesByRoleId(roleId);
        return codes == null ? Collections.emptySet() : Set.copyOf(codes);
    }

    /** 验证 Token：解析 + 用户状态复核 */
    public VerifyVO verify(String token) {
        Claims claims = jwtUtils.parseToken(token);
        if (claims == null) {
            throw new BusinessException(401, "Token 无效或已过期");
        }
        Long userId = Long.valueOf(claims.get("userId").toString());

        // 用户仍有效？
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || (user.getStatus() != null && user.getStatus() == 0)) {
            throw new BusinessException(401, "用户不存在或已禁用");
        }
        String role = claims.get("role") == null ? resolveRoleCode(user.getRoleId()) : claims.get("role").toString();
        return new VerifyVO(userId, claims.get("userName") == null ? user.getUsername() : claims.get("userName").toString(), role);
    }

    /** 通过 roleId 解析角色编码，找不到默认 CITIZEN */
    public String resolveRoleCode(Long roleId) {
        if (roleId == null) {
            return "CITIZEN";
        }
        SysRole role = sysRoleMapper.selectById(roleId);
        return role != null && role.getRoleCode() != null ? role.getRoleCode() : "CITIZEN";
    }
}