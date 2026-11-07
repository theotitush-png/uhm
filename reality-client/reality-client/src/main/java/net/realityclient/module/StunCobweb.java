package net.realityclient.module;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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
 *   Press the key while aiming at a player:
 *     1. select the axe (slot 2), wait until it is charged, hit them
 *        (repeat up to 3 times until their shield is down = stunned)
 *     2. select cobwebs (slot 8) and place a web under them
 *     3. swap back to the sword (slot 1)
 */
public class StunCobweb extends Module {
    private enum State { IDLE, STUN, WEB }

    private static final double RANGE = 4.5;
    private static final int SWORD_SLOT = 0;  // hotbar slot 1
    private static final int AXE_SLOT = 1;    // hotbar slot 2
    private static final int WEB_SLOT = 7;    // hotbar slot 8
    private static final int MAX_HITS = 3;

    private State state = State.IDLE;
    private Entity target = null;
    private int hits = 0;
    private int wait = 0;
    private int settle = 0;
    private int total = 0;

    public StunCobweb() {
        super("Stuncobweb", "Axe (slot 2) stun until shield is down, cobweb (slot 8) under them, back to sword (slot 1).", GLFW.GLFW_KEY_C, true);
        ClientTickEvents.END_CLIENT_TICK.register(this::sequenceTick);
    }

    @Override
    protected void onEnable(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.currentScreen != null || state != State.IDLE) return;
        Entity t = targetedEntity(mc);
        if (t == null) return;
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

    private void finish(ClientPlayerEntity p) {
        p.getInventory().setSelectedSlot(SWORD_SLOT);
        state = State.IDLE;
        target = null;
    }

    private void sequenceTick(MinecraftClient mc) {
        if (state == State.IDLE) return;
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.interactionManager == null) { state = State.IDLE; return; }

        total++;
        if (total > 80 || target == null || !target.isAlive() || p.distanceTo(target) > RANGE + 1.5) {
            finish(p);
            return;
        }

        switch (state) {
            case STUN -> {
                p.getInventory().setSelectedSlot(AXE_SLOT);

                if (settle > 0) { settle--; return; }

                // after at least one hit: if their shield is down (or max hits reached), go to the web
                if (hits > 0) {
                    boolean stillBlocking = target instanceof LivingEntity le && le.isBlocking();
                    if (!stillBlocking || hits >= MAX_HITS) {
                        state = State.WEB;
                        return;
                    }
                }

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
