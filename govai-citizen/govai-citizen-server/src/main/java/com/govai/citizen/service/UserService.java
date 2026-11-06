package com.govai.citizen.service;

import com.govai.citizen.common.BizException;
import com.govai.citizen.common.Constants;
import com.govai.citizen.common.Result;
import com.govai.citizen.config.UserContext;
import com.govai.citizen.dto.UpdateProfileReq;
import com.govai.citizen.dto.UserProfileVO;
import com.govai.citizen.entity.CitizenUser;
import com.govai.citizen.entity.SiteMessage;
import com.govai.citizen.feign.SupervisionClient;
import com.govai.citizen.mapper.CitizenUserMapper;
import com.govai.citizen.mapper.SiteMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final CitizenUserMapper userMapper;
    private final SupervisionClient supervisionClient;
    private final PolicyService policyService;
    private final SiteMessageMapper siteMessageMapper;

    public UserProfileVO profile() {
        Long uid = UserContext.requireUserId();
        CitizenUser user = userMapper.selectById(uid);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        UserProfileVO vo = UserProfileVO.from(user);
        fillCredit(vo, user);
        return vo;
    }

    public UserProfileVO update(UpdateProfileReq req) {
        Long uid = UserContext.requireUserId();
        CitizenUser user = userMapper.selectById(uid);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        boolean wasVerified = user.getIdCard() != null && !user.getIdCard().isBlank();
        if (req.getRealName() != null) {
            user.setRealName(req.getRealName());
        }
        if (req.getIdCard() != null) {
            if (!req.getIdCard().matches("^\\d{17}[\\dXx]$")) {
                throw new BizException(400, "身份证号格式不正确");
            }
            user.setIdCard(req.getIdCard());
        }
        if (req.getAvatarUrl() != null) {
            user.setAvatarUrl(req.getAvatarUrl());
        }
        userMapper.updateById(user);

        // 首次完成实名: 更新政策画像并写系统站内信
        boolean justVerified = !wasVerified && user.getIdCard() != null && !user.getIdCard().isBlank();
        if (justVerified) {
            policyService.reseedForProfileChange(uid);
            SiteMessage m = new SiteMessage();
            m.setUserId(uid);
            m.setMessageType(Constants.MSG_SYSTEM);
            m.setTitle("实名认证成功");
            m.setContent("您已完成实名认证，政策推荐已按最新画像为您更新。");
            m.setIsRead(0);
            siteMessageMapper.insert(m);
        }
        UserProfileVO vo = UserProfileVO.from(user);
        fillCredit(vo, user);
        return vo;
    }

    /** 从监管端读取当前公民的个人信用(失败静默, 不阻塞个人资料) */
    private void fillCredit(UserProfileVO vo, CitizenUser user) {
        if (user.getSysUserId() == null) {
            return;
        }
        try {
            Result<Map<String, Object>> r = supervisionClient.personalCredit(user.getSysUserId());
            if (r != null && r.getCode() == 200 && r.getData() != null) {
                Map<String, Object> d = r.getData();
                if (d.get("creditNo") != null) {
                    vo.setCreditNo(String.valueOf(d.get("creditNo")));
                }
                if (d.get("creditScore") != null) {
                    vo.setCreditScore(new java.math.BigDecimal(String.valueOf(d.get("creditScore"))));
                }
                if (d.get("creditLevel") != null) {
                    vo.setCreditLevel(String.valueOf(d.get("creditLevel")));
                }
            }
        } catch (Exception ignored) {
            // 信用模块不可用不影响个人中心
        }
    }
}
