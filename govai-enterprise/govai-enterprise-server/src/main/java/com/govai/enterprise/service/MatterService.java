package com.govai.enterprise.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.govai.enterprise.client.LlmClient;
import com.govai.enterprise.common.BusinessException;
import com.govai.enterprise.common.PageResult;
import com.govai.enterprise.common.LoginUser;
import com.govai.enterprise.dto.MatterPlanReq;
import com.govai.enterprise.dto.MatterPlanResp;
import com.govai.enterprise.dto.MatterQuery;
import com.govai.enterprise.dto.MatterVO;
import com.govai.enterprise.entity.EntMatterCategory;
import com.govai.enterprise.entity.EntMatterInfo;
import com.govai.enterprise.entity.EntMatterPlanHistory;
import com.govai.enterprise.mapper.EntMatterCategoryMapper;
import com.govai.enterprise.mapper.EntMatterInfoMapper;
import com.govai.enterprise.mapper.EntMatterPlanHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatterService {

    private final EntMatterInfoMapper matterMapper;
    private final EntMatterCategoryMapper categoryMapper;
    private final LlmClient llmClient;
    private final EntMatterPlanHistoryMapper planHistoryMapper;

    public PageResult<MatterVO> pageQuery(MatterQuery query) {
        LambdaQueryWrapper<EntMatterInfo> wrapper = new LambdaQueryWrapper<EntMatterInfo>()
                .eq(EntMatterInfo::getStatus, 1);
        if (StrUtil.isNotBlank(query.getKeyword())) {
            wrapper.and(w -> w.like(EntMatterInfo::getMatterName, query.getKeyword())
                    .or().like(EntMatterInfo::getMatterCode, query.getKeyword()));
        }
        if (query.getCategoryId() != null) {
            wrapper.eq(EntMatterInfo::getCategoryId, query.getCategoryId());
        }
        wrapper.orderByAsc(EntMatterInfo::getId);

        Page<EntMatterInfo> page = matterMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()), wrapper);

        List<MatterVO> voList = page.getRecords().stream()
                .map(m -> MatterVO.from(m, categoryName(m.getCategoryId())))
                .collect(Collectors.toList());
        return PageResult.of(voList, page.getTotal(), query.getPage(), query.getSize());
    }

    public MatterVO detail(Long id) {
        EntMatterInfo matter = matterMapper.selectById(id);
        if (matter == null || (matter.getStatus() != null && matter.getStatus() == 0)) {
            throw new BusinessException("事项不存在或已下线");
        }
        return MatterVO.from(matter, categoryName(matter.getCategoryId()));
    }

    /**
     * 按事项编码查详情(供前端兑现/延续/变更等跨页跳转动态解析 id,避免硬编码)
     */
    public MatterVO detailByCode(String code) {
        if (StrUtil.isBlank(code)) {
            throw new BusinessException("事项编码不能为空");
        }
        EntMatterInfo matter = matterMapper.selectOne(new LambdaQueryWrapper<EntMatterInfo>()
                .eq(EntMatterInfo::getMatterCode, code)
                .last("LIMIT 1"));
        if (matter == null || (matter.getStatus() != null && matter.getStatus() == 0)) {
            throw new BusinessException("未找到事项编码:" + code);
        }
        return MatterVO.from(matter, categoryName(matter.getCategoryId()));
    }

    public List<EntMatterCategory> categories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<EntMatterCategory>()
                .orderByAsc(EntMatterCategory::getSortOrder));
    }

    /**
     * 智能办事规划:用户输入模糊需求,自动从事项库中挑选匹配事项并给出办理顺序。
     * 主路径 LLM + RAG,失败兜底本地关键词匹配。
     */
    public MatterPlanResp planMatters(LoginUser user, MatterPlanReq req) {
        String desc = StrUtil.isBlank(req.getDescription()) ? "" : req.getDescription().trim();
        String title = StrUtil.isBlank(req.getBusinessType())
                ? (desc.length() > 20 ? desc.substring(0, 20) + "…" : desc)
                : req.getBusinessType();

        List<EntMatterInfo> catalog = listOnlineMatters();

        MatterPlanResp resp = tryLlmPlan(desc, req.getHasLicense(), catalog);
        if (resp == null || resp.getItems() == null || resp.getItems().isEmpty()) {
            log.info("LLM 规划未命中,使用本地关键词兜底");
            resp = fallbackPlan(desc, req.getHasLicense(), catalog);
        }

        if (StrUtil.isBlank(resp.getTitle())) {
            resp.setTitle("「" + (title.isEmpty() ? "我的办事清单" : title) + "」办理清单");
        }
        if (resp.getTips() == null) {
            resp.setTips(new ArrayList<>());
        }
        // 保存规划历史(记忆功能)
        EntMatterPlanHistory h = new EntMatterPlanHistory();
        h.setEnterpriseId(user.getEnterpriseId());
        h.setUserId(user.getUserId());
        h.setDescription(desc);
        h.setHasLicense(Boolean.TRUE.equals(req.getHasLicense()) ? 1 : 0);
        h.setTitle(resp.getTitle());
        h.setSummary(resp.getSummary());
        h.setPlanJson(JSONUtil.toJsonStr(resp));
        planHistoryMapper.insert(h);
        resp.setHistoryId(h.getId());
        return resp;
    }

    /** 查询我的规划历史 */
    public List<EntMatterPlanHistory> listPlanHistory(LoginUser user) {
        return planHistoryMapper.selectList(
                new LambdaQueryWrapper<EntMatterPlanHistory>()
                        .eq(EntMatterPlanHistory::getEnterpriseId, user.getEnterpriseId())
                        .orderByDesc(EntMatterPlanHistory::getCreateTime));
    }

    /** 获取某条规划历史详情 */
    public MatterPlanResp getPlanHistory(LoginUser user, Long id) {
        EntMatterPlanHistory h = planHistoryMapper.selectById(id);
        if (h == null || !h.getEnterpriseId().equals(user.getEnterpriseId())) {
            throw new BusinessException("规划历史不存在");
        }
        MatterPlanResp resp = JSONUtil.toBean(h.getPlanJson(), MatterPlanResp.class);
        resp.setHistoryId(h.getId());
        return resp;
    }

    /** 标记规划完成(所有事项办结后确认) */
    public void completePlan(LoginUser user, Long id) {
        EntMatterPlanHistory h = planHistoryMapper.selectById(id);
        if (h == null || !h.getEnterpriseId().equals(user.getEnterpriseId())) {
            throw new BusinessException("规划历史不存在");
        }
        // 在 planJson 里追加 completed 标记
        JSONObject json = JSONUtil.parseObj(h.getPlanJson());
        json.set("completed", true);
        h.setPlanJson(json.toString());
        planHistoryMapper.updateById(h);
    }

    /** 删除规划历史 */
    public void deletePlan(LoginUser user, Long id) {
        EntMatterPlanHistory h = planHistoryMapper.selectById(id);
        if (h == null || !h.getEnterpriseId().equals(user.getEnterpriseId())) {
            throw new BusinessException("规划历史不存在");
        }
        planHistoryMapper.deleteById(id);
    }

    private MatterPlanResp tryLlmPlan(String desc, Boolean hasLicense, List<EntMatterInfo> catalog) {
        String catalogText = buildCatalogText(catalog);
        String system = """
                你是政务办事导航助手,帮助企业用户根据业务需求挑选需要办理的证照/许可事项并规划办理顺序。
                必须严格遵守:
                1. 只能从下方【可用事项库】中选择事项,禁止编造不存在的事项ID或名称。
                2. 办理顺序必须体现前置依赖:营业执照(企业开办)是一切的前提,有了营业执照才能办各类经营许可。
                3. 事项按"必需 → 建议 → 可选"排序,并用 status 字段标注 RECOMMENDED / OPTIONAL / SKIPPED。
                4. 如果用户已明确有营业执照(hasLicense=true),可跳过企业开办类事项。
                5. 直接输出纯 JSON,不要任何 markdown 代码块标记或额外文字。

                【可用事项库】
                """ + catalogText + """

                【输出 JSON 结构】
                {
                  "title": "规划标题",
                  "summary": "整体说明(含办理顺序建议,100字以内)",
                  "items": [
                    {
                      "matterId": 数字(必须来自上方事项库),
                      "order": 从1开始的整数,
                      "reason": "为什么需要办这个",
                      "prerequisite": true/false,
                      "status": "RECOMMENDED|OPTIONAL|SKIPPED"
                    }
                  ],
                  "tips": ["提示1", "提示2"]
                }
                """;

        String user = "用户需求:" + (desc.isBlank() ? "帮我规划一下食品销售/便利店这类小生意需要办的证照" : desc);
        if (hasLicense != null) {
            user += "\n是否已取得营业执照:" + (hasLicense ? "是,可以跳过企业开办" : "否");
        }

        String raw = llmClient.chat(system, new ArrayList<>(), user);
        if (StrUtil.isBlank(raw)) {
            return null;
        }

        try {
            String json = raw.trim();
            if (json.startsWith("```")) {
                int start = json.indexOf('{');
                int end = json.lastIndexOf('}');
                if (start >= 0 && end > start) {
                    json = json.substring(start, end + 1);
                }
            }
            JSONObject obj = JSONUtil.parseObj(json);
            return parseLlmPlan(obj, catalog);
        } catch (Exception e) {
            log.warn("LLM 返回解析失败,内容={}, err={}", StrUtil.maxLength(raw, 200), e.getMessage());
            return null;
        }
    }

    private MatterPlanResp parseLlmPlan(JSONObject obj, List<EntMatterInfo> catalog) {
        Set<Long> validIds = catalog.stream().map(EntMatterInfo::getId).collect(Collectors.toSet());
        Map<Long, EntMatterInfo> byId = catalog.stream()
                .collect(Collectors.toMap(EntMatterInfo::getId, m -> m, (a, b) -> a));

        MatterPlanResp resp = new MatterPlanResp();
        resp.setTitle(obj.getStr("title", "智能办事清单"));
        resp.setSummary(obj.getStr("summary", ""));

        List<MatterPlanResp.PlanItem> items = new ArrayList<>();
        JSONArray arr = obj.getJSONArray("items");
        if (arr != null) {
            for (int i = 0; i < arr.size(); i++) {
                JSONObject it = arr.getJSONObject(i);
                Long mid = it.getLong("matterId");
                if (mid == null || !validIds.contains(mid)) {
                    continue;
                }
                EntMatterInfo m = byId.get(mid);
                MatterPlanResp.PlanItem pi = buildPlanItem(m);
                Integer order = it.getInt("order");
                if (order == null) {
                    order = items.size() + 1;
                }
                pi.setOrder(order);
                pi.setReason(it.getStr("reason", ""));
                String status = it.getStr("status", "RECOMMENDED");
                if (!Arrays.asList("RECOMMENDED", "OPTIONAL", "SKIPPED").contains(status)) {
                    status = "RECOMMENDED";
                }
                pi.setStatus(status);
                Boolean pre = it.getBool("prerequisite");
                if (pre != null) {
                    pi.setPrerequisite(pre);
                }
                items.add(pi);
            }
        }
        items.sort((a, b) -> Integer.compare(a.getOrder(), b.getOrder()));
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setOrder(i + 1);
        }
        resp.setItems(items);

        JSONArray tipsArr = obj.getJSONArray("tips");
        List<String> tips = new ArrayList<>();
        if (tipsArr != null) {
            for (int i = 0; i < tipsArr.size(); i++) {
                tips.add(tipsArr.getStr(i));
            }
        }
        resp.setTips(tips);
        return resp;
    }

    private MatterPlanResp fallbackPlan(String desc, Boolean hasLicense, List<EntMatterInfo> catalog) {
        MatterPlanResp resp = new MatterPlanResp();
        resp.setTitle("智能办事清单");

        String d = desc == null ? "" : desc.toLowerCase();
        Set<Long> picked = new HashSet<>();

        // 1. 企业开办(除非明确已有执照)
        if (hasLicense == null || !hasLicense) {
            for (EntMatterInfo m : catalog) {
                if (m.getId() == 1L) {
                    picked.add(m.getId());
                    break;
                }
            }
        }

        // 2. 关键词匹配各类许可
        Map<String, Long[]> kwMap = new LinkedHashMap<>();
        kwMap.put("食品经营许可", new Long[]{2L, 6L, 7L});
        kwMap.put("食品", new Long[]{2L, 6L, 7L});
        kwMap.put("餐饮", new Long[]{2L, 6L, 7L, 11L, 12L});
        kwMap.put("卫生", new Long[]{11L, 12L});
        kwMap.put("公共场所", new Long[]{11L, 12L});
        kwMap.put("烟草", new Long[]{14L});
        kwMap.put("烟", new Long[]{14L});
        kwMap.put("药品", new Long[]{15L});
        kwMap.put("药", new Long[]{15L});
        kwMap.put("排污", new Long[]{16L});
        kwMap.put("环保", new Long[]{16L});
        kwMap.put("股权", new Long[]{17L});
        kwMap.put("经营期限", new Long[]{18L});
        kwMap.put("科技", new Long[]{3L});
        kwMap.put("高新技术", new Long[]{3L});

        for (Map.Entry<String, Long[]> e : kwMap.entrySet()) {
            if (d.contains(e.getKey()) || (desc != null && desc.contains(e.getKey()))) {
                picked.addAll(Arrays.asList(e.getValue()));
            }
        }

        // 3. 兜底:如果一个都没命中,默认推食品经营许可+卫生许可(演示最完整的链路)
        if (picked.isEmpty() || (picked.size() == 1 && picked.contains(1L))) {
            picked.addAll(Arrays.asList(2L, 11L));
        }

        Map<Long, EntMatterInfo> byId = catalog.stream()
                .collect(Collectors.toMap(EntMatterInfo::getId, m -> m, (a, b) -> a));

        // 4. 排序:先企业开办(1),再经营许可(id 从小到大保持事项库顺序)
        List<EntMatterInfo> list = picked.stream()
                .map(byId::get).filter(m -> m != null)
                .sorted((a, b) -> {
                    if (a.getId() == 1L) return -1;
                    if (b.getId() == 1L) return 1;
                    return Long.compare(a.getId(), b.getId());
                })
                .toList();

        List<MatterPlanResp.PlanItem> items = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            EntMatterInfo m = list.get(i);
            MatterPlanResp.PlanItem pi = buildPlanItem(m);
            pi.setOrder(i + 1);
            pi.setPrerequisite(m.getId() == 1L);
            pi.setReason(m.getId() == 1L ? "营业执照是开展一切经营活动的法律主体基础,必须最先取得"
                    : "根据您的业务描述,该许可为必备证照");
            pi.setStatus("RECOMMENDED");
            items.add(pi);
        }
        resp.setItems(items);

        resp.setSummary("根据您的需求,为您匹配以上事项。建议按顺序依次办理,前置事项办结后再启动后置事项。");
        resp.setTips(new ArrayList<>(Arrays.asList(
                "办理前请确保经营场所已落实,部分许可需要现场核查",
                "大部分材料可通过电子证照自动核验,无需重复提交(免证办)",
                "办理过程中遇到问题可使用「智能问答」咨询"
        )));
        return resp;
    }

    private MatterPlanResp.PlanItem buildPlanItem(EntMatterInfo m) {
        MatterPlanResp.PlanItem pi = new MatterPlanResp.PlanItem();
        pi.setMatterId(m.getId());
        pi.setMatterName(m.getMatterName());
        pi.setDepartment(m.getDepartment());
        pi.setGuide(StrUtil.maxLength(m.getGuideContent() == null ? "" : m.getGuideContent(), 120));
        pi.setMaterials(parseList(m.getMaterials()));
        pi.setProcess(parseList(m.getProcessSteps()));
        pi.setDeadlineDays(m.getDeadlineDays());
        pi.setPrerequisite(false);
        pi.setStatus("RECOMMENDED");
        return pi;
    }

    private List<String> parseList(String json) {
        if (StrUtil.isBlank(json) || "null".equalsIgnoreCase(json)) {
            return new ArrayList<>();
        }
        try {
            return JSONUtil.toList(json, String.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String buildCatalogText(List<EntMatterInfo> catalog) {
        StringBuilder sb = new StringBuilder();
        for (EntMatterInfo m : catalog) {
            sb.append("- id=").append(m.getId())
                    .append(" | 名称=").append(m.getMatterName())
                    .append(" | 分类=").append(categoryName(m.getCategoryId()) == null ? "" : categoryName(m.getCategoryId()))
                    .append(" | 部门=").append(m.getDepartment() == null ? "" : m.getDepartment())
                    .append(" | 指南=").append(StrUtil.maxLength(m.getGuideContent() == null ? "" : m.getGuideContent(), 80))
                    .append(" | 条件=").append(StrUtil.maxLength(m.getConditions() == null ? "" : m.getConditions(), 60))
                    .append(" | 时限=").append(m.getDeadlineDays() == null ? "" : m.getDeadlineDays() + "工作日")
                    .append("\n");
        }
        return sb.toString();
    }

    private List<EntMatterInfo> listOnlineMatters() {
        return matterMapper.selectList(new LambdaQueryWrapper<EntMatterInfo>()
                .eq(EntMatterInfo::getStatus, 1));
    }

    private String categoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        EntMatterCategory c = categoryMapper.selectById(categoryId);
        return c == null ? null : c.getCategoryName();
    }
}
