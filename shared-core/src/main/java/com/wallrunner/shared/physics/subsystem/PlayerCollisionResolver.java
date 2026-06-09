package com.wallrunner.shared.physics.subsystem;

import com.wallrunner.shared.entity.Player;
import com.wallrunner.shared.event.CollisionEvent;
import com.wallrunner.shared.event.GameEventBus;

import static com.wallrunner.shared.constants.GameConstants.*;

/**
 * 玩家间碰撞解析器实现（已修正）。
 * 
 * 修正内容：
 * 1. 添加 GameEventBus 依赖
 * 2. 在 resolvePlayerCollision() 中发布 CollisionEvent(PLAYER_PLAYER)
 * 
 * UML 建模意义：IPlayerCollisionResolver 的具体实现，展示策略分支 + Observer 模式。
 */
public class PlayerCollisionResolver implements IPlayerCollisionResolver {

    private final IKnockbackSystem knockbackSystem;
    private final GameEventBus eventBus;

    public PlayerCollisionResolver(IKnockbackSystem knockbackSystem) {
        this(knockbackSystem, GameEventBus.getInstance());
    }

    public PlayerCollisionResolver(IKnockbackSystem knockbackSystem, GameEventBus eventBus) {
        this.knockbackSystem = knockbackSystem;
        this.eventBus = eventBus;
    }

    @Override
    public boolean checkPlayerCollision(Player a, Player b) {
        return a.getX() < b.getX() + b.getWidth() && a.getX() + a.getWidth() > b.getX()
                && a.getY() < b.getY() + b.getHeight() && a.getY() + a.getHeight() > b.getY();
    }

    @Override
    public void resolvePlayerCollision(Player a, Player b) {
        boolean aInvincible = a.isInvincible();
        boolean bInvincible = b.isInvincible();

        if (aInvincible && bInvincible) return;

        // 发布碰撞事件
        eventBus.publish(new CollisionEvent("physics", a.getId(), b.getId(), CollisionEvent.CollisionType.PLAYER_PLAYER));

        if (aInvincible || bInvincible) {
            resolveInvincibleCollision(aInvincible ? a : b, aInvincible ? b : a);
            return;
        }

        resolveNormalCollision(a, b);
    }

    private void resolveInvincibleCollision(Player invincible, Player normal) {
        boolean invincibleJumping = invincible.isJumping();
        boolean normalJumping = normal.isJumping();

        if (invincibleJumping) {
            knockbackSystem.applyKnockback(normal, invincible.getSide());
        } else if (normalJumping) {
            knockbackSystem.applyKnockback(normal, normal.getSide());
        }
    }

    private void resolveNormalCollision(Player a, Player b) {
        double dx = (a.getX() + a.getWidth() / 2) - (b.getX() + b.getWidth() / 2);
        double dy = (a.getY() + a.getHeight() / 2) - (b.getY() + b.getHeight() / 2);
        double overlapX = (a.getWidth() + b.getWidth()) / 2 - Math.abs(dx);
        double overlapY = (a.getHeight() + b.getHeight()) / 2 - Math.abs(dy);

        boolean aJumping = a.isJumping();
        boolean bJumping = b.isJumping();

        if (aJumping && bJumping) {
            if (dx > 0) {
                knockbackSystem.applyKnockback(a, "left");
                knockbackSystem.applyKnockback(b, "right");
            } else {
                knockbackSystem.applyKnockback(a, "right");
                knockbackSystem.applyKnockback(b, "left");
            }
            return;
        } else if (aJumping && !bJumping) {
            knockbackSystem.applyKnockback(b, a.getSide());
            double pushX = "left".equals(a.getSide()) ? JUMP_SPEED * 0.8 : -JUMP_SPEED * 0.8;
            a.setX(a.getX() + pushX);
        } else if (bJumping && !aJumping) {
            knockbackSystem.applyKnockback(a, b.getSide());
            double pushX = "left".equals(b.getSide()) ? JUMP_SPEED * 0.8 : -JUMP_SPEED * 0.8;
            b.setX(b.getX() + pushX);
        }

        if (overlapX < overlapY) {
            double shift = overlapX / 2;
            a.setX(a.getX() + (dx > 0 ? shift : -shift));
            b.setX(b.getX() + (dx > 0 ? -shift : shift));
        } else {
            double shift = overlapY / 2;
            a.setY(a.getY() + (dy > 0 ? shift : -shift));
            b.setY(b.getY() + (dy > 0 ? -shift : shift));
        }
    }
}
