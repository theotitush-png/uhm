package net.realityclient.module;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.realityclient.RealityClient;
import org.lwjgl.glfw.GLFW;

import java.util.function.Predicate;

/**
 * Stun - attack a targeted player with an axe until their shield is down,
 * then switch back to the sword. The switch delay is configurable in the GUI.
 */
public class Stun extends Module {
    private enum State { IDLE, STUN }

    private static final double RANGE = 4.5;
    private static final int SWORD_PREF = 0;
    private static final int AXE_PREF = 1;
    private static final int MAX_HITS = 3;

    private State state = State.IDLE;
    private Entity target;
    private int axeSlot = -1;
    private int swordSlot = -1;
    private int previousSlot = -1;
    private int hits;
    private int attackWait;
    private int switchWait;
    private int settle;
    private int total;

    public Stun() {
        super("Stun", "Axe stun until the target's shield is down, then switch back to your sword.",
                GLFW.GLFW_KEY_C, true);
        enableDelay(2, 0, 20);
        ClientTickEvents.END_CLIENT_TICK.register(this::sequenceTick);
    }

    @Override
    protected void onEnable(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.currentScreen != null || state != State.IDLE) return;

        Entity t = targetedEntity(mc);
        if (t == null) {
            RealityClient.chat(mc, "Stun: aim at a player first");
            return;
        }

        axeSlot = findSlot(p, AXE_PREF, s -> s.isIn(ItemTags.AXES));
        if (axeSlot == -1) {
            RealityClient.chat(mc, "Stun: no axe in your hotbar");
            return;
        }
        swordSlot = findSlot(p, SWORD_PREF, s -> s.isIn(ItemTags.SWORDS));
        previousSlot = p.getInventory().getSelectedSlot();

        target = t;
        hits = 0;
        attackWait = 0;
        switchWait = 0;
        settle = 0;
        total = 0;
        state = State.STUN;
    }

    @Override
    protected void onDisable(MinecraftClient mc) {
        if (state != State.IDLE && mc.player != null) finish(mc.player);
        else state = State.IDLE;
    }

    @Override
    public void onTick(MinecraftClient mc) {
        // The sequence runs from its own tick handler (sequenceTick).
    }

    private int findSlot(ClientPlayerEntity p, int preferred, Predicate<ItemStack> test) {
        if (test.test(p.getInventory().getStack(preferred))) return preferred;
        for (int i = 0; i < 9; i++) {
            if (test.test(p.getInventory().getStack(i))) return i;
        }
        return -1;
    }

    private void finish(ClientPlayerEntity p) {
        int back = swordSlot != -1 ? swordSlot : previousSlot;
        if (back >= 0) p.getInventory().setSelectedSlot(back);
        state = State.IDLE;
        target = null;
    }

    private void sequenceTick(MinecraftClient mc) {
        if (state == State.IDLE) return;
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.interactionManager == null) {
            state = State.IDLE;
            target = null;
            return;
        }

        total++;
        if (target == null || !target.isAlive() || p.distanceTo(target) > RANGE + 1.5) {
            RealityClient.chat(mc, "Stun: lost the target");
            finish(p);
            return;
        }
        if (total > 80) {
            RealityClient.chat(mc, "Stun: timed out");
            finish(p);
            return;
        }

        if (p.getInventory().getSelectedSlot() != axeSlot) {
            p.getInventory().setSelectedSlot(axeSlot);
            switchWait = getDelay();
            if (switchWait > 0) return;
        }
        if (switchWait > 0) {
            switchWait--;
            return;
        }
        if (settle > 0) {
            settle--;
            return;
        }

        if (hits > 0) {
            boolean stillBlocking = target instanceof LivingEntity le && le.isBlocking();
            if (!stillBlocking || hits >= MAX_HITS) {
                RealityClient.chat(mc, "Stun: done");
                finish(p);
                return;
            }
        }

        attackWait++;
        boolean ready = p.getAttackCooldownProgress(0.5f) >= 0.9f || attackWait > 25;
        if (!ready) return;

        mc.interactionManager.attackEntity(p, target);
        p.swingHand(Hand.MAIN_HAND);
        hits++;
        attackWait = 0;
        settle = 2;
    }

    private Entity targetedEntity(MinecraftClient mc) {
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            Entity e = ((EntityHitResult) mc.crosshairTarget).getEntity();
            if (e instanceof LivingEntity le && le.isAlive() && mc.player.distanceTo(e) <= RANGE) return e;
        }
        return null;
    }
}
