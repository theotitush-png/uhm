package net.realityclient.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

/**
 * ShieldBreaker - hold the key while aiming at a blocking player:
 * hit once with the sword, then immediately switch to the axe for the shield-break hit.
 */
public class ShieldBreaker extends Module {
    private static final double RANGE = 4.5;
    private static final int SWORD_SLOT = 0; // hotbar slot 1
    private static final int AXE_SLOT = 1;   // hotbar slot 2

    private enum State { IDLE, SWORD, AXE }
    private State state = State.IDLE;
    private Entity target;

    public ShieldBreaker() {
        super("ShieldBreaker", "Sword hit followed by an axe shield-break hit (slots 1 and 2).", GLFW.GLFW_KEY_Z, true);
    }

    @Override
    protected void onEnable(MinecraftClient mc) {
        target = targetedEntity(mc);
        state = target == null ? State.IDLE : State.SWORD;
    }

    @Override
    protected void onDisable(MinecraftClient mc) {
        if (mc.player != null) mc.player.getInventory().setSelectedSlot(SWORD_SLOT);
        state = State.IDLE;
        target = null;
    }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null || mc.currentScreen != null) return;

        if (state == State.IDLE) {
            Entity aimed = targetedEntity(mc);
            if (aimed instanceof LivingEntity le && le.isBlocking()) {
                target = aimed;
                state = State.SWORD;
            } else {
                p.getInventory().setSelectedSlot(SWORD_SLOT);
                return;
            }
        }

        if (target == null || !target.isAlive() || p.distanceTo(target) > RANGE + 1.0) {
            reset(p);
            return;
        }

        if (!(target instanceof LivingEntity le) || !le.isBlocking()) {
            reset(p);
            return;
        }

        if (p.getAttackCooldownProgress(0.5f) < 0.9f) return;

        if (state == State.SWORD) {
            p.getInventory().setSelectedSlot(SWORD_SLOT);
            mc.interactionManager.attackEntity(p, target);
            p.swingHand(Hand.MAIN_HAND);
            state = State.AXE;
            return;
        }

        if (state == State.AXE) {
            p.getInventory().setSelectedSlot(AXE_SLOT);
            mc.interactionManager.attackEntity(p, target);
            p.swingHand(Hand.MAIN_HAND);
            reset(p);
        }
    }

    private void reset(ClientPlayerEntity p) {
        p.getInventory().setSelectedSlot(SWORD_SLOT);
        state = State.IDLE;
        target = null;
    }

    private Entity targetedEntity(MinecraftClient mc) {
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            Entity e = ((EntityHitResult) mc.crosshairTarget).getEntity();
            if (e instanceof LivingEntity le && le.isAlive() && mc.player.distanceTo(e) <= RANGE) return e;
        }
        return null;
    }
}
