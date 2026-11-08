package net.realityclient.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Items;
import org.lwjgl.glfw.GLFW;

/**
 * FastXP - while on, holding right click with an XP bottle throws bottles every few ticks
 * (Delay = ticks between throws, 1 = every tick).
 */
public class FastXp extends Module {
    private int cooldown = 0;

    public FastXp() {
        super("FastXP", "Hold right click with XP bottles to throw them faster. Delay = ticks between throws.", GLFW.GLFW_KEY_UNKNOWN, false);
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

        if (!p.getMainHandStack().isOf(Items.EXPERIENCE_BOTTLE)) return;

        if (cooldown > 0) { cooldown--; return; }

        // simulate a fresh right click press
        InputUtil.Key key = InputUtil.fromTranslationKey(mc.options.useKey.getBoundKeyTranslationKey());
        KeyBinding.onKeyPressed(key);
        cooldown = getDelay() - 1;
    }
}
