package com.govai.approval.common;

/**
 * 审批员端常量
 */
public final class Constants {

    private Constants() {}

    /** HTTP头 */
    public static final String HEADER_AUTH = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    /** JWT claims keys */
    public static final String CLAIM_USER_ID   = "userId";
    public static final String CLAIM_USER_NAME = "userName";
    public static final String CLAIM_ROLE      = "role";

    /** 角色码（与监管端 sys_role.role_code 对齐） */
    public static final String ROLE_ADMIN     = "ADMIN";
    public static final String ROLE_APPROVER  = "APPROVER";
    public static final String ROLE_SUPERVISOR = "SUPERVISOR";
    public static final String ROLE_CITIZEN   = "CITIZEN";
    public static final String ROLE_ENTERPRISE = "ENTERPRISE";
    /** 审批岗位角色码（多岗位分离：受理/初审同岗 APPROVER，终审 DECIDE，办结出证 ISSUE） */
    public static final String ROLE_DECIDE    = "DECIDE";
    public static final String ROLE_ISSUE     = "ISSUE";

    /** 审批实例状态 */
    public static final String INSTANCE_PENDING   = "PENDING";     // 待受理
    public static final String INSTANCE_APPROVING = "APPROVING";   // 审批中
    public static final String INSTANCE_APPROVED  = "APPROVED";     // 通过
    public static final String INSTANCE_REJECTED  = "REJECTED";     // 不通过
    public static final String INSTANCE_RETURNED  = "RETURNED";     // 退回补正

    /** 任务状态 */
    public static final String TASK_PENDING = "PENDING";
    public static final String TASK_DONE    = "DONE";
    public static final String TASK_CANCEL  = "CANCEL";

    /** 审批结果 */
    public static final String RESULT_PASS    = "PASS";
    public static final String RESULT_REJECT  = "REJECT";
    public static final String RESULT_RETURN  = "RETURN";

    /** 环节类型 */
    public static final String NODE_START      = "START";
    public static final String NODE_APPROVAL   = "APPROVAL";
    public static final String NODE_RULE_CHECK = "RULE_CHECK";
    public static final String NODE_BRANCH     = "BRANCH";
    public static final String NODE_END        = "END";

    /** 操作类型（审计日志） */
    public static final String OP_SUBMIT   = "SUBMIT";
    public static final String OP_RECEIVE  = "RECEIVE";
    public static final String OP_APPROVE  = "APPROVE";
    public static final String OP_RETURN   = "RETURN";
    public static final String OP_FINISH   = "FINISH";
    public static final String OP_AI_ADVICE = "AI_ADVICE";

    /** 规则引擎结果 */
    public static final String RULE_PASS      = "PASS";
    public static final String RULE_FAIL      = "FAIL";
    public static final String RULE_TO_MANUAL = "TO_MANUAL";

    /** RabbitMQ 推送（向监管端推送审批数据） */
    public static final String MQ_EXCHANGE    = "approval.exchange";
    public static final String MQ_ROUTING_KEY = "approval.record.push";
    public static final String MQ_QUEUE       = "approval.record.queue";

    /** 审批端允许登录的岗位角色集合（受理/初审同岗 APPROVER、终审 DECIDE、办结 ISSUE；APPROVER 兼容历史） */
    public static final java.util.Set<String> APPROVAL_STAFF_ROLES =
            java.util.Set.of(ROLE_APPROVER, ROLE_DECIDE, ROLE_ISSUE);
}
