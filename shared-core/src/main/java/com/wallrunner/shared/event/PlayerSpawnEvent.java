package com.wallrunner.shared.event;

/**
 * 玩家重生事件。
 * 
 * 当玩家 lives > 0 时死亡后自动重生触发。
 */
public class PlayerSpawnEvent extends GameEvent {
    private final double spawnY;

    public PlayerSpawnEvent(String playerId, double spawnY) {
        super(EventType.PLAYER_SPAWN, playerId);
        this.spawnY = spawnY;
    }

    public double getSpawnY() { return spawnY; }
}
