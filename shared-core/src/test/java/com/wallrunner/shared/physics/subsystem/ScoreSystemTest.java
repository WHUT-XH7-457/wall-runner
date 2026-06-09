package com.wallrunner.shared.physics.subsystem;

import com.wallrunner.shared.entity.GameState;
import com.wallrunner.shared.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ScoreSystem 单元测试。
 *
 * 测试范围：
 * - 高度分数计算（joinOffsetY - currentY）。
 * - 时间奖励累加与触发。
 * - 多玩家分数独立计算。
 */
class ScoreSystemTest {

    private ScoreSystem scoreSystem;
    private GameState state;

    @BeforeEach
    void setUp() {
        scoreSystem = new ScoreSystem();
        state = new GameState();
        state.setTimeBonusInterval(5.0);
        state.setTimeBonusPoints(10);
    }

    @Test
    void testRecalculateScores_HeightScore() {
        Player p = new Player("p1", "Test");
        p.setJoinOffsetY(100);
        p.setY(0); // 上升了 100 像素
        p.setBaseScore(0);
        p.setTimeBonusScore(0);
        p.setCoinsCollected(0);

        scoreSystem.recalculateScores(List.of(p));
        // heightScore = (100 - 0) / 10 = 10
        assertEquals(10, p.getScore());
    }

    @Test
    void testRecalculateScores_NegativeHeightScoreClamped() {
        Player p = new Player("p1", "Test");
        p.setJoinOffsetY(0);
        p.setY(100); // 下降了，高度分数应为负，但被 max(0, ...) 截断
        p.setBaseScore(5);
        p.setTimeBonusScore(3);
        p.setCoinsCollected(2);

        scoreSystem.recalculateScores(List.of(p));
        assertEquals(10, p.getScore()); // 5 + 0 + 3 + 2
    }

    @Test
    void testRecalculateScores_WithBaseAndBonus() {
        Player p = new Player("p1", "Test");
        p.setJoinOffsetY(200);
        p.setY(50);
        p.setBaseScore(20);
        p.setTimeBonusScore(15);
        p.setCoinsCollected(5);

        scoreSystem.recalculateScores(List.of(p));
        // heightScore = (200 - 50) / 10 = 15
        assertEquals(55, p.getScore()); // 20 + 15 + 15 + 5
    }

    @Test
    void testApplyTimeBonus_Accumulates() {
        Player p = new Player("p1", "Test");
        p.setPaused(false);

        // 第一次调用，累加 0.016
        scoreSystem.applyTimeBonus(state, List.of(p));
        assertEquals(0, p.getTimeBonusScore());
        assertTrue(state.getTimeBonusAccumulator() > 0);

        // 继续累加直到超过 interval
        for (int i = 0; i < 400; i++) {
            scoreSystem.applyTimeBonus(state, List.of(p));
        }
        assertTrue(p.getTimeBonusScore() > 0);
    }

    @Test
    void testApplyTimeBonus_PausedPlayerNoBonus() {
        Player p = new Player("p1", "Test");
        p.setPaused(true);

        // 累加足够多次以触发奖励
        for (int i = 0; i < 400; i++) {
            scoreSystem.applyTimeBonus(state, List.of(p));
        }
        assertEquals(0, p.getTimeBonusScore());
    }

    @Test
    void testApplyTimeBonus_ZeroInterval() {
        state.setTimeBonusInterval(0);
        Player p = new Player("p1", "Test");
        p.setPaused(false);

        scoreSystem.applyTimeBonus(state, List.of(p));
        assertEquals(0, p.getTimeBonusScore());
    }

    @Test
    void testRecalculateScores_MultiplePlayers() {
        Player a = new Player("p1", "A");
        a.setJoinOffsetY(100);
        a.setY(0);
        a.setBaseScore(0);
        a.setTimeBonusScore(0);
        a.setCoinsCollected(0);

        Player b = new Player("p2", "B");
        b.setJoinOffsetY(50);
        b.setY(0);
        b.setBaseScore(5);
        b.setTimeBonusScore(0);
        b.setCoinsCollected(0);

        scoreSystem.recalculateScores(List.of(a, b));
        assertEquals(10, a.getScore());
        assertEquals(10, b.getScore()); // 5 + 5
    }
}
