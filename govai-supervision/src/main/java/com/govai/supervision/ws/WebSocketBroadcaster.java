package com.govai.supervision.ws;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 实时推送广播器（大屏 /ws 端点）
 * 审批数据经 MQ 落库后，向所有已连接会话广播最新概览，实现大屏实时刷新
 */
@Slf4j
@Component
public class WebSocketBroadcaster {

    /** 会话容器（线程安全） */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void addSession(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }

    public void removeSession(WebSocketSession session) {
        sessions.remove(session.getId());
    }

    public int sessionCount() {
        return sessions.size();
    }

    /**
     * 向所有在线大屏广播一个 JSON 消息
     */
    public void broadcast(Object payload) {
        if (sessions.isEmpty()) {
            return;
        }
        TextMessage message = new TextMessage(JSONUtil.toJsonStr(payload));
        for (WebSocketSession session : sessions.values()) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            } catch (IOException e) {
                log.warn("WebSocket 广播失败，已移除会话 {}", session.getId());
                removeSession(session);
            }
        }
    }
}