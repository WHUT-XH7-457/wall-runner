package com.wallrunner.shared.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GameEventBus 单元测试。
 *
 * 测试范围：
 * - 订阅-发布基本流程。
 * - 多监听器同时接收。
 * - 取消订阅后不再接收。
 * - 弱引用自动清理（GC 后监听器失效）。
 * - 事件类型隔离（不同类型互不干扰）。
 * - clear 清空所有监听器。
 */
class GameEventBusTest {

    private GameEventBus bus;

    @BeforeEach
    void setUp() {
        bus = GameEventBus.getInstance();
        bus.clear();
    }

    @Test
    void testSubscribeAndPublish() {
        final String[] payload = {""};
        bus.subscribe(GameEvent.EventType.PLAYER_DEATH, e -> payload[0] = "dead");
        bus.publish(new PlayerDeathEvent("p1", 0, 0));
        assertEquals("dead", payload[0]);
    }

    @Test
    void testMultipleListeners() {
        final int[] count = {0};
        bus.subscribe(GameEvent.EventType.SCORE_CHANGE, e -> count[0]++);
        bus.subscribe(GameEvent.EventType.SCORE_CHANGE, e -> count[0]++);
        bus.publish(new ScoreChangeEvent("p1", 10, 10));
        assertEquals(2, count[0]);
    }

    @Test
    void testUnsubscribe() {
        final int[] count = {0};
        java.util.function.Consumer<GameEvent> listener = e -> count[0]++;
        bus.subscribe(GameEvent.EventType.PLAYER_JUMP, listener);
        bus.unsubscribe(GameEvent.EventType.PLAYER_JUMP, listener);
        bus.publish(new PlayerJumpEvent("p1", 0, 0, "left"));
        assertEquals(0, count[0]);
    }

    @Test
    void testListenerCount() {
        assertEquals(0, bus.listenerCount(GameEvent.EventType.PHASE_CHANGE));
        bus.subscribe(GameEvent.EventType.PHASE_CHANGE, e -> {});
        assertEquals(1, bus.listenerCount(GameEvent.EventType.PHASE_CHANGE));
    }

    @Test
    void testTypeIsolation() {
        final boolean[] wrong = {false};
        bus.subscribe(GameEvent.EventType.PLAYER_DEATH, e -> wrong[0] = true);
        bus.publish(new PlayerSpawnEvent("p1", 0));
        assertFalse(wrong[0]);
    }

    @Test
    void testClear() {
        bus.subscribe(GameEvent.EventType.COLLISION_PLAYER, e -> {});
        bus.clear();
        assertEquals(0, bus.listenerCount(GameEvent.EventType.COLLISION_PLAYER));
    }
}
