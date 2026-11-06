package com.govai.supervision.common;

import cn.hutool.core.util.IdUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 业务单号生成器
 *
 * 为监管端各类"记录型"业务(抽查/预警/修复/信用档案)生成有业务含义、可展示给用户的单号,
 * 替代把自增主键 id 直接亮给用户的做法。规则:前缀 + 日期 + 4位随机大写(与企业端 recordCode 同款)。
 */
public final class BizNoGenerator {

    private BizNoGenerator() {
    }

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 生成业务单号
     *
     * @param prefix 业务前缀(如 抽查=CJ、预警=YJ、修复=XF、个人信用=CX)
     */
    public static String gen(String prefix) {
        String p = (prefix == null || prefix.isBlank()) ? "NO" : prefix.trim();
        return p + LocalDate.now().format(DAY) + IdUtil.randomUUID().substring(0, 4).toUpperCase();
    }
}
