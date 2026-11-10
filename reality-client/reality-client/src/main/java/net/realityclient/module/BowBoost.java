package net.realityclient.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.math.Vec3d;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public class BowBoost extends Module {
    private boolean rotations = true;
    private int targetPitch = 75;
    private int bowSlot = -1;
    private int previousSlot = -1;
    private float previousYaw;
    private float previousPitch;
    private int drawTicks;
    private int waitTicks;
    private boolean started;
    private boolean launching;
    private int launchTicks;

    /** Horizontal push (blocks/tick) and minimum upward speed given on the jump. */
    private static final double BOOST_SPEED = 1.0;
    private static final double BOOST_LIFT = 0.5;
    private static final int LAUNCH_TICKS = 8;

    public BowBoost() {
        super("Bow Boost", "Fires a fully drawn arrow upwards, then jumps you forward with a boost.", GLFW.GLFW_KEY_B, false);
        setCategory("MOVEMENT");
    }

    public boolean hasRotations() { return rotations; }
    public int getTargetPitch() { return targetPitch; }
    public void toggleRotations() { rotations = !rotations; }
    public void changeTargetPitch(int amount) {
        targetPitch = Math.max(0, Math.min(90, targetPitch + amount));
    }

    @Override
    protected void onEnable(MinecraftClient mc) {
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.interactionManager == null) return;

        bowSlot = findBow(player);
        if (bowSlot < 0) {
            net.realityclient.RealityClient.chat(mc, "Bow Boost: no bow in your hotbar");
            toggle(mc);
            return;
        }

        previousSlot = player.getInventory().getSelectedSlot();
        previousYaw = player.getYaw();
        previousPitch = player.getPitch();
        drawTicks = 0;
        waitTicks = 0;
        started = false;
        launching = false;
        launchTicks = 0;
        player.getInventory().setSelectedSlot(bowSlot);
        // negative pitch = looking up; the arrow flies up in the direction you face
        if (rotations) player.setPitch(-targetPitch);
    }

    @Override
    protected void onDisable(MinecraftClient mc) {
        ClientPlayerEntity player = mc.player;
        if (player != null) {
            mc.options.useKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
            mc.options.forwardKey.setPressed(false);
            mc.options.sprintKey.setPressed(false);
            KeyBinding.updateKeysByCode(); // hand the keys back to the real keyboard state
            if (player.isUsingItem() && mc.interactionManager != null) {
                mc.interactionManager.stopUsingItem(player);
            }
            if (previousSlot >= 0) player.getInventory().setSelectedSlot(previousSlot);
            if (rotations && (bowSlot >= 0 || started)) {
                player.setYaw(previousYaw);
                player.setPitch(previousPitch);
            }
        }
        bowSlot = previousSlot = -1;
        drawTicks = 0;
        waitTicks = 0;
        started = false;
        launching = false;
        launchTicks = 0;
    }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.interactionManager == null || bowSlot < 0) {
            toggle(mc);
            return;
        }

        if (launching) {
            launchTick(mc, player);
            return;
        }

        if (!started) {
            ItemStack held = player.getMainHandStack();
            if (!held.isOf(Items.BOW)) {
                player.getInventory().setSelectedSlot(bowSlot);
                return;
            }
            // Hold the use key so vanilla doesn't cancel the draw on the next tick
            mc.options.useKey.setPressed(true);
            mc.interactionManager.interactItem(player, Hand.MAIN_HAND);
            started = true;
            return;
        }

        if (player.isUsingItem() && player.getActiveItem().isOf(Items.BOW)) {
            mc.options.useKey.setPressed(true);
            drawTicks++;
            if (drawTicks >= 20) {
                mc.options.useKey.setPressed(false);
                // sends the release packet so the arrow actually fires
                mc.interactionManager.stopUsingItem(player);
                launching = true;
                launchTicks = 0;
            }
        } else if (++waitTicks > 5) {
            toggle(mc);
        }
    }

    /** After the arrow is away: jump and dash forward in the direction you are facing. */
    private void launchTick(MinecraftClient mc, ClientPlayerEntity player) {
        launchTicks++;
        mc.options.forwardKey.setPressed(true);
        mc.options.sprintKey.setPressed(true);
        mc.options.jumpKey.setPressed(launchTicks <= 2);

        if (launchTicks == 1) {
            double yaw = Math.toRadians(player.getYaw());
            Vec3d v = player.getVelocity();
            player.setVelocity(
                    v.x - Math.sin(yaw) * BOOST_SPEED,
                    Math.max(v.y, BOOST_LIFT),
                    v.z + Math.cos(yaw) * BOOST_SPEED);
        } else if (launchTicks >= LAUNCH_TICKS) {
            toggle(mc);
        }
    }

    private int findBow(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).isOf(Items.BOW)) return i;
        }
        return -1;
    }
}
