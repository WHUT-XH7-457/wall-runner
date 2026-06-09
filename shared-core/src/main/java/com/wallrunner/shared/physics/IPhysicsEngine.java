package com.wallrunner.shared.physics;

import com.wallrunner.shared.entity.GameState;
import com.wallrunner.shared.entity.Player;

/**
 * 物理引擎接口。
 * 
 * UML 建模意义：策略模式的核心接口，便于展示不同物理实现的可替换性。
 * 设计原则：依赖倒置（DIP）、接口隔离（ISP）。
 */
/**
 * 物理引擎接口。
 *
 * UML 建模意义：策略模式的核心接口，便于展示不同物理实现的可替换性。
 * 设计原则：依赖倒置（DIP）、接口隔离（ISP）。
 */
public interface IPhysicsEngine {
    void initState(GameState state);
    void update(GameState state);
    void handleInput(Player player, String inputType);
    void startGame(GameState state);
    void initJoiningPlayer(GameState state, Player player);

    /**
     * 完全死亡后重生（lives <= 0 → spectator 后手动触发）。
     * 重置分数、生命、高度基准，并将玩家放置到最末端活跃玩家后方。
     * 与 initJoiningPlayer 的区别：重生位置 = 最末端玩家 Y + 300（高度数值 -30）。
     */
    void respawnPlayer(GameState state, Player player);
}
