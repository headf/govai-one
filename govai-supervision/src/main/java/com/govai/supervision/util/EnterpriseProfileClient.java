package com.govai.supervision.util;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 企业端档案只读客户端
 * 监管端按 enterprise_id 批量回填企业名称/统一信用代码时调用企业端(8084) batch 接口。
 * 调用失败或企业端离线时静默降级返回空 Map，不影响监管端主流程。
 */
@Slf4j
@Component
public class EnterpriseProfileClient {

    private final String enterpriseUrl;

    public EnterpriseProfileClient(@Value("${govai.profile.enterprise-url:http://localhost:8084}") String enterpriseUrl) {
        this.enterpriseUrl = enterpriseUrl;
    }

    /** 企业摘要 */
    @Data
    public static class Brief {
        private Long enterpriseId;
        private String enterpriseName;
        private String creditCode;
    }

    /** 企业成员项 */
    @Data
    public static class MemberItem {
        private Long id;
        private String username;
        private String realName;
        private String phone;
        private String role;
        private Integer status;
    }

    /** 企业法人+成员 */
    @Data
    public static class Members {
        private Long enterpriseId;
        private String legalPerson;
        private String legalPersonIdCard;
        private List<MemberItem> members;
    }

    /** 企业用户(全量列表项,含所属企业) */
    @Data
    public static class EnterpriseUserItem {
        private Long id;
        private String username;
        private String realName;
        private String phone;
        private Long enterpriseId;
        private String enterpriseName;
        private String role;
        private Integer status;
    }

    /**
     * 按企业ID查法人+账号成员
     *
     * @return 成员信息；企业端不可用/无记录时返回 null
     */
    public Members fetchMembers(Long enterpriseId) {
        if (enterpriseId == null) {
            return null;
        }
        String url = enterpriseUrl + "/api/v1/ent-enterprise/users/members?enterpriseId=" + enterpriseId;
        try {
            String body = HttpUtil.get(url, 3000);
            JSONObject root = JSONUtil.parseObj(body);
            if (root.getInt("code", -1) != 200 || root.get("data") == null) {
                log.warn("企业成员查询异常 code={}, url={}", root.getInt("code"), url);
                return null;
            }
            JSONObject data = root.getJSONObject("data");
            Members members = new Members();
            members.setEnterpriseId(data.getLong("enterpriseId"));
            members.setLegalPerson(data.getStr("legalPerson"));
            members.setLegalPersonIdCard(data.getStr("legalPersonIdCard"));
            List<MemberItem> items = new java.util.ArrayList<>();
            cn.hutool.json.JSONArray arr = data.getJSONArray("members");
            if (arr != null) {
                for (Object o : arr) {
                    JSONObject jo = (JSONObject) o;
                    MemberItem item = new MemberItem();
                    item.setId(jo.getLong("id"));
                    item.setUsername(jo.getStr("username"));
                    item.setRealName(jo.getStr("realName"));
                    item.setPhone(jo.getStr("phone"));
                    item.setRole(jo.getStr("role"));
                    item.setStatus(jo.getInt("status"));
                    items.add(item);
                }
            }
            members.setMembers(items);
            return members;
        } catch (Exception e) {
            log.debug("企业档案服务({})不可用,静默降级 members", enterpriseUrl);
            return null;
        }
    }

    /**
     * 按企业ID批量查询名称/信用代码
     *
     * @param ids 企业档案ID(enterprise_info.id)列表
     * @return id -> Brief；查询失败/无匹配时返回空 Map
     */
    public Map<Long, Brief> fetchBriefs(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(ids.get(i));
        }
        String url = enterpriseUrl + "/api/v1/ent-enterprise/batch?ids=" + sb;
        try {
            String body = HttpUtil.get(url, 3000);
            JSONObject root = JSONUtil.parseObj(body);
            if (root.getInt("code", -1) != 200) {
                log.warn("企业档案查询异常 code={}, url={}", root.getInt("code"), url);
                return Collections.emptyMap();
            }
            JSONObject data = root.getJSONObject("data");
            if (data == null) {
                return Collections.emptyMap();
            }
            Map<Long, Brief> result = new HashMap<>();
            data.forEach((k, v) -> {
                try {
                    Brief brief = JSONUtil.toBean((JSONObject) v, Brief.class);
                    result.put(Long.valueOf(k), brief);
                } catch (Exception ignore) {
                    // 单条解析失败跳过
                }
            });
            return result;
        } catch (Exception e) {
            log.debug("企业档案服务({})不可用,静默降级", enterpriseUrl);
            return Collections.emptyMap();
        }
    }

    /**
     * 拉取企业端全部企业用户(含所属企业名)
     *
     * @return 用户列表；企业端不可用时返回空列表
     */
    public List<EnterpriseUserItem> fetchAllEnterpriseUsers() {
        String url = enterpriseUrl + "/api/v1/ent-enterprise/users/all";
        try {
            String body = HttpUtil.get(url, 3000);
            JSONObject root = JSONUtil.parseObj(body);
            if (root.getInt("code", -1) != 200 || root.get("data") == null) {
                log.warn("企业用户全量查询异常 code={}, url={}", root.getInt("code"), url);
                return Collections.emptyList();
            }
            List<EnterpriseUserItem> items = new java.util.ArrayList<>();
            cn.hutool.json.JSONArray arr = root.getJSONArray("data");
            if (arr != null) {
                for (Object o : arr) {
                    JSONObject jo = (JSONObject) o;
                    EnterpriseUserItem item = new EnterpriseUserItem();
                    item.setId(jo.getLong("id"));
                    item.setUsername(jo.getStr("username"));
                    item.setRealName(jo.getStr("realName"));
                    item.setPhone(jo.getStr("phone"));
                    item.setEnterpriseId(jo.getLong("enterpriseId"));
                    item.setEnterpriseName(jo.getStr("enterpriseName"));
                    item.setRole(jo.getStr("role"));
                    item.setStatus(jo.getInt("status"));
                    items.add(item);
                }
            }
            return items;
        } catch (Exception e) {
            log.debug("企业档案服务({})不可用,静默降级 all-users", enterpriseUrl);
            return Collections.emptyList();
        }
    }

    /**
     * 监管端跨服禁用企业经办人
     *
     * @return 成功 true；失败(企业端不可用/无权限) false
     */
    public boolean disableEnterpriseUser(Long userId) {
        if (userId == null) {
            return false;
        }
        String url = enterpriseUrl + "/api/v1/ent-enterprise/users/members/" + userId + "/disable";
        try {
            String body = HttpUtil.post(url, "");
            JSONObject root = JSONUtil.parseObj(body);
            boolean ok = root.getInt("code", -1) == 200;
            if (!ok) {
                log.warn("跨服禁用经办人失败 code={}, message={}, url={}",
                        root.getInt("code"), root.getStr("message"), url);
            }
            return ok;
        } catch (Exception e) {
            log.debug("企业档案服务({})不可用,静默降级 disable", enterpriseUrl);
            return false;
        }
    }

    /**
     * 监管端跨服启用企业经办人(解除禁用,恢复企业端登录)
     *
     * @return 成功 true；失败(企业端不可用) false
     */
    public boolean enableEnterpriseUser(Long userId) {
        if (userId == null) {
            return false;
        }
        String url = enterpriseUrl + "/api/v1/ent-enterprise/users/members/" + userId + "/enable";
        try {
            String body = HttpUtil.post(url, "");
            JSONObject root = JSONUtil.parseObj(body);
            boolean ok = root.getInt("code", -1) == 200;
            if (!ok) {
                log.warn("跨服启用经办人失败 code={}, message={}, url={}",
                        root.getInt("code"), root.getStr("message"), url);
            }
            return ok;
        } catch (Exception e) {
            log.debug("企业档案服务({})不可用,静默降级 enable", enterpriseUrl);
            return false;
        }
    }
}
