package com.govai.citizen.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.govai.citizen.common.BizException;
import com.govai.citizen.common.PageResult;
import com.govai.citizen.config.UserContext;
import com.govai.citizen.dto.MessageVO;
import com.govai.citizen.entity.SiteMessage;
import com.govai.citizen.mapper.SiteMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final SiteMessageMapper messageMapper;

    public PageResult<MessageVO> page(int page, int size, String type) {
        Long uid = UserContext.requireUserId();
        page = page < 1 ? 1 : page;
        size = size < 1 ? 10 : Math.min(size, 50);
        LambdaQueryWrapper<SiteMessage> qw = new LambdaQueryWrapper<SiteMessage>()
                .eq(SiteMessage::getUserId, uid)
                .eq(StrUtil.isNotBlank(type), SiteMessage::getMessageType, type)
                .orderByDesc(SiteMessage::getCreateTime);
        IPage<SiteMessage> p = messageMapper.selectPage(new Page<>(page, size), qw);
        List<MessageVO> list = p.getRecords().stream().map(MessageVO::from).collect(Collectors.toList());
        return new PageResult<>(list, p.getTotal(), page, size);
    }

    public long unreadCount() {
        Long uid = UserContext.requireUserId();
        return messageMapper.selectCount(new LambdaQueryWrapper<SiteMessage>()
                .eq(SiteMessage::getUserId, uid)
                .eq(SiteMessage::getIsRead, 0));
    }

    public MessageVO detail(Long id) {
        Long uid = UserContext.requireUserId();
        SiteMessage m = messageMapper.selectById(id);
        if (m == null || !m.getUserId().equals(uid)) {
            throw new BizException(404, "消息不存在");
        }
        if (m.getIsRead() == null || m.getIsRead() == 0) {
            m.setIsRead(1);
            m.setReadTime(LocalDateTime.now());
            messageMapper.updateById(m);
        }
        return MessageVO.from(m);
    }

    public void readAll() {
        Long uid = UserContext.requireUserId();
        messageMapper.update(null, new LambdaUpdateWrapper<SiteMessage>()
                .eq(SiteMessage::getUserId, uid)
                .eq(SiteMessage::getIsRead, 0)
                .set(SiteMessage::getIsRead, 1)
                .set(SiteMessage::getReadTime, LocalDateTime.now()));
    }
}
