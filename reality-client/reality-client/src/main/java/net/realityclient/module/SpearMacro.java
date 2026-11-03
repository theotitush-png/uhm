package net.realityclient.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import org.lwjgl.glfw.GLFW;

/**
 * SpearMacro - hold the key to chain spear lunges.
 * Speed = ticks between lunges (change it in the GUI, lower is faster).
 */
public class SpearMacro extends Module {
    private int cooldown = 0;
    private int previousSlot = -1;

    public SpearMacro() {
        super("SpearMacro", "Hold to chain spear lunges. Delay = ticks between lunges (lower = faster).", GLFW.GLFW_KEY_V, true);
        enableDelay(4, 1, 20);
    }

    @Override
    protected void onEnable(MinecraftClient mc) {
        cooldown = 0;
        if (mc.player != null) previousSlot = mc.player.getInventory().getSelectedSlot();
    }

    @Override
    protected void onDisable(MinecraftClient mc) {
        if (mc.player != null && previousSlot >= 0) {
            mc.player.getInventory().setSelectedSlot(previousSlot);
        }
        previousSlot = -1;
    }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null || mc.currentScreen != null) return;

        int spearSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = p.getInventory().getStack(i);
            if (s.isIn(ItemTags.SPEARS)) { spearSlot = i; break; }
        }
        if (spearSlot == -1) return;

        p.getInventory().setSelectedSlot(spearSlot);

        if (cooldown > 0) { cooldown--; return; }

        // Simulates a left click, which is the spear jab / lunge
        KeyBinding.onKeyPressed(InputUtil.Type.MOUSE.createFromCode(GLFW.GLFW_MOUSE_BUTTON_LEFT));
        cooldown = getDelay();
    }
}
