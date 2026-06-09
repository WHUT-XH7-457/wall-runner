package com.wallrunner.shared.physics.subsystem;

import com.wallrunner.shared.entity.GameState;
import com.wallrunner.shared.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.wallrunner.shared.constants.GameConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * DeathSystem 单元测试。
 *
 * 测试范围：
 * - 死亡线检测（玩家低于摄像机底部 + DEATH_LINE_OFFSET）。
 * - 生命扣除与最终死亡（变为 spectator）。
 * - 重生逻辑（位置、无敌、生命重置）。
 * - 全灭检测（phase 变为 gameover）。
 */
class DeathSystemTest {

    private DeathSystem deathSystem;
    private GameState state;

    @BeforeEach
    void setUp() {
        deathSystem = new DeathSystem();
        state = new GameState();
    }

    @Test
    void testCheckDeathLine_PlayerDies() {
        Player player = createActivePlayer("p1", 100, 100);
        state.getPlayers().put(player.getId(), player);
        player.setCameraY(0);
        // 死亡线 = cameraY + CANVAS_HEIGHT + DEATH_LINE_OFFSET = 0 + 600 + 200 = 800
        player.setY(850);

        deathSystem.checkDeathLine(state);
        assertEquals(MAX_LIVES - 1, player.getLives());
        assertTrue(player.isActive()); // 还有生命，未完全死亡
    }

    @Test
    void testCheckDeathLine_PlayerSurvives() {
        Player player = createActivePlayer("p1", 100, 100);
        state.getPlayers().put(player.getId(), player);
        player.setCameraY(0);
        player.setY(100); // 远在死亡线之上

        deathSystem.checkDeathLine(state);
        assertEquals(MAX_LIVES, player.getLives());
        assertTrue(player.isActive());
    }

    @Test
    void testApplyDeath_FinalDeath() {
        Player player = createActivePlayer("p1", 100, 100);
        player.setLives(1);
        player.setScore(50);

        deathSystem.applyDeath(state, player);
        assertFalse(player.isActive());
        assertTrue(player.isSpectator());
        assertEquals(50, player.getHighScore());
    }

    @Test
    void testApplyDeath_Respawn() {
        Player player = createActivePlayer("p1", 100, 100);
        player.setLives(2);
        player.setSide("left");
        player.setScore(30);
        state.getPlayers().put(player.getId(), player);

        deathSystem.applyDeath(state, player);
        assertEquals(1, player.getLives());
        assertTrue(player.isActive());
        assertTrue(player.isInvincible());
        assertEquals(2.0, player.getInvincibleTimer(), 0.01);
        assertEquals(30, player.getBaseScore());
    }

    @Test
    void testCheckAllDead_AllDead() {
        Player a = createActivePlayer("p1", 100, 100);
        a.setActive(false);
        Player b = createActivePlayer("p2", 200, 100);
        b.setActive(false);
        state.getPlayers().put(a.getId(), a);
        state.getPlayers().put(b.getId(), b);

        deathSystem.checkAllDead(state);
        assertEquals("gameover", state.getPhase());
    }

    @Test
    void testCheckAllDead_NotAllDead() {
        Player a = createActivePlayer("p1", 100, 100);
        a.setActive(false);
        Player b = createActivePlayer("p2", 200, 100);
        b.setActive(true);
        state.getPlayers().put(a.getId(), a);
        state.getPlayers().put(b.getId(), b);
        state.setPhase("playing");

        deathSystem.checkAllDead(state);
        assertEquals("playing", state.getPhase());
    }

    @Test
    void testCheckAllDead_EmptyPlayers() {
        state.setPhase("playing");
        deathSystem.checkAllDead(state);
        // 空玩家列表不应改变 phase（代码中有 !isEmpty 检查）
        assertEquals("playing", state.getPhase());
    }

    @Test
    void testPausedPlayerIgnoredByDeathLine() {
        Player player = createActivePlayer("p1", 100, 100);
        player.setPaused(true);
        player.setCameraY(0);
        player.setY(900); // 在死亡线以下
        state.getPlayers().put(player.getId(), player);

        deathSystem.checkDeathLine(state);
        assertEquals(MAX_LIVES, player.getLives());
    }

    private Player createActivePlayer(String id, double x, double y) {
        Player p = new Player(id, "Test");
        p.setX(x);
        p.setY(y);
        p.setWidth(PLAYER_SIZE);
        p.setHeight(PLAYER_SIZE);
        p.setActive(true);
        p.setPaused(false);
        p.setLives(MAX_LIVES);
        p.setScore(0);
        p.setBaseScore(0);
        p.setSide("left");
        return p;
    }
}
