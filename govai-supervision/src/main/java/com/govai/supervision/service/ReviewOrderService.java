package com.govai.supervision.service;

import com.govai.supervision.common.PageResult;
import com.govai.supervision.entity.ReviewOrder;

/**
 * 差评整改工单服务
 */
public interface ReviewOrderService {

    /** 接收差评上报(群众端/企业端 Feign 调用):落一条待处置工单 */
    void report(ReviewOrder order);

    /** 工单分页查询(可按处置状态/来源筛选) */
    PageResult<ReviewOrder> page(long page, long size, String handleStatus, String source);

    /** 待处置数量(供工作台/大屏展示) */
    long countPending();

    /** 处置工单(记录整改意见与处置人) */
    void handle(Long id, String opinion, String handlerName);
}
