package com.govai.approval.feign;

import com.govai.approval.common.Result;
import com.govai.approval.dto.MatterVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 群众端 Feign 降级（开发阶段群众端未启动时返回 Mock）
 */
@Slf4j
@Component
public class CitizenClientFallback implements CitizenClient {

    @Override
    public Result<MatterVO> getMatter(Long id) {
        log.warn("群众端不可用，返回 Mock 事项 id={}", id);
        MatterVO mock = new MatterVO();
        mock.setId(id);
        mock.setMatterCode("MOCK-001");
        mock.setMatterName("Mock 事项（群众端未启动）");
        mock.setDepartment("Mock部门");
        mock.setGuideContent("群众端未启动，返回 Mock 数据");
        mock.setConditions(List.of("条件1", "条件2"));
        mock.setMaterials(List.of("材料1", "材料2"));
        mock.setDeadlineDays(15);
        return Result.success(mock);
    }

    @Override
    public Result<String> notifyCitizenStatus(Map<String, Object> body) {
        log.warn("群众端不可用，审批定稿通知已跳过 instanceId={}", body == null ? null : body.get("instanceId"));
        return Result.success(null);
    }
}
