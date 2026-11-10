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

public class ShieldBreaker extends Module {
    public enum AttackMode { AUTO, MANUAL }
    public enum BreakMode { NORMAL, STUN }

    private static final double RANGE = 4.5;
    private static final int SWORD_PREF = 0;
    private static final int AXE_PREF = 1;
    private static final int MAX_STUN_HITS = 3;

    private AttackMode attackMode = AttackMode.AUTO;
    private BreakMode breakMode = BreakMode.STUN;
    private boolean manualWasDown;
    private Entity target;
    private int axeSlot = -1, swordSlot = -1, previousSlot = -1;
    private int hits, attackWait, switchWait, settle, total;
    private int switchBackDelay = 2;
    private int switchBackWait;
    private boolean pendingSwitchBack;
    private boolean swordHitDone;

    public ShieldBreaker() {
        super("ShieldBreaker", "Break a blocking player's shield automatically or when clicked.", 0, false);
        enableDelay(2, 0, 20);
    }

    public AttackMode getAttackMode() { return attackMode; }
    public BreakMode getBreakMode() { return breakMode; }
    public int getSwitchBackDelay() { return switchBackDelay; }
    public void changeSwitchBackDelay(int amount) {
        switchBackDelay = Math.max(0, Math.min(20, switchBackDelay + amount));
    }
    public void cycleAttackMode() {
        attackMode = attackMode == AttackMode.AUTO ? AttackMode.MANUAL : AttackMode.AUTO;
        resetSequence(MinecraftClient.getInstance());
    }
    public void cycleBreakMode() {
        breakMode = breakMode == BreakMode.STUN ? BreakMode.NORMAL : BreakMode.STUN;
        resetSequence(MinecraftClient.getInstance());
    }

    @Override
    protected void onEnable(MinecraftClient mc) {
        manualWasDown = false;
        clearState();
    }

    @Override
    protected void onDisable(MinecraftClient mc) {
        resetSequence(mc);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.interactionManager == null) {
            clearState();
            return;
        }

        if (pendingSwitchBack) {
            if (switchBackWait > 0) switchBackWait--;
            if (switchBackWait == 0) {
                restoreSlot(p);
                pendingSwitchBack = false;
            }
        }

        if (attackMode == AttackMode.AUTO && target == null && !pendingSwitchBack) {
            startSequence(mc, targetedEntity(mc));
        }

        if (attackMode == AttackMode.MANUAL && target == null && !pendingSwitchBack) {
            boolean down = mc.options.attackKey.isPressed();
            if (down && !manualWasDown) {
                Entity aimed = targetedEntity(mc);
                if (aimed instanceof LivingEntity living && living.isBlocking()) {
                    startSequence(mc, aimed);
                }
            }
            manualWasDown = down;
        }

        if (target == null) return;
        total++;
        if (!target.isAlive() || p.distanceTo(target) > RANGE + 1.5 || total > 80) {
            finish(p);
            return;
        }

        // STUN mode opens with one sword hit, then immediately switches to the axe.
        int activeSlot = breakMode == BreakMode.STUN && !swordHitDone ? swordSlot : axeSlot;
        if (activeSlot < 0) {
            finish(p);
            return;
        }
        if (p.getInventory().getSelectedSlot() != activeSlot) {
            p.getInventory().setSelectedSlot(activeSlot);
            switchWait = activeSlot == axeSlot ? getDelay() : 0;
            if (switchWait > 0) return;
        }
        if (switchWait > 0) { switchWait--; return; }
        if (settle > 0) { settle--; return; }

        if (swordHitDone || breakMode == BreakMode.NORMAL) {
            if (hits > 0) {
                boolean blocking = target instanceof LivingEntity living && living.isBlocking();
                if (!blocking || hits >= MAX_STUN_HITS) {
                    finish(p);
                    return;
                }
            }
        }

        attackWait++;
        if (p.getAttackCooldownProgress(0.5f) < 0.9f && attackWait <= 25) return;
        mc.interactionManager.attackEntity(p, target);
        p.swingHand(Hand.MAIN_HAND);
        if (breakMode == BreakMode.STUN && !swordHitDone) {
            swordHitDone = true;
            attackWait = 0;
            settle = 0;
            switchWait = 0;
            return;
        }
        hits++;
        attackWait = 0;
        settle = 2;
    }

    private void startSequence(MinecraftClient mc, Entity entity) {
        ClientPlayerEntity p = mc.player;
        if (p == null || entity == null || !(entity instanceof LivingEntity living) || !living.isBlocking()) return;
        int axe = findSlot(p, AXE_PREF, stack -> stack.isIn(ItemTags.AXES));
        if (axe == -1) {
            RealityClient.chat(mc, "ShieldBreaker: no axe in your hotbar");
            return;
        }
        axeSlot = axe;
        swordSlot = findSlot(p, SWORD_PREF, stack -> stack.isIn(ItemTags.SWORDS));
        if (breakMode == BreakMode.STUN && swordSlot == -1) {
            axeSlot = -1;
            RealityClient.chat(mc, "ShieldBreaker: no sword in your hotbar");
            return;
        }
        previousSlot = p.getInventory().getSelectedSlot();
        target = entity;
        hits = attackWait = switchWait = settle = total = 0;
        swordHitDone = breakMode != BreakMode.STUN;
    }

    private int findSlot(ClientPlayerEntity p, int preferred, Predicate<ItemStack> test) {
        if (test.test(p.getInventory().getStack(preferred))) return preferred;
        for (int i = 0; i < 9; i++) if (test.test(p.getInventory().getStack(i))) return i;
        return -1;
    }

    private void finish(ClientPlayerEntity p) {
        target = null;
        hits = attackWait = switchWait = settle = total = 0;
        swordHitDone = false;
        pendingSwitchBack = true;
        switchBackWait = switchBackDelay;
        if (switchBackWait == 0) {
            restoreSlot(p);
            pendingSwitchBack = false;
        }
    }

    private void restoreSlot(ClientPlayerEntity p) {
        // Return to the slot the player was holding before the stun sequence.
        if (previousSlot >= 0) p.getInventory().setSelectedSlot(previousSlot);
        axeSlot = swordSlot = previousSlot = -1;
    }

    private void resetSequence(MinecraftClient mc) {
        if ((target != null || pendingSwitchBack) && mc.player != null) {
            if (target != null) {
                target = null;
                restoreSlot(mc.player);
            } else if (pendingSwitchBack) {
                restoreSlot(mc.player);
            }
        }
        clearState();
    }

    private void clearState() {
        target = null;
        pendingSwitchBack = false;
        switchBackWait = 0;
        swordHitDone = false;
        hits = attackWait = switchWait = settle = total = 0;
    }

    private Entity targetedEntity(MinecraftClient mc) {
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            Entity e = ((EntityHitResult) mc.crosshairTarget).getEntity();
            if (e instanceof LivingEntity living && living.isAlive()
                    && living.isBlocking() && mc.player.distanceTo(e) <= RANGE) return e;
        }
        return null;
    }
}