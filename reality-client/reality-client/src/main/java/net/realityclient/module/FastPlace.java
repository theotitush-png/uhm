package net.realityclient.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * FastPlace - while on, holding right click with a block places every few ticks
 * (Delay = ticks between placements, 1 = every tick).
 */
public class FastPlace extends Module {
    private int cooldown = 0;

    public FastPlace() {
        super("FastPlace", "Hold right click with blocks to place them faster. Delay = ticks between placements.", GLFW.GLFW_KEY_UNKNOWN, false);
        setCategory("MISC");
        enableDelay(1, 1, 10);
    }

    @Override
    protected void onEnable(MinecraftClient mc) {
        cooldown = 0;
    }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.currentScreen != null) return;

        if (!mc.options.useKey.isPressed()) { cooldown = 0; return; }

        ItemStack held = p.getMainHandStack();
        if (!(held.getItem() instanceof BlockItem)) return;

        if (cooldown > 0) { cooldown--; return; }

        // simulate a fresh right click press
        InputUtil.Key key = InputUtil.fromTranslationKey(mc.options.useKey.getBoundKeyTranslationKey());
        KeyBinding.onKeyPressed(key);
        cooldown = getDelay() - 1;
    }
}
