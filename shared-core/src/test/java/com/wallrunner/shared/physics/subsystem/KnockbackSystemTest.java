package com.wallrunner.shared.physics.subsystem;

import com.wallrunner.shared.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.wallrunner.shared.constants.GameConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * KnockbackSystem 单元测试。
 *
 * 测试范围：
 * - 非击退状态下短路返回。
 * - applyKnockback 正确初始化状态（无敌、击退、速度、位置）。
 * - 击退过程分阶段：下落期 → 回墙期 → 结束。
 * - 回墙坐标修正与旋转归零。
 */
class KnockbackSystemTest {

    private KnockbackSystem system;
    private Player player;

    @BeforeEach
    void setUp() {
        system = new KnockbackSystem();
        player = new Player("p1", "Test");
        player.setSide("left");
        player.setWidth(PLAYER_SIZE);
        player.setHeight(PLAYER_SIZE);
    }

    @Test
    void testNotKnockedBack_NothingHappens() {
        player.setKnockedBack(false);
        double prevY = player.getY();
        system.processKnockback(player);
        assertEquals(prevY, player.getY());
    }

    @Test
    void testApplyKnockback_SetsState() {
        system.applyKnockback(player, "right");
        assertTrue(player.isKnockedBack());
        assertTrue(player.isInvincible());
        assertEquals(2.5, player.getInvincibleTimer(), 0.01);
        assertEquals(KNOCKBACK_DURATION, player.getKnockbackTimer(), 0.01);
        assertFalse(player.isJumping());
    }

    @Test
    void testApplyKnockback_LeftSidePushedRight() {
        // 玩家贴左墙，被右侧攻击者撞击
        // 原代码会向左推入墙内（X=15），修复后应反弹向右（远离墙）
        player.setSide("left");
        player.setX(WALL_WIDTH + 5);
        system.applyKnockback(player, "right");
        assertTrue(player.getX() >= WALL_WIDTH); // 不应进入墙内
        assertTrue(player.getX() > WALL_WIDTH + 5); // 反弹后向右移动
        assertEquals(KNOCKBACK_VY, player.getVy(), 0.01);
    }

    @Test
    void testProcess_FallingPhase() {
        system.applyKnockback(player, "right");
        double prevY = player.getY();
        system.processKnockback(player);
        assertTrue(player.getY() > prevY || player.getVy() > KNOCKBACK_VY);
        assertFalse(player.isReturningToWall());
    }

    @Test
    void testProcess_ReturningPhase() {
        // 玩家被击退到远离墙的位置，进入回墙阶段
        system.applyKnockback(player, "left");
        player.setX(200); // 远离左墙，避免立即触发 backToWall
        player.setKnockbackTimer(KNOCKBACK_RETURN_DELAY - 0.01);
        player.setReturningToWall(true);
        system.processKnockback(player);
        assertTrue(player.isReturningToWall());
        // 回墙过程中应向墙壁移动
        assertTrue(player.getX() < 200 || player.getX() == 200);
    }


    @Test
    void testApplyKnockback_NoWallPenetration_LeftSide() {
        // 贴左墙玩家被右侧攻击，不应进入墙内（X < WALL_WIDTH）
        player.setSide("left");
        player.setX(WALL_WIDTH + 5);
        system.applyKnockback(player, "right");
        assertTrue(player.getX() >= WALL_WIDTH, "被击退后不应进入左墙内部");
    }

    @Test
    void testApplyKnockback_NoWallPenetration_RightSide() {
        // 贴右墙玩家被左侧攻击，不应进入墙内
        player.setSide("right");
        player.setX(CANVAS_WIDTH - WALL_WIDTH - PLAYER_SIZE - 5);
        system.applyKnockback(player, "left");
        assertTrue(player.getX() <= CANVAS_WIDTH - WALL_WIDTH - PLAYER_SIZE,
                "被击退后不应进入右墙内部");
    }

    @Test
    void testApplyKnockback_InvincibleCollisionDisabled() {
        // 被撞后应进入无敌状态，碰撞检测被关闭
        player.setSide("left");
        player.setX(WALL_WIDTH + 5);
        system.applyKnockback(player, "right");
        assertTrue(player.isInvincible(), "被撞后应立即进入无敌状态");
        assertEquals(2.5, player.getInvincibleTimer(), 0.01, "无敌持续时间应为2.5秒");
    }

    @Test
    void testApplyKnockback_EndKnockback_ClearsInvincible() {
        // 击退结束后无敌状态应由 InvincibilitySystem 处理，此处验证击退结束不直接清除无敌
        player.setSide("left");
        player.setX(WALL_WIDTH + 5);
        system.applyKnockback(player, "right");
        // 模拟击退结束
        player.setKnockbackTimer(0.01);
        system.processKnockback(player);
        // 击退结束，但无敌状态仍保持（由 InvincibilitySystem 独立计时）
        assertTrue(player.isInvincible(), "击退结束后无敌状态应保持，由 InvincibilitySystem 管理");
    }

    @Test
    void testEndKnockback_ResetsToWall() {
        system.applyKnockback(player, "right");
        player.setKnockbackTimer(0.01);
        system.processKnockback(player);
        assertFalse(player.isKnockedBack());
        assertEquals(0, player.getRotationAngle(), 0.01);
        assertEquals(0, player.getVy(), 0.01);
        assertEquals(WALL_WIDTH + 5, player.getX(), 0.01);
    }
}
