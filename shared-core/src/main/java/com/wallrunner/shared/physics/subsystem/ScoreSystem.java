package com.wallrunner.shared.physics.subsystem;

import com.wallrunner.shared.entity.GameState;
import com.wallrunner.shared.entity.Player;
import com.wallrunner.shared.event.GameEventBus;
import com.wallrunner.shared.event.ScoreChangeEvent;

import java.util.List;

/**
 * 计分系统实现（已修正）。
 * 
 * 修正内容：
 * 1. 添加 GameEventBus 依赖
 * 2. 在 recalculateScores() 中发布 ScoreChangeEvent（分数变化时）
 * 3. 在 applyTimeBonus() 中发布 ScoreChangeEvent（时间奖励时）
 */
public class ScoreSystem implements IScoreCalculator {

    private final GameEventBus eventBus;

    public ScoreSystem() {
        this.eventBus = GameEventBus.getInstance();
    }

    public ScoreSystem(GameEventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void recalculateScores(List<Player> activePlayers) {
        for (Player p : activePlayers) {
            int oldScore = p.getScore();
            int heightScore = (int) ((p.getJoinOffsetY() - p.getY()) / 10.0);
            int total = p.getBaseScore() + Math.max(0, heightScore) + p.getTimeBonusScore() + p.getCoinsCollected();
            p.setScore(total);
            if (total != oldScore) {
                eventBus.publish(new ScoreChangeEvent(p.getId(), oldScore, total));
            }
        }
    }

    @Override
    public void applyTimeBonus(GameState state, List<Player> activePlayers) {
        double interval = state.getTimeBonusInterval();
        if (interval <= 0) return;
        state.setTimeBonusAccumulator(state.getTimeBonusAccumulator() + 0.016);
        if (state.getTimeBonusAccumulator() >= interval) {
            int points = state.getTimeBonusPoints();
            for (Player p : activePlayers) {
                if (!p.isPaused()) {
                    int oldScore = p.getScore();
                    p.setTimeBonusScore(p.getTimeBonusScore() + points);
                    eventBus.publish(new ScoreChangeEvent(p.getId(), oldScore, p.getScore()));
                }
            }
            state.setTimeBonusAccumulator(state.getTimeBonusAccumulator() - interval);
        }
    }
}
