package com.govai.supervision.service;

import com.govai.supervision.common.PageResult;
import com.govai.supervision.entity.SupervisionSample;

/**
 * 抽样检查服务
 */
public interface SupervisionSampleService {

    /** 分页查询抽样记录 */
    PageResult<SupervisionSample> pageSamples(long page, long size, String sampleType, String checkResult);

    /** 随机抽样：从未被抽过的审批实例中随机抽取 count 个进行抽查 */
    void randomSample(int count, Long checkerId);

    /**
     * 随机抽样(支持信用差异化)
     * @param creditWeighted true 时按申报企业信用等级加权抽取:D级最优先 > C > B > A > 未知(需求8.2:差异化抽查)
     */
    void randomSample(int count, Long checkerId, boolean creditWeighted);

    /** 定向抽样：按实例号创建检查 */
    SupervisionSample targetedSample(Long instanceId, String checkResult, String checkDetail, Long checkerId);
}