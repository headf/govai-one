package com.govai.citizen.util;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JsonUtil {

    private JsonUtil() {
    }

    public static List<String> toList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            JSONArray arr = JSONUtil.parseArray(json);
            List<String> out = new ArrayList<>(arr.size());
            for (Object o : arr) {
                out.add(String.valueOf(o));
            }
            return out;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return JSONUtil.toBean(json, Map.class);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    public static String fromList(List<?> list) {
        return list == null ? "[]" : JSONUtil.toJsonStr(list);
    }

    public static String fromMap(Map<?, ?> map) {
        return map == null ? "{}" : JSONUtil.toJsonStr(map);
    }
}
