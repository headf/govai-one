package com.govai.supervision.service;

import com.govai.supervision.common.PageResult;
import com.govai.supervision.entity.SysDict;

import java.util.List;
import java.util.Map;

/**
 * 数据字典服务
 */
public interface SysDictService {

    /** 分页查询（可按类型/关键字） */
    PageResult<SysDict> pageDicts(long page, long size, String dictType, String keyword);

    /** 所有字典类型及其条目（分组，供前端做 tab/树） */
    List<Map<String, Object>> groupByType();

    /** 按类型查启用条目（供下拉） */
    List<SysDict> listByType(String dictType);

    /** 新增 */
    SysDict create(SysDict dict);

    /** 更新 */
    void update(SysDict dict);

    /** 删除 */
    void delete(Long id);
}
