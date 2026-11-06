package com.govai.citizen.service;

import com.govai.citizen.dto.AiChatResp;
import org.springframework.stereotype.Service;

/**
 * 本地规则答复: 审批端 AI 服务不可用时, 按关键词给出常见政务问答,
 * 保证群众端 AI 助手入口单机可用。
 */
@Service
public class AiLocalService {

    public AiChatResp answer(String q) {
        String text = "";
        if (containsAny(q, "材料", "资料", "证件")) {
            text = "您好,一般政务服务事项需要:1.身份证原件及复印件;2.户口本/居住证明;3.事项对应的申请表与证明材料。具体以事项详情页的'所需材料'清单为准。";
        } else if (containsAny(q, "户口", "迁移")) {
            text = "户口迁移办理流程:1.在线提交申请;2.公安机关审核材料(约15个工作日);3.审核通过后办理迁移。您可在'我的申报'中实时查看进度。";
        } else if (containsAny(q, "社保", "转移")) {
            text = "社保关系转移:1.先在新参保地办理参保;2.在线申请转移;3.转出地审核后转入地接收。全程约20个工作日,进度可在'我的申报'查询。";
        } else if (containsAny(q, "医保", "报销")) {
            text = "医保报销需在定点医疗机构就诊后,凭身份证、医保卡、费用清单、诊断证明在线申请,审核通过后报销款将打入您的社保卡账户。";
        } else if (containsAny(q, "进度")) {
            text = "您可以在'我的-我的申报'中选择对应办件,查看当前办理环节与完整进度时间线。如有疑问可拨打12345热线咨询。";
        } else if (containsAny(q, "你好", "您好", "hi", "hello", "在吗")) {
            text = "您好!我是智慧政务AI助手,可以为您解答办事指南、所需材料、办理进度等问题,请输入您想咨询的内容。";
        } else {
            text = "您好,我是智慧政务AI助手。您可以咨询事项办理流程、所需材料、办理进度、政策信息等,例如'户口迁移需要哪些材料?'";
        }
        AiChatResp resp = new AiChatResp();
        resp.setResponse(text);
        resp.setTokenCount(0L);
        return resp;
    }

    private boolean containsAny(String q, String... keys) {
        if (q == null) {
            return false;
        }
        for (String k : keys) {
            if (q.contains(k)) {
                return true;
            }
        }
        return false;
    }
}
