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
 * ShieldBreaker - hold the key while aiming at a player who is blocking:
 * swaps to the axe (slot 2), hits when it is charged, swaps back to the sword (slot 1) on release.
 */
public class ShieldBreaker extends Module {
    private static final double RANGE = 4.5;
    private static final int SWORD_SLOT = 0; // hotbar slot 1
    private static final int AXE_SLOT = 1;   // hotbar slot 2

    public ShieldBreaker() {
        super("ShieldBreaker", "Hold to hit blocking players with your axe (slot 2), then back to sword (slot 1).", GLFW.GLFW_KEY_Z, true);
    }

    @Override
    protected void onDisable(MinecraftClient mc) {
        if (mc.player != null) mc.player.getInventory().setSelectedSlot(SWORD_SLOT);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null || mc.currentScreen != null) return;

        Entity target = targetedEntity(mc);
        boolean blocking = target instanceof LivingEntity le && le.isBlocking();

        if (!blocking) {
            p.getInventory().setSelectedSlot(SWORD_SLOT);
            return;
        }

        p.getInventory().setSelectedSlot(AXE_SLOT);
        if (p.getAttackCooldownProgress(0.5f) >= 0.9f) {
            mc.interactionManager.attackEntity(p, target);
            p.swingHand(Hand.MAIN_HAND);
        }
    }

    private Entity targetedEntity(MinecraftClient mc) {
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            Entity e = ((EntityHitResult) mc.crosshairTarget).getEntity();
            if (e instanceof LivingEntity le && le.isAlive() && mc.player.distanceTo(e) <= RANGE) return e;
        }
        return null;
    }
}
