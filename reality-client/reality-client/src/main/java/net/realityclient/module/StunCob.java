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
 * StunCob - cobweb stun combo with shield break.
 * Hold the key while aiming at a player:
 *   1. places a cobweb on them
 *   2. if they are blocking with a shield, hits them with an axe to break it
 *   3. otherwise hits with your sword
 * Delay = ticks between steps (change it in the GUI, lower is faster).
 */
public class StunCob extends Module {
    private static final double RANGE = 4.5;

    private int cooldown = 0;
    private int previousSlot = -1;

    public StunCob() {
        super("StunCob", "Webs the target, breaks their shield with an axe, then hits. Needs cobweb + sword + axe in hotbar.", GLFW.GLFW_KEY_C, true);
        enableDelay(3, 1, 20);
    }

    @Override
    protected void onEnable(MinecraftClient mc) {
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
        if (target == null) return;

        int webSlot = -1;
        int swordSlot = -1;
        int axeSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = p.getInventory().getStack(i);
            if (s.isOf(Items.COBWEB) && webSlot == -1) webSlot = i;
            if (s.isIn(ItemTags.SWORDS) && swordSlot == -1) swordSlot = i;
            if (s.isIn(ItemTags.AXES) && axeSlot == -1) axeSlot = i;
        }

        BlockPos feet = target.getBlockPos();
        BlockPos below = feet.down();
        boolean alreadyWebbed = mc.world.getBlockState(feet).isOf(Blocks.COBWEB);
        boolean canPlace = !mc.world.getBlockState(below).isAir() && mc.world.getBlockState(feet).isAir();

        // 1. Web them first
        if (!alreadyWebbed && canPlace && webSlot != -1) {
            p.getInventory().setSelectedSlot(webSlot);
            Vec3d hit = Vec3d.ofCenter(below).add(0, 0.5, 0);
            BlockHitResult bhr = new BlockHitResult(hit, Direction.UP, below, false);
            mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, bhr);
            p.swingHand(Hand.MAIN_HAND);
            cooldown = getDelay();
            return;
        }

        // 2. Break their shield with an axe if they are blocking
        boolean blocking = target instanceof LivingEntity le && le.isBlocking();
        if (blocking && axeSlot != -1) {
            p.getInventory().setSelectedSlot(axeSlot);
            mc.interactionManager.attackEntity(p, target);
            p.swingHand(Hand.MAIN_HAND);
            cooldown = getDelay();
            return;
        }

        // 3. Otherwise hit with the sword
        if (swordSlot != -1) {
            p.getInventory().setSelectedSlot(swordSlot);
            mc.interactionManager.attackEntity(p, target);
            p.swingHand(Hand.MAIN_HAND);
            cooldown = getDelay();
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
