package net.realityclient.module;

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
 * ShieldBreaker combines the axe shield-stun sequence with the normal
 * shield-breaker module. Hold the bind to run; the switch delay is configurable.
 */
public class ShieldBreaker extends Module {
    private enum State { IDLE, BREAKING }

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

    public ShieldBreaker() {
        super("ShieldBreaker", "Breaks a blocking player's shield with an axe, then returns to your sword.",
                GLFW.GLFW_KEY_Z, true);
        enableDelay(2, 0, 20);
    }

    @Override
    protected void onEnable(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.currentScreen != null || state != State.IDLE) return;

        Entity t = targetedEntity(mc);
        if (t == null) {
            RealityClient.chat(mc, "ShieldBreaker: aim at a player first");
            return;
        }

        axeSlot = findSlot(p, AXE_PREF, stack -> stack.isIn(ItemTags.AXES));
        if (axeSlot == -1) {
            RealityClient.chat(mc, "ShieldBreaker: no axe in your hotbar");
            return;
        }
        swordSlot = findSlot(p, SWORD_PREF, stack -> stack.isIn(ItemTags.SWORDS));
        previousSlot = p.getInventory().getSelectedSlot();
        target = t;
        hits = attackWait = switchWait = settle = total = 0;
        state = State.BREAKING;
    }

    @Override
    protected void onDisable(MinecraftClient mc) {
        if (state != State.IDLE && mc.player != null) finish(mc.player);
        else state = State.IDLE;
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (state == State.IDLE) return;
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.interactionManager == null) {
            state = State.IDLE;
            target = null;
            return;
        }

        total++;
        if (target == null || !target.isAlive() || p.distanceTo(target) > RANGE + 1.5) {
            RealityClient.chat(mc, "ShieldBreaker: lost target");
            finish(p);
            return;
        }
        if (total > 80) {
            RealityClient.chat(mc, "ShieldBreaker: timed out");
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
            boolean blocking = target instanceof LivingEntity living && living.isBlocking();
            if (!blocking || hits >= MAX_HITS) {
                RealityClient.chat(mc, "ShieldBreaker: done");
                finish(p);
                return;
            }
        }

        attackWait++;
        if (p.getAttackCooldownProgress(0.5f) < 0.9f && attackWait <= 25) return;

        mc.interactionManager.attackEntity(p, target);
        p.swingHand(Hand.MAIN_HAND);
        hits++;
        attackWait = 0;
        settle = 2;
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

    private Entity targetedEntity(MinecraftClient mc) {
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            Entity e = ((EntityHitResult) mc.crosshairTarget).getEntity();
            if (e instanceof LivingEntity living && living.isAlive()
                    && mc.player.distanceTo(e) <= RANGE) return e;
        }
        return null;
    }
}
