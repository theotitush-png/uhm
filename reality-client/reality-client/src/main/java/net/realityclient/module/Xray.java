package net.realityclient.module;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import org.lwjgl.glfw.GLFW;

/**
 * Xray - while on, only ores, ancient debris, chests and spawners are drawn.
 * The actual hiding happens in BlockMixin.
 */
public class Xray extends Module {
    public static boolean active = false;

    public Xray() {
        super("Xray", "Hides normal blocks so ores, chests and spawners show through.", GLFW.GLFW_KEY_UNKNOWN, false);
        setCategory("MISC");
    }

    @Override
    protected void onEnable(MinecraftClient mc) {
        active = true;
        reload(mc);
    }

    @Override
    protected void onDisable(MinecraftClient mc) {
        active = false;
        reload(mc);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        // nothing to do per tick
    }

    private void reload(MinecraftClient mc) {
        if (mc.worldRenderer != null) mc.worldRenderer.reload();
    }

    public static boolean isVisible(BlockState state) {
        String path = Registries.BLOCK.getId(state.getBlock()).getPath();
        return path.endsWith("_ore")
                || path.equals("ancient_debris")
                || path.contains("chest")
                || path.equals("spawner")
                || path.equals("trial_spawner");
    }
}
