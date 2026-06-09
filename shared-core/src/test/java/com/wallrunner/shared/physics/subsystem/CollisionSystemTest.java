package com.wallrunner.shared.physics.subsystem;

import com.wallrunner.shared.entity.Obstacle;
import com.wallrunner.shared.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CollisionSystem 单元测试。
 *
 * 测试范围：
 * - AABB 矩形相交检测（各种位置关系）。
 * - 玩家与障碍物列表碰撞检测。
 * - 玩家间碰撞检测。
 */
class CollisionSystemTest {

    private CollisionSystem collisionSystem;

    @BeforeEach
    void setUp() {
        collisionSystem = new CollisionSystem();
    }

    @Test
    void testRectIntersect_Overlap() {
        assertTrue(collisionSystem.rectIntersect(0, 0, 10, 10, 5, 5, 10, 10));
    }

    @Test
    void testRectIntersect_NoOverlap() {
        assertFalse(collisionSystem.rectIntersect(0, 0, 10, 10, 20, 20, 10, 10));
    }

    @Test
    void testRectIntersect_Containment() {
        assertTrue(collisionSystem.rectIntersect(0, 0, 20, 20, 5, 5, 5, 5));
    }

    @Test
    void testRectIntersect_EdgeTouch_NotIntersect() {
        // 代码中使用的是严格 <，因此边界接触不算相交
        assertFalse(collisionSystem.rectIntersect(0, 0, 10, 10, 10, 0, 10, 10));
        assertFalse(collisionSystem.rectIntersect(0, 0, 10, 10, 0, 10, 10, 10));
    }

    @Test
    void testCheckPlayerObstacleCollision_Hit() {
        Player player = new Player("p1", "Test");
        player.setX(0);
        player.setY(0);
        player.setWidth(10);
        player.setHeight(10);

        Obstacle obs = new Obstacle();
        obs.setX(5);
        obs.setY(5);
        obs.setWidth(10);
        obs.setHeight(10);

        List<Obstacle> obstacles = List.of(obs);
        assertTrue(collisionSystem.checkPlayerObstacleCollision(player, obstacles));
    }

    @Test
    void testCheckPlayerObstacleCollision_NoHit() {
        Player player = new Player("p1", "Test");
        player.setX(0);
        player.setY(0);
        player.setWidth(10);
        player.setHeight(10);

        Obstacle obs = new Obstacle();
        obs.setX(20);
        obs.setY(20);
        obs.setWidth(10);
        obs.setHeight(10);

        List<Obstacle> obstacles = List.of(obs);
        assertFalse(collisionSystem.checkPlayerObstacleCollision(player, obstacles));
    }

    @Test
    void testCheckPlayerObstacleCollision_EmptyList() {
        Player player = new Player("p1", "Test");
        assertFalse(collisionSystem.checkPlayerObstacleCollision(player, new ArrayList<>()));
    }

    @Test
    void testCheckPlayerPlayerCollision_Collide() {
        Player a = new Player("p1", "A");
        a.setX(0);
        a.setY(0);
        a.setWidth(10);
        a.setHeight(10);

        Player b = new Player("p2", "B");
        b.setX(5);
        b.setY(5);
        b.setWidth(10);
        b.setHeight(10);

        assertTrue(collisionSystem.checkPlayerPlayerCollision(a, b));
    }

    @Test
    void testCheckPlayerPlayerCollision_NoCollide() {
        Player a = new Player("p1", "A");
        a.setX(0);
        a.setY(0);
        a.setWidth(10);
        a.setHeight(10);

        Player b = new Player("p2", "B");
        b.setX(100);
        b.setY(100);
        b.setWidth(10);
        b.setHeight(10);

        assertFalse(collisionSystem.checkPlayerPlayerCollision(a, b));
    }
}
