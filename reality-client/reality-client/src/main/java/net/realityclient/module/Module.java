package net.realityclient.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.realityclient.RealityClient;

public abstract class Module {
    private final String name;
    private final String description;
    private final int defaultKey;
    private boolean enabled;
    private KeyBinding keyBinding;

    protected Module(String name, String description, int defaultKey) {
        this.name = name;
        this.description = description;
        this.defaultKey = defaultKey;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getDefaultKey() { return defaultKey; }
    public boolean isEnabled() { return enabled; }
    public void setKeyBinding(KeyBinding kb) { this.keyBinding = kb; }

    public void pollKey(MinecraftClient mc) {
        while (keyBinding != null && keyBinding.wasPressed()) toggle(mc);
    }

    public void toggle(MinecraftClient mc) {
        enabled = !enabled;
        if (enabled) onEnable(mc); else onDisable(mc);
        RealityClient.chat(mc, name + (enabled ? " §aON" : " §cOFF"));
    }

    protected void onEnable(MinecraftClient mc) {}
    protected void onDisable(MinecraftClient mc) {}
    public abstract void onTick(MinecraftClient mc);
}
