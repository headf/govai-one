package com.govai.enterprise.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.govai.enterprise.common.BusinessException;
import com.govai.enterprise.common.LoginUser;
import com.govai.enterprise.common.PageResult;
import com.govai.enterprise.dto.QaAskReq;
import com.govai.enterprise.dto.QaAskResp;
import com.govai.enterprise.dto.QaHistoryVO;
import com.govai.enterprise.dto.QaSessionVO;
import com.govai.enterprise.entity.EntMatterInfo;
import com.govai.enterprise.entity.EntPolicyMatch;
import com.govai.enterprise.entity.EntQaRecord;
import com.govai.enterprise.entity.EntQaSession;
import com.govai.enterprise.mapper.EntMatterInfoMapper;
import com.govai.enterprise.mapper.EntPolicyMatchMapper;
import com.govai.enterprise.mapper.EntQaRecordMapper;
import com.govai.enterprise.mapper.EntQaSessionMapper;
import com.govai.enterprise.client.LlmClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 智能问答服务(本地知识库关键词检索;联调期可切审批端 /api/v1/ai/chat 大模型)
 * 支持会话维度:每次提问归属一个会话,历史可按会话切换恢复。
 */
@Service
@RequiredArgsConstructor
public class QaService {

    private final EntMatterInfoMapper matterMapper;
    private final EntPolicyMatchMapper policyMapper;
    private final EntQaRecordMapper qaRecordMapper;
    private final EntQaSessionMapper qaSessionMapper;
    private final LlmClient llmClient;

    /** 会话标题最大长度 */
    private static final int TITLE_MAX = 20;

    /** 本地 FAQ 知识库(演示用) */
    private static final List<Faq> FAQS = Arrays.asList(
            new Faq("如何开办一家公司(企业开办)",
                    Arrays.asList("开办", "开公司", "营业执照", "企业开办", "注册公司", "刻章", "税务登记", "社保登记"),
                    "企业开办已整合为「企业开办一件事」,一次申报即可并联办理营业执照核发、印章刻制、税务登记、社保登记四环节,承诺1个工作日办结。您可在大厅选择该事项,填写申报表并上传营业执照(支持OCR拍照回填)。",
                    "企业开办一件事 · 办理指南",
                    Arrays.asList(1L)),
            new Faq("食品经营许可证如何办理",
                    Arrays.asList("食品", "餐饮", "餐厅", "食品经营许可", "健康证", "开店"),
                    "从事食品销售、餐饮服务的经营者需取得食品经营许可证。需满足:已取得营业执照、经营场所卫生达标、从业人员持有效健康证明。承诺10个工作日办结,支持电子证照「免证办」自动核验营业执照。",
                    "食品经营许可新办 · 办理指南",
                    Arrays.asList(2L)),
            new Faq("高新技术企业认定条件与流程",
                    Arrays.asList("高新技术", "高企", "科技", "研发", "专利", "认定"),
                    "高新技术企业认定需满足:成立满一年、拥有核心自主知识产权、研发费用占比达标。流程为提交申请→专家评审→认定公示→证书颁发,承诺45个工作日。认定后可减按15%缴纳企业所得税。",
                    "高新技术企业认定 · 办理指南",
                    Arrays.asList(3L)),
            new Faq("小规模纳税人增值税减免(免申即享)",
                    Arrays.asList("增值税", "税收减免", "小规模", "免税", "税务", "减免"),
                    "小规模纳税人符合条件可享受增值税减免,且支持「免申即享」:系统自动识别匹配,企业在线确认即可,无需提交申请。可前往「政策服务」查看匹配政策并一键确认。",
                    "小规模纳税人增值税减免确认 · 政策",
                    Arrays.asList(4L)),
            new Faq("什么是免证办(电子证照自动带证)",
                    Arrays.asList("免证办", "自动带证", "带证"),
                    "「免证办」指申报时系统自动从企业电子证照库核验有效证照,免于重复上传。营业执照、许可证等材料会自动匹配带出,您无需重复提交。可在「企业证照」查看名下全部电子证照。",
                    "企业电子证照 · 免证办",
                    Arrays.asList(1L, 2L)),
            new Faq("电子证照即将到期怎么办",
                    Arrays.asList("到期", "过期", "快到期", "即将到期", "有效期", "续期", "提醒"),
                    "系统会在证照到期前主动提醒。您可前往「企业证照」查看证照有效期,即将到期的证照会高亮「即将到期」标签,请及时办理续期,避免影响后续申报审批。",
                    "企业电子证照 · 到期提醒",
                    new ArrayList<>()),
            new Faq("一件事套餐与一表申报是什么",
                    Arrays.asList("套餐", "一件事", "一表申报", "一表申请", "并联", "打包"),
                    "「一件事」套餐将多个关联事项打包,如「开办餐饮公司一件事」整合企业开办与食品经营许可。选择套餐后系统合并表单为一张表(共用字段只填一次)、材料去重,提交后自动拆分为多个事项独立审批。",
                    "一件事套餐 · 一表申报",
                    Arrays.asList(1L, 2L)),
            new Faq("政策免申即享如何操作",
                    Arrays.asList("免申即享", "政策", "补贴", "返还", "优惠", "确认"),
                    "系统根据企业画像自动匹配您可能符合的惠企政策,在「政策服务」中展示匹配度。点击「确认享受」即可,无需提交申请,权益同步生效。",
                    "政策服务 · 免申即享",
                    new ArrayList<>()),
            new Faq("如何查询申报进度",
                    Arrays.asList("进度", "审批", "办到哪", "查询进度", "状态"),
                    "提交申报后,可在「我的申报」中查看每条申报的审批进度时间轴(提交→初审→复审→办结)。审批状态变更也会在进度页实时展示。",
                    "我的申报 · 审批进度",
                    new ArrayList<>()),
            new Faq("申报材料如何上传与智能填表",
                    Arrays.asList("材料", "上传", "拍照", "OCR", "识别", "填表"),
                    "申报时按材料清单上传文件即可;支持拍照上传营业执照等证照,OCR自动识别并回填表单字段(智能填表)。标记「电子证照自动带出」的材料无需上传,系统自动核验。",
                    "智能申报 · OCR 回填",
                    new ArrayList<>()),
            new Faq("如何评价办事体验(好差评)",
                    Arrays.asList("评价", "好差评", "差评", "打分", "满意度"),
                    "办结后您可对本次办事体验进行1-5星好差评评分,并填写文字评价和快捷标签。差评会自动记录并反馈至办理部门整改。",
                    "好差评 · 服务评价",
                    new ArrayList<>()),
            new Faq("草稿如何保存与继续填写",
                    Arrays.asList("草稿", "草稿箱", "暂存", "继续填写"),
                    "填写申报表时可随时「保存草稿」,草稿保存在「草稿箱」。后续可在草稿箱中点击「继续编辑」恢复填写,或删除不再需要的草稿。",
                    "草稿箱 · 暂存恢复",
                    new ArrayList<>())
    );

    // ============ 会话管理 ============

    /**
     * 新建会话(返回会话ID;首条提问后标题会被更新为问题摘要)
     */
    @Transactional
    public QaSessionVO createSession(LoginUser user) {
        EntQaSession session = new EntQaSession();
        session.setEnterpriseId(user.getEnterpriseId());
        session.setUserId(user.getUserId());
        session.setTitle("新对话");
        session.setMessageCount(0);
        session.setLastTime(LocalDateTime.now());
        qaSessionMapper.insert(session);
        return toSessionVO(session);
    }

    /**
     * 会话分页列表(按最后活跃时间倒序)
     */
    public PageResult<QaSessionVO> listSessions(LoginUser user, int page, int size) {
        Page<EntQaSession> p = qaSessionMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<EntQaSession>()
                        .eq(EntQaSession::getEnterpriseId, user.getEnterpriseId())
                        .orderByDesc(EntQaSession::getLastTime));
        List<QaSessionVO> list = p.getRecords().stream().map(this::toSessionVO).collect(Collectors.toList());
        return PageResult.of(list, p.getTotal(), page, size);
    }

    /**
     * 加载某会话全部消息(按提问时间升序)
     */
    public List<QaHistoryVO> getSessionMessages(LoginUser user, Long sessionId) {
        EntQaSession session = requireOwnedSession(user, sessionId);
        List<EntQaRecord> records = qaRecordMapper.selectList(
                new LambdaQueryWrapper<EntQaRecord>()
                        .eq(EntQaRecord::getSessionId, session.getId())
                        .orderByAsc(EntQaRecord::getId));
        return records.stream().map(this::toHistoryVO).collect(Collectors.toList());
    }

    /**
     * 删除会话(连同其下全部问答记录)
     */
    @Transactional
    public void deleteSession(LoginUser user, Long sessionId) {
        EntQaSession session = requireOwnedSession(user, sessionId);
        qaRecordMapper.delete(new LambdaQueryWrapper<EntQaRecord>()
                .eq(EntQaRecord::getSessionId, session.getId()));
        qaSessionMapper.deleteById(session.getId());
    }

    /**
     * 清空当前企业全部会话与问答记录
     */
    @Transactional
    public void clearHistory(LoginUser user) {
        List<EntQaSession> sessions = qaSessionMapper.selectList(
                new LambdaQueryWrapper<EntQaSession>()
                        .eq(EntQaSession::getEnterpriseId, user.getEnterpriseId()));
        if (sessions.isEmpty()) {
            return;
        }
        List<Long> ids = sessions.stream().map(EntQaSession::getId).collect(Collectors.toList());
        qaRecordMapper.delete(new LambdaQueryWrapper<EntQaRecord>().in(EntQaRecord::getSessionId, ids));
        qaSessionMapper.delete(new LambdaQueryWrapper<EntQaSession>()
                .eq(EntQaSession::getEnterpriseId, user.getEnterpriseId()));
    }

    // ============ 问答主入口 ============

    /**
     * 智能问答入口(RAG + LLM + 会话上下文;LLM 失败兜底本地 FAQ)
     */
    public QaAskResp ask(LoginUser user, QaAskReq req) {
        String q = normalize(req.getQuestion());
        String raw = req.getQuestion() == null ? "" : req.getQuestion().trim();
        QaAskResp resp = new QaAskResp();

        // 1. RAG:检索相关事项(关键词命中事项名)
        List<QaAskResp.RelatedMatter> related = new ArrayList<>();
        for (EntMatterInfo m : listOnlineMatters()) {
            if (containsAny(q, m.getMatterName()) || containsAny(m.getMatterName(), q)) {
                related.add(toRelated(m));
            }
        }

        // 2. RAG:检索相关政策
        List<QaAskResp.Source> sources = new ArrayList<>();
        for (EntPolicyMatch p : listPolicies()) {
            if (containsAny(q, p.getPolicyTitle()) || (p.getPolicyContent() != null && containsAny(q, p.getPolicyContent()))) {
                QaAskResp.Source s = new QaAskResp.Source();
                s.setType("POLICY");
                s.setTitle(p.getPolicyTitle());
                s.setSnippet(StrUtil.maxLength(p.getPolicyContent(), 80));
                sources.add(s);
            }
        }

        // 3. 构建知识库上下文(事项办理指南 + 政策内容)
        StringBuilder kb = new StringBuilder();
        if (!related.isEmpty()) {
            kb.append("【相关事项】\n");
            for (QaAskResp.RelatedMatter r : related) {
                EntMatterInfo info = matterMapper.selectById(r.getMatterId());
                if (info != null) {
                    kb.append("- ").append(info.getMatterName())
                            .append("(").append(info.getDepartment() == null ? "" : info.getDepartment()).append("):")
                            .append(info.getGuideContent() != null ? info.getGuideContent() : (info.getDescription() == null ? "" : info.getDescription()))
                            .append("\n");
                }
            }
        }
        if (!sources.isEmpty()) {
            kb.append("【相关政策】\n");
            for (QaAskResp.Source s : sources) {
                kb.append("- ").append(s.getTitle()).append(":").append(s.getSnippet()).append("\n");
            }
        }

        // 4. 取会话历史(最近6轮,作为多轮对话上下文)
        List<Map<String, String>> history = new ArrayList<>();
        if (req.getSessionId() != null) {
            try {
                EntQaSession session = qaSessionMapper.selectById(req.getSessionId());
                if (session != null && session.getEnterpriseId().equals(user.getEnterpriseId())) {
                    Page<EntQaRecord> rp = qaRecordMapper.selectPage(new Page<>(1, 6),
                            new LambdaQueryWrapper<EntQaRecord>()
                                    .eq(EntQaRecord::getSessionId, session.getId())
                                    .orderByDesc(EntQaRecord::getId));
                    List<EntQaRecord> recs = rp.getRecords();
                    java.util.Collections.reverse(recs);
                    for (EntQaRecord r : recs) {
                        history.add(Map.of("role", "user", "content", r.getQuestion()));
                        history.add(Map.of("role", "assistant", "content", r.getAnswer()));
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // 5. system prompt:政务助手身份 + 知识库
        String system = "你是政务办事智能助手「小政」,服务企业用户。请基于以下政务知识库回答问题,要求:简明、准确、可操作,分点说明。如知识库未覆盖用户问题,请坦诚说明并建议前往「法人办事大厅」浏览事项或「政策服务」查看惠企政策。不要编造未在知识库中的具体办理流程或材料。\n\n" + kb;

        // 6. 调 LLM 生成答案,失败兜底本地 FAQ
        String answer = llmClient.chat(system, history, raw);
        Faq best = matchFaq(q);
        if (StrUtil.isBlank(answer)) {
            answer = (best != null) ? best.answer : "已为您在事项库与政策库中检索,暂未直接命中明确答案。建议您换一种说法,或前往「法人办事大厅」浏览事项、前往「政策服务」查看惠企政策。";
        }
        resp.setAnswer(answer);
        // FAQ 命中也补充事项来源标签
        if (best != null && best.sourceTitle != null && sources.stream().noneMatch(s -> best.sourceTitle.equals(s.getTitle()))) {
            QaAskResp.Source s = new QaAskResp.Source();
            s.setType("MATTER");
            s.setTitle(best.sourceTitle);
            s.setSnippet(StrUtil.maxLength(best.answer, 60));
            sources.add(s);
        }
        if (best != null) {
            for (Long matterId : best.relatedMatterIds) {
                EntMatterInfo m = matterMapper.selectById(matterId);
                if (m != null && related.stream().noneMatch(r -> r.getMatterId().equals(m.getId()))) {
                    related.add(toRelated(m));
                }
            }
        }

        resp.setSources(sources);
        resp.setRelatedMatters(related.size() > 3 ? new ArrayList<>(related.subList(0, 3)) : related);
        resp.setSuggestedQuestions(suggestions(best));

        // 7. 持久化问答记录到会话
        saveRecordToSession(user, req, resp);
        return resp;
    }

    /**
     * 本地 FAQ 关键词打分(用于 LLM 失败兜底与来源补充)
     */
    private Faq matchFaq(String q) {
        Faq best = null;
        int bestScore = 0;
        for (Faq faq : FAQS) {
            int score = 0;
            for (String kw : faq.keywords) {
                if (q.contains(kw)) {
                    score++;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best = faq;
            }
        }
        return best;
    }

    /**
     * 问答历史记录(分页,按会话维度返回会话列表)
     * 兼容旧 /history 路径:语义升级为会话列表
     */
    public PageResult<QaSessionVO> history(LoginUser user, int page, int size) {
        return listSessions(user, page, size);
    }

    /**
     * 推荐问题
     */
    public List<String> suggestions() {
        return Arrays.asList(
                "如何开办一家公司?",
                "食品经营许可证怎么办理?",
                "什么是免证办?",
                "电子证照快到期了怎么办?",
                "一件事套餐和一表申报是什么?"
        );
    }

    // ============ 内部方法 ============

    private void saveRecordToSession(LoginUser user, QaAskReq req, QaAskResp resp) {
        try {
            // 确定会话:请求带 sessionId 则校验归属,否则新建
            EntQaSession session;
            if (req.getSessionId() != null) {
                session = requireOwnedSession(user, req.getSessionId());
            } else {
                QaSessionVO vo = createSession(user);
                session = qaSessionMapper.selectById(vo.getId());
            }

            EntQaRecord record = new EntQaRecord();
            record.setEnterpriseId(user.getEnterpriseId());
            record.setUserId(user.getUserId());
            record.setSessionId(session.getId());
            record.setQuestion(req.getQuestion());
            record.setAnswer(resp.getAnswer());
            record.setSources(resp.getSources() == null ? null : JSONUtil.toJsonStr(resp.getSources()));
            qaRecordMapper.insert(record);

            // 首条消息时把会话标题更新为问题摘要
            boolean firstMessage = (session.getMessageCount() == null || session.getMessageCount() == 0);
            session.setMessageCount((session.getMessageCount() == null ? 0 : session.getMessageCount()) + 1);
            session.setLastTime(LocalDateTime.now());
            if (firstMessage) {
                String title = StrUtil.maxLength(req.getQuestion().trim(), TITLE_MAX);
                session.setTitle(StrUtil.isBlank(title) ? "新对话" : title);
            }
            qaSessionMapper.updateById(session);
        } catch (Exception e) {
            // 历史记录写入失败不影响问答主流程
        }
    }

    private EntQaSession requireOwnedSession(LoginUser user, Long sessionId) {
        if (sessionId == null) {
            throw new BusinessException("会话ID不能为空");
        }
        EntQaSession session = qaSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        if (!session.getEnterpriseId().equals(user.getEnterpriseId())) {
            throw new BusinessException(403, "无权操作该会话");
        }
        return session;
    }

    private QaHistoryVO toHistoryVO(EntQaRecord r) {
        QaHistoryVO vo = new QaHistoryVO();
        vo.setId(r.getId());
        vo.setQuestion(r.getQuestion());
        vo.setAnswer(r.getAnswer());
        vo.setCreateTime(r.getCreateTime());
        if (StrUtil.isNotBlank(r.getSources())) {
            try {
                vo.setSources(JSONUtil.toList(r.getSources(), QaAskResp.Source.class));
            } catch (Exception ignored) {
            }
        }
        return vo;
    }

    private QaSessionVO toSessionVO(EntQaSession s) {
        QaSessionVO vo = new QaSessionVO();
        vo.setId(s.getId());
        vo.setTitle(s.getTitle());
        vo.setMessageCount(s.getMessageCount());
        vo.setLastTime(s.getLastTime());
        vo.setCreateTime(s.getCreateTime());
        return vo;
    }

    private List<String> suggestions(Faq matched) {
        List<String> all = suggestions();
        if (matched == null) {
            return all;
        }
        return all.stream().filter(s -> !s.equals(matched.question)).limit(4).toList();
    }

    private QaAskResp.RelatedMatter toRelated(EntMatterInfo m) {
        QaAskResp.RelatedMatter r = new QaAskResp.RelatedMatter();
        r.setMatterId(m.getId());
        r.setMatterName(m.getMatterName());
        r.setMatterCode(m.getMatterCode());
        return r;
    }

    private List<EntMatterInfo> listOnlineMatters() {
        return matterMapper.selectList(new LambdaQueryWrapper<EntMatterInfo>()
                .eq(EntMatterInfo::getStatus, 1));
    }

    private List<EntPolicyMatch> listPolicies() {
        return policyMapper.selectList(new LambdaQueryWrapper<EntPolicyMatch>());
    }

    private boolean containsAny(String text, String keyword) {
        if (StrUtil.isBlank(text) || StrUtil.isBlank(keyword)) {
            return false;
        }
        return text.contains(keyword);
    }

    private String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.toLowerCase().replaceAll("[\\s\\p{Punct}，。！？、：；“”‘’（）《》【】]", "");
    }

    /**
     * 本地 FAQ 条目
     */
    private static class Faq {
        final String question;
        final List<String> keywords;
        final String answer;
        final String sourceTitle;
        final List<Long> relatedMatterIds;

        Faq(String question, List<String> keywords, String answer, String sourceTitle, List<Long> relatedMatterIds) {
            this.question = question;
            this.keywords = keywords;
            this.answer = answer;
            this.sourceTitle = sourceTitle;
            this.relatedMatterIds = relatedMatterIds;
        }
    }
}
