package com.govai.supervision.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.govai.supervision.common.BusinessException;
import com.govai.supervision.common.PageResult;
import com.govai.supervision.entity.SysDict;
import com.govai.supervision.mapper.SysDictMapper;
import com.govai.supervision.service.SysDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据字典服务实现
 */
@Service
@RequiredArgsConstructor
public class SysDictServiceImpl implements SysDictService {

    private final SysDictMapper sysDictMapper;

    @Override
    public PageResult<SysDict> pageDicts(long page, long size, String dictType, String keyword) {
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<SysDict>()
                .eq(StringUtils.hasText(dictType), SysDict::getDictType, dictType)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(SysDict::getDictLabel, keyword)
                        .or().like(SysDict::getDictValue, keyword)
                        .or().like(SysDict::getDictType, keyword))
                .orderByAsc(SysDict::getDictType)
                .orderByAsc(SysDict::getSortOrder);
        return PageResult.of(sysDictMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @Override
    public List<Map<String, Object>> groupByType() {
        List<SysDict> all = sysDictMapper.selectList(new LambdaQueryWrapper<SysDict>()
                .orderByAsc(SysDict::getDictType)
                .orderByAsc(SysDict::getSortOrder));
        Map<String, List<SysDict>> grouped = new LinkedHashMap<>();
        for (SysDict d : all) {
            grouped.computeIfAbsent(d.getDictType(), k -> new ArrayList<>()).add(d);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((type, items) -> {
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("dictType", type);
            group.put("items", items);
            result.add(group);
        });
        return result;
    }

    @Override
    public List<SysDict> listByType(String dictType) {
        if (!StringUtils.hasText(dictType)) {
            return java.util.Collections.emptyList();
        }
        return sysDictMapper.selectList(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getDictType, dictType)
                .eq(SysDict::getStatus, 1)
                .orderByAsc(SysDict::getSortOrder));
    }

    @Override
    public SysDict create(SysDict dict) {
        if (!StringUtils.hasText(dict.getDictType()) || !StringUtils.hasText(dict.getDictLabel())
                || !StringUtils.hasText(dict.getDictValue())) {
            throw new BusinessException(400, "类型、显示名、字典值不能为空");
        }
        Long cnt = sysDictMapper.selectCount(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getDictType, dict.getDictType())
                .eq(SysDict::getDictValue, dict.getDictValue()));
        if (cnt != null && cnt > 0) {
            throw new BusinessException(400, "该类型下已存在相同字典值");
        }
        dict.setId(null);
        if (dict.getSortOrder() == null) {
            dict.setSortOrder(0);
        }
        if (dict.getStatus() == null) {
            dict.setStatus(1);
        }
        sysDictMapper.insert(dict);
        return dict;
    }

    @Override
    public void update(SysDict dict) {
        if (dict.getId() == null || sysDictMapper.selectById(dict.getId()) == null) {
            throw new BusinessException(404, "字典项不存在");
        }
        sysDictMapper.updateById(dict);
    }

    @Override
    public void delete(Long id) {
        if (sysDictMapper.selectById(id) == null) {
            throw new BusinessException(404, "字典项不存在");
        }
        sysDictMapper.deleteById(id);
    }
}
