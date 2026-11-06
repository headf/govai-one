package com.govai.citizen.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.govai.citizen.common.BizException;
import com.govai.citizen.common.PageResult;
import com.govai.citizen.dto.MatterDetailVO;
import com.govai.citizen.dto.MatterListVO;
import com.govai.citizen.dto.MatterQuery;
import com.govai.citizen.dto.MatterSaveReq;
import com.govai.citizen.entity.MatterCategory;
import com.govai.citizen.entity.MatterInfo;
import com.govai.citizen.mapper.MatterCategoryMapper;
import com.govai.citizen.mapper.MatterInfoMapper;
import com.govai.citizen.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatterService {

    private final MatterInfoMapper matterMapper;
    private final MatterCategoryMapper categoryMapper;

    public List<MatterCategory> categories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<MatterCategory>()
                .eq(MatterCategory::getParentId, 0)
                .orderByAsc(MatterCategory::getSortOrder));
    }

    public PageResult<MatterListVO> page(MatterQuery query) {
        int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int size = query.getSize() == null || query.getSize() < 1 ? 10 : Math.min(query.getSize(), 50);
        String keyword = StrUtil.trimToNull(query.getKeyword());
        List<MatterInfo> rows;
        long total;
        Map<Long, String> catName = loadCategoryNames();

        if (keyword != null) {
            List<MatterInfo> all = matterMapper.searchByKeyword(keyword, query.getCategoryId(), 1);
            total = all.size();
            int from = (page - 1) * size;
            rows = all.stream().skip(from).limit(size).collect(Collectors.toList());
        } else {
            LambdaQueryWrapper<MatterInfo> qw = new LambdaQueryWrapper<MatterInfo>()
                    .eq(query.getCategoryId() != null, MatterInfo::getCategoryId, query.getCategoryId())
                    .eq(MatterInfo::getStatus, 1)
                    .orderByAsc(MatterInfo::getSortOrder)
                    .orderByAsc(MatterInfo::getId);
            IPage<MatterInfo> p = matterMapper.selectPage(new Page<>(page, size), qw);
            total = p.getTotal();
            rows = p.getRecords();
        }
        List<MatterListVO> list = rows.stream().map(m -> {
            MatterListVO vo = new MatterListVO();
            vo.setId(m.getId());
            vo.setMatterCode(m.getMatterCode());
            vo.setMatterName(m.getMatterName());
            vo.setDepartment(m.getDepartment());
            vo.setDescription(m.getDescription());
            vo.setCategoryId(m.getCategoryId());
            vo.setCategoryName(catName.get(m.getCategoryId()));
            vo.setDeadlineDays(m.getDeadlineDays());
            return vo;
        }).collect(Collectors.toList());
        return new PageResult<>(list, total, page, size);
    }

    public MatterDetailVO detail(Long id) {
        MatterInfo m = matterMapper.selectById(id);
        if (m == null) {
            throw new BizException(404, "事项不存在");
        }
        MatterCategory cat = m.getCategoryId() == null ? null : categoryMapper.selectById(m.getCategoryId());
        MatterDetailVO vo = new MatterDetailVO();
        vo.setId(m.getId());
        vo.setMatterCode(m.getMatterCode());
        vo.setMatterName(m.getMatterName());
        vo.setCategoryId(m.getCategoryId());
        vo.setCategoryName(cat == null ? null : cat.getCategoryName());
        vo.setDepartment(m.getDepartment());
        vo.setDescription(m.getDescription());
        vo.setGuideContent(m.getGuideContent());
        vo.setConditions(JsonUtil.toList(m.getConditions()));
        vo.setMaterials(JsonUtil.toList(m.getMaterials()));
        vo.setProcessSteps(JsonUtil.toList(m.getProcessSteps()));
        vo.setDeadlineDays(m.getDeadlineDays());
        vo.setStatus(m.getStatus());
        return vo;
    }

    public Long save(MatterSaveReq req) {
        MatterInfo m = req.getId() == null ? new MatterInfo() : matterMapper.selectById(req.getId());
        if (req.getId() != null && m == null) {
            throw new BizException(404, "事项不存在");
        }
        m.setMatterCode(req.getMatterCode());
        m.setMatterName(req.getMatterName());
        m.setCategoryId(req.getCategoryId());
        m.setDepartment(req.getDepartment());
        m.setDescription(req.getDescription());
        m.setGuideContent(req.getGuideContent());
        m.setConditions(JsonUtil.fromList(req.getConditions()));
        m.setMaterials(JsonUtil.fromList(req.getMaterials()));
        m.setProcessSteps(JsonUtil.fromList(req.getProcessSteps()));
        m.setDeadlineDays(req.getDeadlineDays());
        m.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        m.setSortOrder(req.getSortOrder() == null ? 0 : req.getSortOrder());
        if (req.getId() == null) {
            if (matterMapper.selectCount(new LambdaQueryWrapper<MatterInfo>()
                    .eq(MatterInfo::getMatterCode, m.getMatterCode())) > 0) {
                throw new BizException("事项编码已存在");
            }
            matterMapper.insert(m);
        } else {
            matterMapper.updateById(m);
        }
        return m.getId();
    }

    public void delete(Long id) {
        matterMapper.deleteById(id);
    }

    private Map<Long, String> loadCategoryNames() {
        List<MatterCategory> cats = categoryMapper.selectList(null);
        Map<Long, String> map = new HashMap<>();
        for (MatterCategory c : cats) {
            map.put(c.getId(), c.getCategoryName());
        }
        return map;
    }
}
