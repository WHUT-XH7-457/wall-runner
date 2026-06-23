package com.wallrunner.shared.physics.subsystem;

import static com.wallrunner.shared.constants.GameConstants.CAMERA_OFFSET_RATIO;
import static com.wallrunner.shared.constants.GameConstants.CANVAS_HEIGHT;
import static com.wallrunner.shared.constants.GameConstants.CANVAS_WIDTH;
import static com.wallrunner.shared.constants.GameConstants.DEATH_LINE_OFFSET;
import static com.wallrunner.shared.constants.GameConstants.WALL_WIDTH;
import com.wallrunner.shared.entity.GameState;
import com.wallrunner.shared.entity.Player;
import com.wallrunner.shared.event.GameEventBus;
import com.wallrunner.shared.event.PlayerDeathEvent;
import com.wallrunner.shared.event.PlayerSpawnEvent;

/**
 * 死亡与重生系统实现（已修正）。
 *
 * 修正内容：
 * 1. 添加 GameEventBus 依赖，通过事件总线发布 PlayerDeathEvent 和 PlayerSpawnEvent
 * 2. applyDeath() 中发布 PlayerDeathEvent（lives <= 0 时）
 * 3. respawnPlayer() 中发布 PlayerSpawnEvent（lives > 0 时）
 *
 * 机制要点（避免与 GameController.onRestart / PhysicsEngine.initJoiningPlayer 混淆）：
 * 1. 丢心复活（本类 respawnPlayer）：lives > 0 时死亡自动触发。
 *    - 保留高度与总分：joinOffsetY 不变，baseScore 继承死亡前 score。
 *    - 复活位置 = 玩家自身当前 Y - 100（对应高度数值 +10，即向前推进 10）。
 *    - 仅重置局内临时加分（timeBonusScore / coinsCollected）。
 * 2. 完全死亡后重生（GameController.onRestart）：lives <= 0 → spectator 后手动触发。
 *    - 重置分数：score = 0, baseScore = 0。
 *    - 重生位置 = 最末端活跃玩家的 Y + 300（该玩家高度数值 -30）。
 *    - joinOffsetY 同步重置到重生位置，高度重新累计。
 * 3. 新玩家加入（PhysicsEngine.initJoiningPlayer）：
 *    - 与"完全死亡后重生"逻辑类似，高度从加入位置重新开始累计。
 *
 * UML 建模意义：IDeathSystem 的具体实现，展示生命状态机 + Observer 模式。
 */
public class DeathSystem implements IDeathSystem {

    private final GameEventBus eventBus;

    public DeathSystem() {
        this.eventBus = GameEventBus.getInstance();
    }

    public DeathSystem(GameEventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void checkDeathLine(GameState state) {
        for (Player player : state.getPlayers().values()) {
            if (!player.isActive() || player.isPaused()) continue;
            double deathLine = player.getCameraY() + CANVAS_HEIGHT + DEATH_LINE_OFFSET;
            if (player.getY() > deathLine) {
                applyDeath(state, player);
            }
        }
    }

    @Override
    public void applyDeath(GameState state, Player player) {
        int oldLives = player.getLives();
        player.setLives(oldLives - 1);
        if (player.getLives() <= 0) {
            if (player.getScore() > player.getHighScore()) {
                player.setHighScore(player.getScore());
            }
            player.setActive(false);
            player.setSpectator(true);
            eventBus.publish(new PlayerDeathEvent(player.getId(), 0, player.getScore()));
        } else {
            eventBus.publish(new PlayerDeathEvent(player.getId(), player.getLives(), player.getScore()));
            respawnPlayer(state, player);
        }
    }

    /**
     * 丢心复活：lives > 0 时自动调用。
     * 保留高度与总分，仅向前推进 100 像素（高度数值 +10）并换边。
     * 注：采用"前进"而非"后退"，避免玩家复活在障碍物密集区导致连续死亡。
     */
    private void respawnPlayer(GameState state, Player player) {
        double spawnY = player.getY() - 100;
        player.setJumping(false);
        player.setVy(0);
        player.setSide("left".equals(player.getSide()) ? "right" : "left");
        player.setX("left".equals(player.getSide()) ? WALL_WIDTH : CANVAS_WIDTH - WALL_WIDTH - player.getWidth());
        player.setY(spawnY);
        player.setBlocked(false);
        player.setPaused(false);
        player.setKnockedBack(false);
        player.setInvincible(true);
        player.setInvincibleTimer(2.0);
        player.setSpectator(false);
        player.setBaseScore(player.getScore());
        player.setTimeBonusScore(0);
        player.setCoinsCollected(0);
        double spawnCamY = spawnY - CANVAS_HEIGHT * CAMERA_OFFSET_RATIO;
        player.setCameraY(spawnCamY);
        player.setCameraTargetY(spawnCamY);
        eventBus.publish(new PlayerSpawnEvent(player.getId(), spawnY));
    }

    @Override
    public void checkAllDead(GameState state) {
        boolean anyActive = false;
        for (Player p : state.getPlayers().values()) {
            if (p.isActive()) {
                anyActive = true;
                break;
            }
        }
        if (!anyActive && !state.getPlayers().isEmpty()) {
            state.setPhase("gameover");
        }
    }
}
