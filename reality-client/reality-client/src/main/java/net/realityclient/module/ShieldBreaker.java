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

    public ShieldBreaker() {
        super("ShieldBreaker", "Break a blocking player's shield automatically or when clicked.", 0, false);
        enableDelay(2, 0, 20);
    }

    public AttackMode getAttackMode() { return attackMode; }
    public BreakMode getBreakMode() { return breakMode; }
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

        if (attackMode == AttackMode.AUTO && target == null) {
            startSequence(mc, targetedEntity(mc));
        }

        if (attackMode == AttackMode.MANUAL && target == null) {
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

        if (p.getInventory().getSelectedSlot() != axeSlot) {
            p.getInventory().setSelectedSlot(axeSlot);
            switchWait = getDelay();
            if (switchWait > 0) return;
        }
        if (switchWait > 0) { switchWait--; return; }
        if (settle > 0) { settle--; return; }

        if (hits > 0) {
            boolean blocking = target instanceof LivingEntity living && living.isBlocking();
            if (!blocking || breakMode == BreakMode.NORMAL || hits >= MAX_STUN_HITS) {
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
        previousSlot = p.getInventory().getSelectedSlot();
        target = entity;
        hits = attackWait = switchWait = settle = total = 0;
    }

    private int findSlot(ClientPlayerEntity p, int preferred, Predicate<ItemStack> test) {
        if (test.test(p.getInventory().getStack(preferred))) return preferred;
        for (int i = 0; i < 9; i++) if (test.test(p.getInventory().getStack(i))) return i;
        return -1;
    }

    private void finish(ClientPlayerEntity p) {
        int back = swordSlot != -1 ? swordSlot : previousSlot;
        if (back >= 0) p.getInventory().setSelectedSlot(back);
        clearState();
    }

    private void resetSequence(MinecraftClient mc) {
        if (target != null && mc.player != null) finish(mc.player);
        else clearState();
    }

    private void clearState() {
        target = null;
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
