package com.govai.enterprise.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.govai.enterprise.common.BusinessException;
import com.govai.enterprise.entity.EntMessage;
import com.govai.enterprise.mapper.EntMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 企业站内消息(办结/证照/政策提醒聚合 + 未读角标)
 */
@Service
@RequiredArgsConstructor
public class EntMessageService {

    private final EntMessageMapper messageMapper;

    public void send(Long enterpriseId, String msgType, String title, String content) {
        EntMessage m = new EntMessage();
        m.setEnterpriseId(enterpriseId);
        m.setMsgType(msgType);
        m.setTitle(title);
        m.setContent(content);
        m.setIsRead(0);
        messageMapper.insert(m);
    }

    public List<EntMessage> listByEnterprise(Long enterpriseId, String msgType) {
        return messageMapper.selectList(new LambdaQueryWrapper<EntMessage>()
                .eq(EntMessage::getEnterpriseId, enterpriseId)
                .eq(msgType != null && !msgType.isBlank(), EntMessage::getMsgType, msgType)
                .orderByDesc(EntMessage::getId)
                .last("limit 100"));
    }

    public long unreadCount(Long enterpriseId) {
        Long c = messageMapper.selectCount(new LambdaQueryWrapper<EntMessage>()
                .eq(EntMessage::getEnterpriseId, enterpriseId)
                .eq(EntMessage::getIsRead, 0));
        return c == null ? 0 : c;
    }

    public void markRead(Long enterpriseId, Long id) {
        EntMessage m = messageMapper.selectById(id);
        if (m == null || !m.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException("消息不存在");
        }
        if (m.getIsRead() == null || m.getIsRead() == 0) {
            m.setIsRead(1);
            messageMapper.updateById(m);
        }
    }

    public void readAll(Long enterpriseId) {
        messageMapper.update(null, new LambdaUpdateWrapper<EntMessage>()
                .eq(EntMessage::getEnterpriseId, enterpriseId)
                .eq(EntMessage::getIsRead, 0)
                .set(EntMessage::getIsRead, 1));
    }
}
