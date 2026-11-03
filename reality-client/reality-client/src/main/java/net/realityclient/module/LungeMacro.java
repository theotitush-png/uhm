package net.realityclient.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

/**
 * LungeMacro - chain lunges for rapid mace engagement.
 *
 * Cycle: LUNGE (spear jab, launches you at the target)
 *     -> SWAP  (switch to the mace hotbar slot)
 *     -> SMASH (mace hit while falling/airborne)
 *     -> RESET (back to the spear, wait for cooldown, repeat)
 */
public class LungeMacro extends Module {
    private enum State { LUNGE, SWAP, SMASH, RESET }

    private State state = State.LUNGE;
    private int spearSlot = -1;
    private int maceSlot = -1;
    private int wait = 0;

    // Tunables
    private static final double RANGE = 4.5;
    private static final int LUNGE_DELAY = 2;  // ticks between steps
    private static final int RESET_DELAY = 6;  // ticks before the next chain

    public LungeMacro() {
        super("LungeMacro", "Chain lunges for rapid mace engagement.", GLFW.GLFW_KEY_R);
    }

    @Override
    protected void onEnable(MinecraftClient mc) {
        state = State.LUNGE;
        wait = 0;
        findSlots(mc.player);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null || mc.currentScreen != null) return;

        if (wait > 0) { wait--; return; }
        findSlots(p);
        if (spearSlot == -1 || maceSlot == -1) return; // need both in the hotbar

        Entity target = targetedEntity(mc);
        if (target == null) { state = State.LUNGE; return; }

        switch (state) {
            case LUNGE -> {
                p.getInventory().setSelectedSlot(spearSlot);
                mc.interactionManager.attackEntity(p, target);
                p.swingHand(Hand.MAIN_HAND);
                state = State.SWAP;
                wait = LUNGE_DELAY;
            }
            case SWAP -> {
                p.getInventory().setSelectedSlot(maceSlot);
                state = State.SMASH;
                wait = 1;
            }
            case SMASH -> {
                // Mace damage scales with fall distance, so hit while airborne
                if (!p.isOnGround() || p.fallDistance > 0) {
                    mc.interactionManager.attackEntity(p, target);
                    p.swingHand(Hand.MAIN_HAND);
                    state = State.RESET;
                    wait = RESET_DELAY;
                }
            }
            case RESET -> {
                p.getInventory().setSelectedSlot(spearSlot);
                state = State.LUNGE;
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

    private void findSlots(ClientPlayerEntity p) {
        spearSlot = -1;
        maceSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = p.getInventory().getStack(i);
            if (s.isOf(Items.MACE)) maceSlot = i;
            else if (s.isIn(ItemTags.SPEARS)) spearSlot = i;
        }
    }
}
