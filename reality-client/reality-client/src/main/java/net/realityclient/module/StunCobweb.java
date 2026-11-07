package net.realityclient.module;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

/**
 * Stuncobweb - hold the key while aiming at a player:
 *   1. SWORD hit, then AXE hit instantly (same tick) = stun / shield break
 *   2. after the delay, place a COBWEB under them while they are stunned
 * Then it repeats until you let go.
 * Delay = ticks before the web / before the next combo (change it in the GUI, lower is faster).
 */
public class StunCobweb extends Module {
    private enum Step { COMBO, WEB }

    private static final double RANGE = 4.5;

    private Step step = Step.COMBO;
    private int cooldown = 0;
    private int previousSlot = -1;

    public StunCobweb() {
        super("Stuncobweb", "Sword + axe hit instantly (stun), then webs them. Needs sword + axe + cobweb in hotbar.", GLFW.GLFW_KEY_C, true);
        enableDelay(3, 1, 20);
    }

    @Override
    protected void onEnable(MinecraftClient mc) {
        step = Step.COMBO;
        cooldown = 0;
        if (mc.player != null) previousSlot = mc.player.getInventory().getSelectedSlot();
    }

    @Override
    protected void onDisable(MinecraftClient mc) {
        if (mc.player != null && previousSlot >= 0) {
            mc.player.getInventory().setSelectedSlot(previousSlot);
        }
        previousSlot = -1;
    }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;

        if (cooldown > 0) { cooldown--; return; }

        Entity target = targetedEntity(mc);
        if (target == null) { step = Step.COMBO; return; }

        int swordSlot = -1;
        int axeSlot = -1;
        int webSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = p.getInventory().getStack(i);
            if (s.isIn(ItemTags.SWORDS) && swordSlot == -1) swordSlot = i;
            if (s.isIn(ItemTags.AXES) && axeSlot == -1) axeSlot = i;
            if (s.isOf(Items.COBWEB) && webSlot == -1) webSlot = i;
        }

        switch (step) {
            case COMBO -> {
                // sword hit
                if (swordSlot != -1) {
                    p.getInventory().setSelectedSlot(swordSlot);
                    mc.interactionManager.attackEntity(p, target);
                    p.swingHand(Hand.MAIN_HAND);
                }
                // axe hit instantly after (same tick) = stun
                if (axeSlot != -1) {
                    p.getInventory().setSelectedSlot(axeSlot);
                    mc.interactionManager.attackEntity(p, target);
                    p.swingHand(Hand.MAIN_HAND);
                }
                step = Step.WEB;
                cooldown = getDelay();
            }
            case WEB -> {
                BlockPos feet = target.getBlockPos();
                BlockPos below = feet.down();
                boolean canPlace = !mc.world.getBlockState(below).isAir()
                        && mc.world.getBlockState(feet).isAir()
                        && !mc.world.getBlockState(feet).isOf(Blocks.COBWEB);
                if (canPlace && webSlot != -1) {
                    p.getInventory().setSelectedSlot(webSlot);
                    Vec3d hit = Vec3d.ofCenter(below).add(0, 0.5, 0);
                    BlockHitResult bhr = new BlockHitResult(hit, Direction.UP, below, false);
                    mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, bhr);
                    p.swingHand(Hand.MAIN_HAND);
                }
                step = Step.COMBO;
                cooldown = getDelay();
            }
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
