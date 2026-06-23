package com.wallrunner.shared.physics.subsystem;

import com.wallrunner.shared.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.wallrunner.shared.constants.GameConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * InvincibilitySystem 单元测试。
 *
 * 测试范围：
 * - 非无敌状态下短路返回。
 * - 无敌计时器正常递减。
 * - 计时器归零后清除无敌状态。
 * - 道具(COLLECTIBLE_A)倒计时与效果清除。
 */
class InvincibilitySystemTest {

    private InvincibilitySystem system;
    private Player player;

    @BeforeEach
    void setUp() {
        system = new InvincibilitySystem();
        player = new Player("p1", "Test");
    }

    @Test
    void testNotInvincible_NothingHappens() {
        player.setInvincible(false);
        system.processInvincibility(player);
        assertFalse(player.isInvincible());
        assertEquals(0, player.getInvincibleTimer(), 0.01);
    }

    @Test
    void testTimerDecrements() {
        player.setInvincible(true);
        player.setInvincibleTimer(1.0);
        system.processInvincibility(player);
        assertTrue(player.isInvincible());
        assertEquals(0.984, player.getInvincibleTimer(), 0.001);
    }

    @Test
    void testTimerExpires_ClearsInvincible() {
        player.setInvincible(true);
        player.setInvincibleTimer(0.01);
        system.processInvincibility(player);
        assertFalse(player.isInvincible());
        assertEquals(0, player.getInvincibleTimer(), 0.01);
    }

    @Test
    void testPowerUpCountdown() {
        player.setInvincible(true);
        player.setInvincibleTimer(5.0);
        player.setActivePowerUp(COLLECTIBLE_A);
        player.setPowerUpTimer(0.02);
        player.getEffects().add("rainbow_sparkle");
        system.processInvincibility(player);
        assertEquals(0.004, player.getPowerUpTimer(), 0.001);
        assertTrue(player.getEffects().contains("rainbow_sparkle"));
    }

    @Test
    void testPowerUpExpires_ClearsEffect() {
        player.setInvincible(true);
        player.setInvincibleTimer(5.0);
        player.setActivePowerUp(COLLECTIBLE_A);
        player.setPowerUpTimer(0.01);
        player.getEffects().add("rainbow_sparkle");
        system.processInvincibility(player);
        assertEquals("", player.getActivePowerUp());
        assertFalse(player.getEffects().contains("rainbow_sparkle"));
    }
}
