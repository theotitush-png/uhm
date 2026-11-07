package net.realityclient.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

/**
 * AutoDrain - press the key while looking at a water/lava source block:
 * swaps to an empty bucket in your hotbar, picks the liquid up, swaps back.
 */
public class AutoDrain extends Module {
    private static final double REACH = 5.0;

    public AutoDrain() {
        super("AutoDrain", "Press while looking at water/lava: picks it up with an empty bucket in your hotbar.", GLFW.GLFW_KEY_X, true);
    }

    @Override
    protected void onEnable(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;

        // look for a liquid source block under the crosshair
        HitResult hr = p.raycast(REACH, 1.0f, true);
        if (hr.getType() != HitResult.Type.BLOCK) return;
        BlockHitResult bhr = (BlockHitResult) hr;
        FluidState fs = mc.world.getFluidState(bhr.getBlockPos());
        if (fs.isEmpty() || !fs.isStill()) return;

        // find an empty bucket in the hotbar
        int bucketSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = p.getInventory().getStack(i);
            if (s.isOf(Items.BUCKET)) { bucketSlot = i; break; }
        }
        if (bucketSlot == -1) return;

        int previous = p.getInventory().getSelectedSlot();
        p.getInventory().setSelectedSlot(bucketSlot);
        mc.interactionManager.interactItem(p, Hand.MAIN_HAND);
        p.swingHand(Hand.MAIN_HAND);
        p.getInventory().setSelectedSlot(previous);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        // fires once per key press (see onEnable)
    }
}
