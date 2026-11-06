package com.govai.citizen.feign;

import com.govai.citizen.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 审批端不可用时的 Mock 兜底: 提交返回假 instanceId, 进度返回模拟时间线, AI 返回本地规则答复。
 * 保证群众端单机可完整演示申报→进度→评价全流程。
 */
@Slf4j
@Component
public class ApprovalClientFallbackFactory implements FallbackFactory<ApprovalClient> {

    @Override
    public ApprovalClient create(Throwable cause) {
        log.warn("审批端服务不可用,启用 Mock 兜底: {}", cause.getMessage());
        return new ApprovalClient() {
            @Override
            public Result<Map<String, Object>> submit(Map<String, Object> req) {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("instanceId", 9000L + System.currentTimeMillis() % 1000);
                data.put("status", "APPROVING");
                return Result.success(data);
            }

            @Override
            public Result<Map<String, Object>> progress(Long instanceId) {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("instanceId", instanceId);
                data.put("status", "APPROVING");
                data.put("currentNode", "初审");
                data.put("submitTime", LocalDateTime.now().minusDays(1).toString());
                List<Map<String, Object>> nodes = new ArrayList<>();
                nodes.add(node("提交", "DONE", LocalDateTime.now().minusDays(1).toString(), null));
                nodes.add(node("初审", "CURRENT", null, "张审批"));
                nodes.add(node("复审", "PENDING", null, null));
                nodes.add(node("办结", "PENDING", null, null));
                data.put("nodes", nodes);
                return Result.success(data);
            }

            @Override
            public Result<Map<String, Object>> aiChat(Map<String, Object> req) {
                Object msg = req.get("message");
                String q = msg == null ? "" : String.valueOf(msg);
                String answer = localAnswer(q);
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("response", answer);
                data.put("tokenCount", 0);
                return Result.success(data);
            }
        };
    }

    private Map<String, Object> node(String name, String status, String handleTime, String assignee) {
        Map<String, Object> n = new LinkedHashMap<>();
        n.put("name", name);
        n.put("status", status);
        if (handleTime != null) {
            n.put("handleTime", handleTime);
        }
        if (assignee != null) {
            n.put("assignee", assignee);
        }
        return n;
    }

    private String localAnswer(String q) {
        if (q.contains("材料") || q.contains("资料")) {
            return "您好,一般政务服务事项需要:1.身份证原件及复印件;2.户口本/居住证明;3.事项对应的申请表与证明材料。具体以事项详情页的'所需材料'清单为准。";
        }
        if (q.contains("户口") || q.contains("迁移")) {
            return "户口迁移办理流程:1.在线提交申请;2.公安机关审核材料(约15个工作日);3.审核通过后办理迁移。您可在'我的申报'中实时查看进度。";
        }
        if (q.contains("社保") || q.contains("转移")) {
            return "社保关系转移:1.先在新参保地办理参保;2.在线申请转移;3.转出地审核后转入地接收。全程约20个工作日,进度可在'我的申报'查询。";
        }
        if (q.contains("医保") || q.contains("报销")) {
            return "医保报销需在定点医疗机构就诊后,凭身份证、医保卡、费用清单、诊断证明在线申请,审核通过后报销款将打入您的社保卡账户。";
        }
        if (q.contains("进度")) {
            return "您可以在'我的-我的申报'中选择对应办件,查看当前办理环节与完整进度时间线。如有疑问可拨打12345热线咨询。";
        }
        if (q.contains("你好") || q.contains("您好") || q.contains("hi")) {
            return "您好!我是智慧政务AI助手,可以为您解答办事指南、所需材料、办理进度等问题,请输入您想咨询的内容。";
        }
        return "您好,我是智慧政务AI助手。您可以咨询事项办理流程、所需材料、办理进度、政策信息等,例如'户口迁移需要哪些材料?'";
    }
}
