package com.govai.supervision.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用分页查询请求参数
 * 所有列表接口均可继承此参数，前端统一传 page / size
 */
@Data
public class PageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页，从 1 开始 */
    private long page = 1;

    /** 每页条数，默认 10 */
    private long size = 10;

    public long getPage() {
        return page <= 0 ? 1 : page;
    }

    public long getSize() {
        return size <= 0 ? 10 : size;
    }
}