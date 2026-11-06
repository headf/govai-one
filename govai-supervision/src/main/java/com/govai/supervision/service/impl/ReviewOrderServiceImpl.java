package com.govai.supervision.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.govai.supervision.common.BizNoGenerator;
import com.govai.supervision.common.BusinessException;
import com.govai.supervision.common.PageResult;
import com.govai.supervision.entity.ReviewOrder;
import com.govai.supervision.mapper.ReviewOrderMapper;
import com.govai.supervision.service.ReviewOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 差评整改工单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewOrderServiceImpl implements ReviewOrderService {

    private final ReviewOrderMapper reviewOrderMapper;

    @Override
    public void report(ReviewOrder order) {
        if (order == null) return;
        order.setId(null);                       // 忽略上报方传来的 id,由本端生成
        order.setReviewNo(BizNoGenerator.gen("XP"));   // 差评工单号
        order.setHandleStatus("PENDING");
        order.setCreateTime(LocalDateTime.now());
        order.setHandleOpinion(null);
        order.setHandlerName(null);
        order.setHandleTime(null);
        reviewOrderMapper.insert(order);
        log.info("收到差评上报:{} 来源={} 星级={} 事项={}",
                order.getReviewNo(), order.getSource(), order.getStarRating(), order.getMatterName());
    }

    @Override
    public PageResult<ReviewOrder> page(long page, long size, String handleStatus, String source) {
        LambdaQueryWrapper<ReviewOrder> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(handleStatus)) w.eq(ReviewOrder::getHandleStatus, handleStatus);
        if (StringUtils.hasText(source)) w.eq(ReviewOrder::getSource, source);
        w.orderByDesc(ReviewOrder::getId);
        Page<ReviewOrder> p = reviewOrderMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p);
    }

    @Override
    public long countPending() {
        Long c = reviewOrderMapper.selectCount(new LambdaQueryWrapper<ReviewOrder>()
                .eq(ReviewOrder::getHandleStatus, "PENDING"));
        return c == null ? 0L : c;
    }

    @Override
    public void handle(Long id, String opinion, String handlerName) {
        ReviewOrder order = reviewOrderMapper.selectById(id);
        if (order == null) throw new BusinessException(404, "差评工单不存在");
        if ("HANDLED".equals(order.getHandleStatus())) throw new BusinessException("该工单已处置，请勿重复操作");
        order.setHandleStatus("HANDLED");
        order.setHandleOpinion(opinion);
        order.setHandlerName(handlerName);
        order.setHandleTime(LocalDateTime.now());
        reviewOrderMapper.updateById(order);
    }
}
