package com.wallrunner.shared.physics;

import com.wallrunner.shared.entity.GameState;
import com.wallrunner.shared.entity.Player;
import com.wallrunner.shared.event.GameEventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.wallrunner.shared.constants.GameConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PhysicsEngine 集成单元测试。
 *
 * 测试范围：
 * - initState 正确初始化玩家位置与状态。
 * - startGame 仅在 menu/gameover 阶段生效。
 * - update 仅在 playing 阶段增加 frames。
 * - handleInput 触发跳跃。
 * - initJoiningPlayer 将新玩家放置到最后方玩家附近。
 */
class PhysicsEngineTest {

    private PhysicsEngine engine;
    private GameState state;
    private GameEventBus eventBus;

    @BeforeEach
    void setUp() {
        eventBus = GameEventBus.getInstance();
        eventBus.clear();
        engine = PhysicsEngine.createDefault(eventBus);
        state = new GameState();
    }

    @Test
    void testInitState_ResetsPlayers() {
        Player p = new Player("p1", "Test");
        p.setActive(true);
        p.setScore(100);
        p.setLives(1);
        state.getPlayers().put(p.getId(), p);

        engine.initState(state);
        assertEquals(0, p.getScore());
        assertEquals(MAX_LIVES, p.getLives());
        assertEquals("menu", state.getPhase());
        assertEquals(0, state.getFrames());
    }

    @Test
    void testInitState_PlayerPosition() {
        Player p1 = new Player("p1", "A");
        Player p2 = new Player("p2", "B");
        state.getPlayers().put(p1.getId(), p1);
        state.getPlayers().put(p2.getId(), p2);

        engine.initState(state);
        assertEquals("left", p1.getSide());
        assertEquals("right", p2.getSide());
        assertEquals(WALL_WIDTH, p1.getX(), 0.01);
        assertEquals(CANVAS_WIDTH - WALL_WIDTH - PLAYER_SIZE, p2.getX(), 0.01);
        assertEquals(0, p1.getY(), 0.01);
    }

    @Test
    void testStartGame_FromMenu() {
        state.setPhase("menu");
        engine.startGame(state);
        assertEquals("playing", state.getPhase());
    }

    @Test
    void testStartGame_FromGameOver() {
        state.setPhase("gameover");
        engine.startGame(state);
        assertEquals("playing", state.getPhase());
    }

    @Test
    void testStartGame_NoEffectWhenPlaying() {
        state.setPhase("playing");
        state.setFrames(100);
        engine.startGame(state);
        assertEquals("playing", state.getPhase());
        assertEquals(100, state.getFrames());
    }

    @Test
    void testUpdate_IncrementsFramesWhenPlaying() {
        Player p = new Player("p1", "Test");
        p.setActive(true);
        state.getPlayers().put(p.getId(), p);

        engine.initState(state);
        state.setPhase("playing");
        int before = state.getFrames();
        engine.update(state);
        assertEquals(before + 1, state.getFrames());
    }

    @Test
    void testUpdate_NoEffectWhenMenu() {
        Player p = new Player("p1", "Test");
        p.setActive(true);
        state.getPlayers().put(p.getId(), p);
        state.setPhase("menu");

        int before = state.getFrames();
        engine.update(state);
        assertEquals(before, state.getFrames());
    }

    @Test
    void testHandleInput_Jump() {
        Player p = new Player("p1", "Test");
        p.setSide("left");
        p.setJumping(false);

        engine.handleInput(p, "jump");
        assertTrue(p.isJumping());
    }

    @Test
    void testInitJoiningPlayer() {
        Player host = new Player("host", "Host");
        host.setActive(true);
        host.setY(500);
        host.setSide("left");
        host.setCameraY(100);
        host.setCameraTargetY(100);
        state.getPlayers().put(host.getId(), host);
        state.setPhase("playing");

        Player joiner = new Player("join", "Joiner");
        engine.initJoiningPlayer(state, joiner);

        assertTrue(joiner.isActive());
        assertEquals("right", joiner.getSide()); // 与 host 相反
        assertEquals(500, joiner.getY(), 0.01);
        assertEquals(100, joiner.getCameraY(), 0.01);
    }

    @Test
    void testInitJoiningPlayer_NoActivePlayers() {
        Player joiner = new Player("join", "Joiner");
        joiner.setActive(false);
        engine.initJoiningPlayer(state, joiner);
        // 无存活玩家时应该不做任何事
        assertFalse(joiner.isActive());
    }

    @Test
    void testUpdate_GameOverPhaseStopsUpdates() {
        Player p = new Player("p1", "Test");
        p.setActive(true);
        state.getPlayers().put(p.getId(), p);
        state.setPhase("gameover");

        int before = state.getFrames();
        engine.update(state);
        // gameover 阶段不应增加 frames
        assertEquals(before, state.getFrames());
    }
}
