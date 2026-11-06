package com.govai.supervision.ws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 大屏实时推送 WebSocket 处理器（/ws 端点）
 * 客户端连接后维持长连接，服务端收到 MQ 审批数据时由 WebSocketBroadcaster 推送最新概览
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketBroadcaster broadcaster;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        broadcaster.addSession(session);
        log.info("大屏已连接，当前在线 {} 个", broadcaster.sessionCount());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 客户端可通过发送 ping 保活；大屏数据由服务端主动推送，无需处理上行数据
        log.debug("收到 ws 消息: {}", message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        broadcaster.removeSession(session);
        log.info("大屏已断开，剩余在线 {} 个", broadcaster.sessionCount());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        broadcaster.removeSession(session);
        log.warn("WebSocket 传输异常: {}", exception.getMessage());
    }
}