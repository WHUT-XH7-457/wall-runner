package com.wallrunner.shared.event;

/**
 * 碰撞事件（已修正）。
 * 
 * 修正内容：根据碰撞类型动态设置 EventType，
 * 支持 COLLISION_OBSTACLE、COLLISION_PLAYER、COLLECTIBLE_PICKUP 三种事件类型。
 */
public class CollisionEvent extends GameEvent {
    private final String entityAId;
    private final String entityBId;
    private final CollisionType collisionType;

    public enum CollisionType {
        PLAYER_OBSTACLE, PLAYER_PLAYER, PLAYER_COLLECTIBLE, PLAYER_WALL
    }

    public CollisionEvent(String sourceId, String entityAId, String entityBId, CollisionType type) {
        super(resolveEventType(type), sourceId);
        this.entityAId = entityAId;
        this.entityBId = entityBId;
        this.collisionType = type;
    }

    private static EventType resolveEventType(CollisionType type) {
        return switch (type) {
            case PLAYER_OBSTACLE -> EventType.COLLISION_OBSTACLE;
            case PLAYER_PLAYER -> EventType.COLLISION_PLAYER;
            case PLAYER_COLLECTIBLE -> EventType.COLLECTIBLE_PICKUP;
            case PLAYER_WALL -> EventType.COLLISION_OBSTACLE;
        };
    }

    public String getEntityAId() { return entityAId; }
    public String getEntityBId() { return entityBId; }
    public CollisionType getCollisionType() { return collisionType; }
}
