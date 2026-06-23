package com.wallrunner.shared.physics.subsystem;

import com.wallrunner.shared.entity.Player;
import com.wallrunner.shared.event.CollisionEvent;
import com.wallrunner.shared.event.GameEvent;
import com.wallrunner.shared.event.GameEventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.wallrunner.shared.constants.GameConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PlayerCollisionResolver 单元测试。
 *
 * 测试范围：
 * - AABB 碰撞检测（重叠/不重叠）。
 * - 双方无敌时短路返回，无击退。
 * - 单方无敌时，普通玩家被击退。
 * - 双方跳跃时对向击退。
 * - 碰撞事件通过 GameEventBus 发布。
 */
class PlayerCollisionResolverTest {

    private PlayerCollisionResolver resolver;
    private GameEventBus eventBus;

    @BeforeEach
    void setUp() {
        eventBus = GameEventBus.getInstance();
        eventBus.clear();
        resolver = new PlayerCollisionResolver(new KnockbackSystem(), eventBus);
    }

    @Test
    void testCheckCollision_Overlap() {
        Player a = createPlayer(0, 0);
        Player b = createPlayer(5, 5);
        assertTrue(resolver.checkPlayerCollision(a, b));
    }

    @Test
    void testCheckCollision_NoOverlap() {
        Player a = createPlayer(0, 0);
        Player b = createPlayer(100, 100);
        assertFalse(resolver.checkPlayerCollision(a, b));
    }

    @Test
    void testBothInvincible_NoAction() {
        Player a = createPlayer(0, 0); a.setInvincible(true);
        Player b = createPlayer(5, 5); b.setInvincible(true);
        resolver.resolvePlayerCollision(a, b);
        assertFalse(a.isKnockedBack());
        assertFalse(b.isKnockedBack());
    }

    @Test
    void testOneInvincible_OtherKnockedBack() {
        Player inv = createPlayer(0, 0);
        inv.setInvincible(true); inv.setSide("left"); inv.setJumping(true);
        Player norm = createPlayer(5, 5); norm.setSide("right");
        resolver.resolvePlayerCollision(inv, norm);
        assertTrue(norm.isKnockedBack());
    }

    @Test
    void testBothJumping_OppositeKnockback() {
        Player a = createPlayer(0, 0); a.setJumping(true); a.setSide("left");
        Player b = createPlayer(5, 5); b.setJumping(true); b.setSide("right");
        resolver.resolvePlayerCollision(a, b);
        assertTrue(a.isKnockedBack());
        assertTrue(b.isKnockedBack());
    }

    @Test
    void testCollisionEventPublished() {
        Player a = createPlayer(0, 0); a.setId("p1");
        Player b = createPlayer(5, 5); b.setId("p2");
        final boolean[] received = {false};
        eventBus.subscribe(GameEvent.EventType.COLLISION_PLAYER, e -> received[0] = true);
        resolver.resolvePlayerCollision(a, b);
        assertTrue(received[0]);
    }

    private Player createPlayer(double x, double y) {
        Player p = new Player("id", "Test");
        p.setX(x); p.setY(y); p.setWidth(PLAYER_SIZE); p.setHeight(PLAYER_SIZE);
        return p;
    }
}
