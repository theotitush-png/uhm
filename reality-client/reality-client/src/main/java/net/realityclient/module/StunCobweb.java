package net.realityclient.module;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

/**
 * Stuncobweb - stun web combo:
 *   sword in hotbar slot 1, axe in slot 2, cobwebs in slot 8.
 *   On key press: select slot 2 (axe), 3 left clicks (stun / shield break),
 *   select slot 8 (cobweb) and place a web under the target,
 *   then swap back to slot 1 (sword) so it can be chained.
 */
public class StunCobweb extends Module {
    private static final double RANGE = 4.5;
    private static final int SWORD_SLOT = 0;  // hotbar slot 1
    private static final int AXE_SLOT = 1;    // hotbar slot 2
    private static final int WEB_SLOT = 7;    // hotbar slot 8
    private static final int CLICKS = 3;

    public StunCobweb() {
        super("Stuncobweb", "Axe (slot 2) x3 stun, cobweb (slot 8) under them, back to sword (slot 1).", GLFW.GLFW_KEY_C, true);
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
        if (p == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;

        Entity target = targetedEntity(mc);

        // 1. Axe stun: 3 hits
        p.getInventory().setSelectedSlot(AXE_SLOT);
        for (int i = 0; i < CLICKS; i++) {
            if (target != null) mc.interactionManager.attackEntity(p, target);
            p.swingHand(Hand.MAIN_HAND);
        }

        // 2. Cobweb under them
        if (target != null) {
            BlockPos feet = target.getBlockPos();
            BlockPos below = feet.down();
            boolean canPlace = !mc.world.getBlockState(below).isAir()
                    && mc.world.getBlockState(feet).isAir()
                    && !mc.world.getBlockState(feet).isOf(Blocks.COBWEB);
            if (canPlace) {
                p.getInventory().setSelectedSlot(WEB_SLOT);
                Vec3d hit = Vec3d.ofCenter(below).add(0, 0.5, 0);
                BlockHitResult bhr = new BlockHitResult(hit, Direction.UP, below, false);
                mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, bhr);
                p.swingHand(Hand.MAIN_HAND);
            }
        }

        // 3. Back to the sword
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
