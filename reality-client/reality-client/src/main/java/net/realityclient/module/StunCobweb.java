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
 * Stuncobweb - matches the stun_web macro:
 *   axe in hotbar slot 2, sword in hotbar slot 1.
 *   On key press: select slot 2 (axe), 3 left clicks (stun / shield break),
 *   then swap back to slot 1 (sword) so it can be chained.
 *   You place the cobweb yourself afterwards.
 */
public class StunCobweb extends Module {
    private static final double RANGE = 4.5;
    private static final int AXE_SLOT = 1;    // hotbar slot 2
    private static final int SWORD_SLOT = 0;  // hotbar slot 1
    private static final int CLICKS = 3;

    public StunCobweb() {
        super("Stuncobweb", "Shield stun: axe (slot 2) x3 hits, then back to sword (slot 1). Then web them.", GLFW.GLFW_KEY_C, true);
    }

    @Override
    protected void onEnable(MinecraftClient mc) {
        run(mc);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        // fires once per key press (see onEnable)
    }

    private void run(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null || mc.currentScreen != null) return;

        Entity target = targetedEntity(mc);

        p.getInventory().setSelectedSlot(AXE_SLOT);
        for (int i = 0; i < CLICKS; i++) {
            if (target != null) mc.interactionManager.attackEntity(p, target);
            p.swingHand(Hand.MAIN_HAND);
        }
        p.getInventory().setSelectedSlot(SWORD_SLOT);
    }

    private Entity targetedEntity(MinecraftClient mc) {
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            Entity e = ((EntityHitResult) mc.crosshairTarget).getEntity();
            if (e instanceof LivingEntity le && le.isAlive() && mc.player.distanceTo(e) <= RANGE) return e;
        }
        return null;
    }
}
