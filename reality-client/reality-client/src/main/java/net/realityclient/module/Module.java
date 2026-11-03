package net.realityclient.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.realityclient.RealityClient;

public abstract class Module {
    private final String name;
    private final String description;
    private final int defaultKey;
    private boolean toggled;
    private boolean held;
    private boolean holdMode;
    private KeyBinding keyBinding;

    protected Module(String name, String description, int defaultKey, boolean holdByDefault) {
        this.name = name;
        this.description = description;
        this.defaultKey = defaultKey;
        this.holdMode = holdByDefault;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getDefaultKey() { return defaultKey; }
    public boolean isHoldMode() { return holdMode; }
    public boolean isEnabled() { return holdMode ? held : toggled; }
    public void setKeyBinding(KeyBinding kb) { this.keyBinding = kb; }
    public KeyBinding getKeyBinding() { return keyBinding; }

    public void pollKey(MinecraftClient mc) {
        if (keyBinding == null) return;
        if (holdMode) {
            boolean now = keyBinding.isPressed() && mc.currentScreen == null;
            while (keyBinding.wasPressed()) { /* drain */ }
            if (now != held) {
                held = now;
                if (held) onEnable(mc); else onDisable(mc);
            }
        } else {
            while (keyBinding.wasPressed()) toggle(mc);
        }
    }

    /** Called from the GUI. Does nothing in hold mode. */
    public void toggle(MinecraftClient mc) {
        if (holdMode) return;
        toggled = !toggled;
        if (toggled) onEnable(mc); else onDisable(mc);
        RealityClient.chat(mc, name + (toggled ? " §aON" : " §cOFF"));
    }

    /** Called from the GUI on right click. */
    public void switchMode(MinecraftClient mc) {
        if (isEnabled()) onDisable(mc);
        toggled = false;
        held = false;
        holdMode = !holdMode;
    }

    protected void onEnable(MinecraftClient mc) {}
    protected void onDisable(MinecraftClient mc) {}
    public abstract void onTick(MinecraftClient mc);
}
