package net.realityclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.realityclient.module.LungeMacro;
import net.realityclient.module.Module;

import java.util.ArrayList;
import java.util.List;

public class RealityClient implements ClientModInitializer {
    public static final String NAME = "Reality Client";
    public static final List<Module> MODULES = new ArrayList<>();
    public static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("realityclient", "main"));

    @Override
    public void onInitializeClient() {
        register(new LungeMacro());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            for (Module m : MODULES) {
                m.pollKey(client);
                if (m.isEnabled() && client.player != null && client.world != null) m.onTick(client);
            }
        });
    }

    private static void register(Module m) {
        KeyBinding kb = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.realityclient." + m.getName().toLowerCase(),
                        InputUtil.Type.KEYSYM, m.getDefaultKey(), CATEGORY));
        m.setKeyBinding(kb);
        MODULES.add(m);
    }

    public static void chat(MinecraftClient mc, String msg) {
        if (mc.player != null) mc.player.sendMessage(Text.literal("§b[" + NAME + "] §r" + msg), true);
    }
}
