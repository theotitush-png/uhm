package net.realityclient.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
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
    private boolean started;

    public BowBoost() {
        super("Bow Boost", "Charges and fires your bow at the configured pitch when activated.", GLFW.GLFW_KEY_B, false);
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
        started = false;
        player.getInventory().setSelectedSlot(bowSlot);
        if (rotations) player.setPitch(targetPitch);
    }

    @Override
    protected void onDisable(MinecraftClient mc) {
        ClientPlayerEntity player = mc.player;
        if (player != null) {
            if (player.isUsingItem()) player.stopUsingItem();
            if (previousSlot >= 0) player.getInventory().setSelectedSlot(previousSlot);
            if (rotations && (bowSlot >= 0 || started)) {
                player.setYaw(previousYaw);
                player.setPitch(previousPitch);
            }
        }
        bowSlot = previousSlot = -1;
        drawTicks = 0;
        started = false;
    }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.interactionManager == null || bowSlot < 0) {
            toggle(mc);
            return;
        }

        if (!started) {
            ItemStack held = player.getMainHandStack();
            if (!held.isOf(Items.BOW)) {
                player.getInventory().setSelectedSlot(bowSlot);
                return;
            }
            mc.interactionManager.interactItem(player, Hand.MAIN_HAND);
            started = true;
            return;
        }

        if (player.isUsingItem() && player.getActiveItem().isOf(Items.BOW)) {
            drawTicks++;
            if (drawTicks >= 20) {
                player.stopUsingItem();
                toggle(mc);
            }
        } else if (drawTicks > 0) {
            toggle(mc);
        } else if (drawTicks > 5) {
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
