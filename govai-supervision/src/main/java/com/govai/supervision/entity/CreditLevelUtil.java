package com.govai.supervision.entity;

import java.math.BigDecimal;

/**
 * 信用等级判定工具
 * 依据评分映射 A/B/C/D 等级
 */
public final class CreditLevelUtil {

    private CreditLevelUtil() {
    }

    /** A≥85, B≥70, C≥60, 否则 D */
    public static String levelOf(BigDecimal score) {
        if (score == null) {
            return "B";
        }
        double s = score.doubleValue();
        if (s >= 85) {
            return "A";
        } else if (s >= 70) {
            return "B";
        } else if (s >= 60) {
            return "C";
        } else {
            return "D";
        }
    }
}