package com.govai.enterprise.service;

import com.govai.enterprise.common.LoginUser;
import com.govai.enterprise.dto.EnterpriseBriefVO;
import com.govai.enterprise.dto.LegalPersonUpdateReq;
import com.govai.enterprise.entity.EnterpriseInfo;
import com.govai.enterprise.mapper.EnterpriseInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 企业信息服务(法人信息管理:查看/修改法定代表人)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnterpriseService {

    private final EnterpriseInfoMapper enterpriseInfoMapper;

    /**
     * 查询当前登录企业的信息
     */
    public EnterpriseInfo getMyEnterprise(LoginUser user) {
        return enterpriseInfoMapper.selectById(user.getEnterpriseId());
    }

    /**
     * 批量查询企业摘要(供监管端等跨服务只读调用,不依赖登录上下文)
     * @param ids 企业档案ID集合
     * @return id -> {enterpriseName, creditCode}
     */
    public Map<Long, EnterpriseBriefVO> batchBrief(List<Long> ids) {
        Map<Long, EnterpriseBriefVO> map = new LinkedHashMap<>();
        if (ids == null || ids.isEmpty()) {
            return map;
        }
        List<EnterpriseInfo> list = enterpriseInfoMapper.selectBatchIds(ids);
        if (list != null) {
            for (EnterpriseInfo info : list) {
                EnterpriseBriefVO vo = new EnterpriseBriefVO();
                vo.setEnterpriseId(info.getId());
                vo.setEnterpriseName(info.getEnterpriseName());
                vo.setCreditCode(info.getCreditCode());
                map.put(info.getId(), vo);
            }
        }
        return map;
    }

    /**
     * 修改法定代表人信息(姓名+身份证号)
     */
    @Transactional
    public void updateLegalPerson(LoginUser user, LegalPersonUpdateReq req) {
        EnterpriseInfo info = enterpriseInfoMapper.selectById(user.getEnterpriseId());
        if (info == null) {
            throw new IllegalArgumentException("企业信息不存在");
        }
        if (req.getLegalPerson() != null && !req.getLegalPerson().isBlank()) {
            info.setLegalPerson(req.getLegalPerson().trim());
        }
        if (req.getLegalPersonIdCard() != null && !req.getLegalPersonIdCard().isBlank()) {
            info.setLegalPersonIdCard(req.getLegalPersonIdCard().trim());
        }
        info.setUpdateTime(LocalDateTime.now());
        enterpriseInfoMapper.updateById(info);
        log.info("企业{}修改法定代表人信息: {} / {}", user.getEnterpriseId(), info.getLegalPerson(), info.getLegalPersonIdCard());
    }
}
