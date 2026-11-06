package com.govai.citizen.service;

import com.govai.citizen.common.BizException;
import com.govai.citizen.common.Constants;
import com.govai.citizen.config.UserContext;
import com.govai.citizen.dto.ProgressVO;
import com.govai.citizen.entity.ApplicationRecord;
import com.govai.citizen.feign.ApprovalClient;
import com.govai.citizen.mapper.ApplicationRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final ApplicationRecordMapper recordMapper;
    private final ApprovalClient approvalClient;

    public ProgressVO getProgress(Long recordId) {
        Long uid = UserContext.requireUserId();
        ApplicationRecord r = recordMapper.selectById(recordId);
        if (r == null || !r.getUserId().equals(uid)) {
            throw new BizException(404, "申报记录不存在");
        }
        if (r.getApprovalInstanceId() == null) {
            // 无审批实例(提交未到达审批端/历史遗留): 本地时间线
            ProgressVO vo = new ProgressVO();
            vo.setStatus(r.getStatus());
            vo.setCurrentNode("提交");
            vo.setSubmitTime(r.getSubmitTime() == null ? null : r.getSubmitTime().toString());
            List<ProgressVO.Node> nodes = new ArrayList<>();
            nodes.add(node("提交", "DONE", r.getSubmitTime(), null));
            nodes.add(node("受理中", "CURRENT", null, null));
            vo.setNodes(nodes);
            return vo;
        }
        // 有真实审批实例: 一律真调审批端(本地状态只作兜底), 到达终态时回写本地
        try {
            Map<String, Object> data = approvalClient.progress(r.getApprovalInstanceId()).getData();
            if (data == null) {
                throw new RuntimeException("progress data empty");
            }
            ProgressVO vo = new ProgressVO();
            vo.setInstanceId(r.getApprovalInstanceId());
            String st = data.get("status") == null ? r.getStatus() : String.valueOf(data.get("status"));
            vo.setStatus(st);
            vo.setCurrentNode(data.get("currentNode") == null ? null : String.valueOf(data.get("currentNode")));
            vo.setSubmitTime(data.get("submitTime") == null ? null : String.valueOf(data.get("submitTime")));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawNodes = (List<Map<String, Object>>) data.get("nodes");
            if (rawNodes != null) {
                List<ProgressVO.Node> nodes = new ArrayList<>();
                for (Map<String, Object> n : rawNodes) {
                    ProgressVO.Node nd = new ProgressVO.Node();
                    nd.setName(n.get("name") == null ? null : String.valueOf(n.get("name")));
                    nd.setStatus(n.get("status") == null ? null : String.valueOf(n.get("status")));
                    nd.setHandleTime(n.get("handleTime") == null ? null : String.valueOf(n.get("handleTime")));
                    nd.setAssignee(n.get("assignee") == null ? null : String.valueOf(n.get("assignee")));
                    nodes.add(nd);
                }
                vo.setNodes(nodes);
            }
            // 审批端已终态(办结/驳回/退回)时回写本地状态, 保证列表 statusText 与评价入口同步
            if (isTerminal(st) && !st.equals(r.getStatus())) {
                r.setStatus(st);
                if (data.get("finishTime") != null) {
                    try {
                        r.setFinishTime(java.time.LocalDateTime.parse(
                                String.valueOf(data.get("finishTime")).substring(0, 19)));
                    } catch (Exception ignore) {
                        // finishTime 解析失败不阻塞状态回写
                    }
                }
                recordMapper.updateById(r);
            }
            return vo;
        } catch (Exception e) {
            // 审批端不可达时退化为本地时间线(含已办结本地状态), 不阻塞查看
            ProgressVO vo = new ProgressVO();
            vo.setInstanceId(r.getApprovalInstanceId());
            vo.setStatus(r.getStatus());
            vo.setCurrentNode(Constants.STATUS_APPROVED.equals(r.getStatus()) ? "已办结" : "审批中");
            vo.setSubmitTime(r.getSubmitTime() == null ? null : r.getSubmitTime().toString());
            List<ProgressVO.Node> nodes = new ArrayList<>();
            if (Constants.STATUS_APPROVED.equals(r.getStatus())) {
                nodes.add(node("提交", "DONE", r.getSubmitTime(), null));
                nodes.add(node("办结", "DONE", r.getFinishTime(), null));
            } else {
                nodes.add(node("提交", "DONE", r.getSubmitTime(), null));
                nodes.add(node("审批中", "CURRENT", null, null));
            }
            vo.setNodes(nodes);
            return vo;
        }
    }

    private boolean isTerminal(String status) {
        return Constants.STATUS_APPROVED.equals(status)
                || Constants.STATUS_REJECTED.equals(status)
                || "RETURNED".equals(status);
    }

    private ProgressVO.Node node(String name, String status, java.time.LocalDateTime handleTime, String assignee) {
        ProgressVO.Node n = new ProgressVO.Node();
        n.setName(name);
        n.setStatus(status);
        n.setHandleTime(handleTime == null ? null : handleTime.toString());
        n.setAssignee(assignee);
        return n;
    }
}
