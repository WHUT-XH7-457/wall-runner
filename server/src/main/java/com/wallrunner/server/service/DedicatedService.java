package com.wallrunner.server.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallrunner.shared.entity.GameState;
import com.wallrunner.shared.entity.Player;
import com.wallrunner.shared.physics.GamePhysics;

/**
 * 公共服务器模式（Dedicated）：服务端运行权威物理，向所有客户端广播 STATE。
 *
 * 原则：物理计算委托给 GamePhysics（Y层），本类仅做调度与网络 I/O（X层）。
 */
@Service
public class DedicatedService implements IDedicatedService {

    private final RoomManager roomManager;
    private final SessionManager sessionManager;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final Map<String, Boolean> activeDedicated = new ConcurrentHashMap<>();

    private final String mainRoomId;
    private final int broadcastEveryTicks;
    private final long pingWarningMs;
    private final long pingOfflineMs;

    public DedicatedService(RoomManager roomManager, SessionManager sessionManager,
                            @Value("${dedicated.room-id:DEDICATED-MAIN}") String mainRoomId,
                            @Value("${dedicated.broadcast-every-ticks:2}") int broadcastEveryTicks,
                            @Value("${dedicated.ping-warning-ms:8000}") long pingWarningMs,
                            @Value("${dedicated.ping-offline-ms:15000}") long pingOfflineMs) {
        this.roomManager = roomManager;
        this.sessionManager = sessionManager;
        this.mainRoomId = mainRoomId;
        this.broadcastEveryTicks = broadcastEveryTicks;
        this.pingWarningMs = pingWarningMs;
        this.pingOfflineMs = pingOfflineMs;
    }

    @Override
    public synchronized String getOrCreateRoom() {
        if (!roomManager.isRoomExists(mainRoomId)) {
            roomManager.createRoom(mainRoomId, "SERVER");
            GameState state = roomManager.getRoom(mainRoomId);
            if (state != null) {
                state.setPhase("menu");
            }
            activeDedicated.put(mainRoomId, false);
        }
        return mainRoomId;
    }

    @Override
    public boolean isRoomActive(String roomId) {
        return Boolean.TRUE.equals(activeDedicated.get(roomId));
    }

    @Override
    public void join(String roomId, Player player, WebSocketSession session) {
        GameState state = roomManager.getRoom(roomId);
        boolean isLateJoin = isRoomActive(roomId) && state != null && "playing".equals(state.getPhase());
        roomManager.joinRoom(roomId, player);
        sessionManager.bindRoom(session.getId(), roomId);
        if (isLateJoin && state != null) {
            GamePhysics.initJoiningPlayer(state, player);
        }
        if (state != null && state.getPlayers().size() >= 1 && !Boolean.TRUE.equals(activeDedicated.get(roomId))) {
            startGame(roomId);
        }
    }

    @Override
    public void startGame(String roomId) {
        GameState state = roomManager.getRoom(roomId);
        if (state != null) {
            GamePhysics.startGame(state);
            activeDedicated.put(roomId, true);
        }
    }

    @Override
    public void handleInput(String roomId, String playerId, String action) {
        GameState state = roomManager.getRoom(roomId);
        if (state == null) return;
        Player p = state.getPlayers().get(playerId);
        if (p == null) return;
        if ("start".equals(action)) {
            if ("menu".equals(state.getPhase()) || "gameover".equals(state.getPhase())) {
                GamePhysics.startGame(state);
                activeDedicated.put(roomId, true);
            }
        } else if ("respawn".equals(action)) {
            if (!p.isActive()) {
                GamePhysics.respawnPlayer(state, p);
            }
        } else {
            GamePhysics.handleInput(p, action);
        }
    }

    private final Map<String, Integer> broadcastCounters = new ConcurrentHashMap<>();

    @Scheduled(fixedRateString = "${dedicated.tick-ms:8}")
    @Override
    public void tick() {
        for (Map.Entry<String, Boolean> entry : activeDedicated.entrySet()) {
            if (!Boolean.TRUE.equals(entry.getValue())) continue;
            String roomId = entry.getKey();
            GameState state = roomManager.getRoom(roomId);
            if (state == null) continue;
            GamePhysics.update(state);

            // 掉线检测：双重确认机制
            long now = System.currentTimeMillis();
            for (Player p : state.getPlayers().values()) {
                if (p.isDisconnected()) {
                    // 已离线玩家：检查是否超过1分钟，超过则隐藏分数
                    continue;
                }
                if (p.getLastPingTime() <= 0) {
                    p.setLastPingTime(now);
                    continue;
                }
                long elapsed = now - p.getLastPingTime();
                if (elapsed > pingWarningMs && p.isPingAcknowledged()) {
                    // 超过配置时间未收到心跳，发送ping请求
                    p.setPingAcknowledged(false);
                    sendPing(roomId, p.getId());
                } else if (elapsed > pingOfflineMs && !p.isPingAcknowledged()) {
                    // 超过配置时间仍未收到回应，标记为离线
                    p.setDisconnected(true);
                    p.setOfflineTime(now);
                    p.setPaused(true); // 离线玩家视为暂停状态（无碰撞）
                    System.out.println("[Dedicated] Player " + p.getName() + " marked offline");
                }
            }

            int counter = broadcastCounters.getOrDefault(roomId, 0) + 1;
            broadcastCounters.put(roomId, counter);
            if (counter >= broadcastEveryTicks) {
                broadcastCounters.put(roomId, 0);
                broadcastState(roomId, state);
            }
            if ("gameover".equals(state.getPhase())) {
                activeDedicated.put(roomId, false);
            }
        }
    }

    private void sendPing(String roomId, String playerId) {
        // 向房间内所有在线会话发送 ping（客户端根据自身的 playerId 回复 pong）
        for (org.springframework.web.socket.WebSocketSession s : sessionManager.getAllSessions().values()) {
            if (roomId.equals(sessionManager.getRoomId(s.getId())) && s.isOpen()) {
                try {
                    java.util.Map<String, Object> ping = new java.util.HashMap<>();
                    ping.put("type", "ping");
                    ping.put("roomId", roomId);
                    ping.put("playerId", playerId);
                    ping.put("timestamp", System.currentTimeMillis());
                    s.sendMessage(new org.springframework.web.socket.TextMessage(objectMapper.writeValueAsString(ping)));
                } catch (Exception e) {
                    System.err.println("[Dedicated] Ping send failed: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public GameState getGameState() {
        return roomManager.getRoom(mainRoomId);
    }

    private void broadcastState(String roomId, GameState state) {
        try {
            String payloadJson = objectMapper.writeValueAsString(state);
            Map<String, Object> msg = Map.of("type", "state", "payload", payloadJson);
            String json = objectMapper.writeValueAsString(msg);
            TextMessage tm = new TextMessage(json);
            for (WebSocketSession s : sessionManager.getAllSessions().values()) {
                if (roomId.equals(sessionManager.getRoomId(s.getId())) && s.isOpen()) {
                    try {
                        s.sendMessage(tm);
                    } catch (Exception e) {
                        System.err.println("[DedicatedService] Send to " + s.getId() + " failed: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[DedicatedService] broadcast failed: " + e.getMessage());
        }
    }
}
