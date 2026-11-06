package com.govai.enterprise.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.govai.enterprise.entity.EnterpriseUser;
import com.govai.enterprise.mapper.EnterpriseUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 启动时写入演示登录账号(幂等):
 *   ent_admin / 123456  -> 东软智膳(企业1) 管理员
 *   ent_operator / 123456 -> 东软智膳(企业1) 经办人
 *   yunqi_admin / 123456 -> 云启科技(企业2) 管理员
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final EnterpriseUserMapper userMapper;
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @Override
    public void run(ApplicationArguments args) {
        initUser("ent_admin", "123456", 1L, "郝总", "ADMIN", "13800138000");
        initUser("ent_operator", "123456", 1L, "小李", "OPERATOR", "13800138001");
        initUser("yunqi_admin", "123456", 2L, "王总", "ADMIN", "13900139000");
        log.info("[govai-enterprise] 演示账号初始化完成: ent_admin/123456, ent_operator/123456, yunqi_admin/123456");
    }

    private void initUser(String username, String rawPassword, Long enterpriseId,
                          String realName, String role, String phone) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<EnterpriseUser>().eq(EnterpriseUser::getUsername, username));
        if (count != null && count > 0) {
            return;
        }
        EnterpriseUser user = new EnterpriseUser();
        user.setUsername(username);
        user.setPassword(ENCODER.encode(rawPassword));
        user.setEnterpriseId(enterpriseId);
        user.setRealName(realName);
        user.setRole(role);
        user.setPhone(phone);
        user.setStatus(1);
        userMapper.insert(user);
    }
}
