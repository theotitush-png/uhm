package net.realityclient.module;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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
import net.realityclient.RealityClient;
import org.lwjgl.glfw.GLFW;

import java.util.function.Predicate;

/**
 * Stuncobweb - press the key while aiming at a player:
 *   1. swap to your axe (slot 2, or the first axe in your hotbar)
 *      and hit until their shield is down (max 3 hits, waits for the axe to charge)
 *   2. swap to cobwebs (slot 8, or the first cobweb) and place a web under them
 *   3. swap back to your sword (slot 1, or the first sword)
 * Messages above the hotbar tell you what happened.
 */
public class StunCobweb extends Module {
    private enum State { IDLE, STUN, WEB }

    private static final double RANGE = 4.5;
    private static final int SWORD_PREF = 0; // hotbar slot 1
    private static final int AXE_PREF = 1;   // hotbar slot 2
    private static final int WEB_PREF = 7;   // hotbar slot 8
    private static final int MAX_HITS = 3;

    private State state = State.IDLE;
    private Entity target = null;
    private int axeSlot = -1;
    private int webSlot = -1;
    private int swordSlot = -1;
    private int previousSlot = -1;
    private int hits = 0;
    private int wait = 0;
    private int settle = 0;
    private int total = 0;

    public StunCobweb() {
        super("Stuncobweb", "Axe stun until shield is down, then cobweb under them, then back to sword.", GLFW.GLFW_KEY_C, true);
        ClientTickEvents.END_CLIENT_TICK.register(this::sequenceTick);
    }

    @Override
    protected void onEnable(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.currentScreen != null || state != State.IDLE) return;

        Entity t = targetedEntity(mc);
        if (t == null) {
            RealityClient.chat(mc, "Stuncobweb: aim at a player first");
            return;
        }

        axeSlot = findSlot(p, AXE_PREF, s -> s.isIn(ItemTags.AXES));
        if (axeSlot == -1) {
            RealityClient.chat(mc, "Stuncobweb: no axe in your hotbar");
            return;
        }
        webSlot = findSlot(p, WEB_PREF, s -> s.isOf(Items.COBWEB));
        swordSlot = findSlot(p, SWORD_PREF, s -> s.isIn(ItemTags.SWORDS));
        previousSlot = p.getInventory().getSelectedSlot();

        target = t;
        hits = 0;
        wait = 0;
        settle = 0;
        total = 0;
        state = State.STUN;
    }

    @Override
    public void onTick(MinecraftClient mc) {
        // the sequence runs from its own tick handler (sequenceTick)
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
        if (p == null || mc.world == null || mc.interactionManager == null) { state = State.IDLE; return; }

        total++;
        if (target == null || !target.isAlive() || p.distanceTo(target) > RANGE + 1.5) {
            RealityClient.chat(mc, "Stuncobweb: lost the target");
            finish(p);
            return;
        }
        if (total > 80) {
            RealityClient.chat(mc, "Stuncobweb: timed out");
            finish(p);
            return;
        }

        switch (state) {
            case STUN -> {
                p.getInventory().setSelectedSlot(axeSlot);

                if (settle > 0) { settle--; return; }

                // after at least one hit: go to the web once their shield is down (or max hits reached)
                if (hits > 0) {
                    boolean stillBlocking = target instanceof LivingEntity le && le.isBlocking();
                    if (!stillBlocking || hits >= MAX_HITS) {
                        state = State.WEB;
                        return;
                    }
                }

                // wait until the axe is charged (servers without attack cooldown are always charged)
                wait++;
                boolean ready = p.getAttackCooldownProgress(0.5f) >= 0.9f || wait > 25;
                if (!ready) return;

                mc.interactionManager.attackEntity(p, target);
                p.swingHand(Hand.MAIN_HAND);
                hits++;
                wait = 0;
                settle = 2; // give the server a moment to update their shield state
            }
            case WEB -> {
                boolean placed = false;
                String reason = "";

                if (webSlot == -1) {
                    reason = "no cobwebs in your hotbar";
                } else {
                    BlockPos feet = target.getBlockPos();
                    BlockPos below = feet.down();
                    if (mc.world.getBlockState(below).isAir()) {
                        reason = "they are in the air";
                    } else if (mc.world.getBlockState(feet).isOf(Blocks.COBWEB)) {
                        reason = "already webbed";
                    } else if (!mc.world.getBlockState(feet).isAir()) {
                        reason = "no space at their feet";
                    } else {
                        p.getInventory().setSelectedSlot(webSlot);
                        Vec3d hit = Vec3d.ofCenter(below).add(0, 0.5, 0);
                        BlockHitResult bhr = new BlockHitResult(hit, Direction.UP, below, false);
                        mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, bhr);
                        p.swingHand(Hand.MAIN_HAND);
                        placed = true;
                    }
                }

                RealityClient.chat(mc, placed
                        ? "Stuncobweb: stunned + webbed"
                        : "Stuncobweb: stun done, no web (" + reason + ")");
                finish(p);
            }
            default -> { }
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
