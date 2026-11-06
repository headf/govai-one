package com.govai.supervision.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.govai.supervision.common.BusinessException;
import com.govai.supervision.common.Result;
import com.govai.supervision.entity.ApprovalDataCache;
import com.govai.supervision.entity.CreditPersonal;
import com.govai.supervision.entity.SysUser;
import com.govai.supervision.mapper.ApprovalDataCacheMapper;
import com.govai.supervision.mapper.SysUserMapper;
import com.govai.supervision.service.CreditPersonalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 公民个人画像(监管视角，走监管端登录 JWT)
 * 按 sys_user.id 聚合：公民基本信息 + 个人信用 + 办件历史(来自审批台账 approval_data_cache)
 */
@Tag(name = "公民个人画像", description = "监管人员按公民查看个人办件历史 + 信用档案")
@RestController
@RequestMapping("/api/v1/person-profile")
@RequiredArgsConstructor
public class PersonProfileController {

    private final SysUserMapper sysUserMapper;
    private final ApprovalDataCacheMapper approvalDataCacheMapper;
    private final CreditPersonalService creditPersonalService;

    @Operation(summary = "公民个人画像")
    @GetMapping("/{userId}")
    public Result<Map<String, Object>> profile(@PathVariable Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "公民不存在");
        }
        CreditPersonal credit = creditPersonalService.getByUserId(userId);
        List<ApprovalDataCache> records = approvalDataCacheMapper.selectList(
                new LambdaQueryWrapper<ApprovalDataCache>()
                        .eq(ApprovalDataCache::getApplicantUserId, userId)
                        .orderByDesc(ApprovalDataCache::getSubmitTime)
                        .last("limit 50"));

        Map<String, Object> creditVo = null;
        if (credit != null) {
            creditVo = new LinkedHashMap<>();
            creditVo.put("creditNo", credit.getCreditNo());
            creditVo.put("creditScore", credit.getCreditScore());
            creditVo.put("creditLevel", credit.getCreditLevel());
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", userId);
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("phone", user.getPhone());
        data.put("credit", creditVo);
        data.put("records", records);
        return Result.success(data);
    }
}
