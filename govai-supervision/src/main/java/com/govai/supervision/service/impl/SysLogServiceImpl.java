package com.govai.supervision.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.govai.supervision.common.PageResult;
import com.govai.supervision.entity.SysLog;
import com.govai.supervision.mapper.SysLogMapper;
import com.govai.supervision.service.SysLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 系统操作日志服务实现
 */
@Service
@RequiredArgsConstructor
public class SysLogServiceImpl implements SysLogService {

    private final SysLogMapper sysLogMapper;

    @Override
    public PageResult<SysLog> pageLogs(long page, long size, String keyword, String begin, String end) {
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<SysLog>()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(SysLog::getUsername, keyword)
                        .or().like(SysLog::getOperation, keyword)
                        .or().like(SysLog::getMethod, keyword))
                .ge(StringUtils.hasText(begin), SysLog::getCreateTime, begin)
                .le(StringUtils.hasText(end), SysLog::getCreateTime, end)
                .orderByDesc(SysLog::getCreateTime);
        return PageResult.of(sysLogMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @Override
    public void record(SysLog log) {
        try {
            sysLogMapper.insert(log);
        } catch (Exception e) {
            // 日志写入失败不影响主流程
        }
    }

    @Override
    public void clearAll() {
        sysLogMapper.delete(null);
    }
}
