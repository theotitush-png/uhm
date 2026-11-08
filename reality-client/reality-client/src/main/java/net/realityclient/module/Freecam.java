package net.realityclient.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import org.lwjgl.glfw.GLFW;

/**
 * Freecam - detaches the camera from your body. Your real player stands still
 * while you fly the camera around (W/A/S/D, Space = up, Shift = down, Sprint = faster).
 */
public class Freecam extends Module {
    private static final double SPEED = 0.6;

    private OtherClientPlayerEntity cam;
    private Input savedInput;
    private final Input frozenInput = new Input();

    public Freecam() {
        super("Freecam", "Fly the camera around while your body stays put.", GLFW.GLFW_KEY_UNKNOWN, false);
        setCategory("MISC");
    }

    @Override
    protected void onEnable(MinecraftClient mc) {
        setup(mc);
    }

    @Override
    protected void onDisable(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p != null && savedInput != null) p.input = savedInput;
        if (p != null) mc.setCameraEntity(p);
        cam = null;
        savedInput = null;
    }

    private void setup(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) return;

        // stop the real player from moving
        if (p.input != frozenInput) savedInput = p.input;
        p.input = frozenInput;

        cam = new OtherClientPlayerEntity(mc.world, p.getGameProfile());
        cam.setPosition(p.getX(), p.getY(), p.getZ());
        cam.setYaw(p.getYaw());
        cam.setPitch(p.getPitch());
        mc.setCameraEntity(cam);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) return;

        // (re)attach if the player was respawned or the world changed
        if (cam == null || mc.getCameraEntity() != cam || p.input != frozenInput) {
            setup(mc);
            if (cam == null) return;
        }

        GameOptions o = mc.options;
        double forward = (o.forwardKey.isPressed() ? 1 : 0) - (o.backKey.isPressed() ? 1 : 0);
        double side = (o.rightKey.isPressed() ? 1 : 0) - (o.leftKey.isPressed() ? 1 : 0);
        double up = (o.jumpKey.isPressed() ? 1 : 0) - (o.sneakKey.isPressed() ? 1 : 0);
        double speed = o.sprintKey.isPressed() ? SPEED * 2 : SPEED;

        double yaw = Math.toRadians(p.getYaw());
        double dx = forward * -Math.sin(yaw) + side * -Math.cos(yaw);
        double dz = forward * Math.cos(yaw) + side * -Math.sin(yaw);

        cam.setPosition(cam.getX() + dx * speed, cam.getY() + up * speed, cam.getZ() + dz * speed);
        cam.setYaw(p.getYaw());
        cam.setPitch(p.getPitch());

        // keep the hotbar and health bar showing your real ones
        for (int i = 0; i < 9; i++) cam.getInventory().setStack(i, p.getInventory().getStack(i));
        cam.getInventory().setSelectedSlot(p.getInventory().getSelectedSlot());
        cam.setHealth(p.getHealth());
    }
}
