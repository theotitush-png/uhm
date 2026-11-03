package net.realityclient.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.realityclient.RealityClient;
import net.realityclient.module.Module;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ClickGuiScreen extends Screen {
    private static final int W = 270;
    private static final int ROW = 24;
    private static final int HEADER = 32;
    private static final int BTN = 12;

    private static final int PANEL_BG = 0xF00C1A12;
    private static final int HEADER_BG = 0xF008120C;
    private static final int ACCENT = 0xFF22C55E;
    private static final int ROW_OFF = 0xFF12261A;
    private static final int ROW_HOVER = 0xFF1A3626;
    private static final int ROW_ON = 0xFF14532D;
    private static final int KEY_BG = 0xFF0A140E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFF8BA394;
    private static final int TEXT_ACCENT = 0xFFBBF7D0;

    private Module listening = null;

    public ClickGuiScreen() {
        super(Text.literal(RealityClient.NAME));
    }

    private int panelX() { return width / 2 - W / 2; }

    private int panelY() {
        int total = HEADER + RealityClient.MODULES.size() * ROW;
        return height / 2 - total / 2;
    }

    private String keyText(Module m) {
        if (listening == m) return "[...]";
        KeyBinding kb = m.getKeyBinding();
        if (kb == null) return "[?]";
        return "[" + kb.getBoundKeyLocalizedText().getString() + "]";
    }

    private int keyBoxWidth(Module m) {
        return textRenderer.getWidth(keyText(m)) + 8;
    }

    private String modeTag(Module m) {
        return m.isHoldMode() ? "HOLD" : "TOGGLE";
    }

    /** x of the mode tag text */
    private int tagX(Module m, int x) {
        int kx = x + W - keyBoxWidth(m) - 6;
        return kx - textRenderer.getWidth(modeTag(m)) - 8;
    }

    /** x of the "+" button (delay widget) */
    private int plusX(Module m, int x) {
        return tagX(m, x) - 8 - BTN;
    }

    private int valueX(Module m, int x) {
        int dw = textRenderer.getWidth(String.valueOf(m.getDelay()));
        return plusX(m, x) - 4 - dw;
    }

    /** x of the "-" button (delay widget) */
    private int minusX(Module m, int x) {
        return valueX(m, x) - 4 - BTN;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0x99040A06);

        List<Module> mods = RealityClient.MODULES;
        int x = panelX();
        int y = panelY();
        int total = HEADER + mods.size() * ROW;

        ctx.fill(x - 1, y - 1, x + W + 1, y + total + 1, ACCENT);
        ctx.fill(x, y, x + W, y + total, PANEL_BG);
        ctx.fill(x, y, x + W, y + HEADER, HEADER_BG);
        ctx.fill(x, y + HEADER - 2, x + W, y + HEADER, ACCENT);
        ctx.drawTextWithShadow(textRenderer, RealityClient.NAME, x + 10, y + 10, TEXT);

        String tip = null;
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int ry = y + HEADER + i * ROW;
            boolean hover = mouseX >= x && mouseX < x + W && mouseY >= ry && mouseY < ry + ROW;
            boolean on = m.isEnabled();

            ctx.fill(x, ry, x + W, ry + ROW, on ? ROW_ON : (hover ? ROW_HOVER : ROW_OFF));
            if (on) ctx.fill(x, ry, x + 3, ry + ROW, ACCENT);

            ctx.drawTextWithShadow(textRenderer, m.getName(), x + 12, ry + 8, on ? TEXT_ACCENT : TEXT);

            // key box (click to rebind)
            String kt = keyText(m);
            int kw = keyBoxWidth(m);
            int kx = x + W - kw - 6;
            ctx.fill(kx, ry + 4, kx + kw, ry + ROW - 4, KEY_BG);
            ctx.drawTextWithShadow(textRenderer, kt, kx + 4, ry + 8, listening == m ? ACCENT : TEXT_ACCENT);

            // mode tag
            String tag = modeTag(m);
            ctx.drawTextWithShadow(textRenderer, tag, tagX(m, x), ry + 8, TEXT_DIM);

            // delay widget
            if (m.hasDelay()) {
                int mx0 = minusX(m, x);
                int px0 = plusX(m, x);
                ctx.fill(mx0, ry + 6, mx0 + BTN, ry + ROW - 6, KEY_BG);
                ctx.drawTextWithShadow(textRenderer, "-", mx0 + 4, ry + 8, TEXT_ACCENT);
                ctx.drawTextWithShadow(textRenderer, String.valueOf(m.getDelay()), valueX(m, x), ry + 8, TEXT);
                ctx.fill(px0, ry + 6, px0 + BTN, ry + ROW - 6, KEY_BG);
                ctx.drawTextWithShadow(textRenderer, "+", px0 + 3, ry + 8, TEXT_ACCENT);
            }

            if (hover) {
                tip = m.getDescription() + "  (right click: hold/toggle)";
            }
        }

        if (listening != null) {
            tip = "Press a key to bind it, Escape to cancel";
        }
        if (tip != null) {
            ctx.drawTextWithShadow(textRenderer, tip, x, y + total + 8, TEXT_DIM);
        }
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        List<Module> mods = RealityClient.MODULES;
        int x = panelX();
        int y = panelY();
        double mx = click.x();
        double my = click.y();
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int ry = y + HEADER + i * ROW;
            if (mx >= x && mx < x + W && my >= ry && my < ry + ROW) {
                MinecraftClient mc = MinecraftClient.getInstance();
                int kw = keyBoxWidth(m);
                int kx = x + W - kw - 6;

                if (click.button() == 0 && mx >= kx && mx < kx + kw) {
                    listening = m;
                } else if (click.button() == 0 && m.hasDelay()
                        && mx >= minusX(m, x) && mx < minusX(m, x) + BTN) {
                    listening = null;
                    m.changeDelay(-1); // faster
                } else if (click.button() == 0 && m.hasDelay()
                        && mx >= plusX(m, x) && mx < plusX(m, x) + BTN) {
                    listening = null;
                    m.changeDelay(1);  // slower
                } else if (click.button() == 0) {
                    listening = null;
                    m.toggle(mc);
                } else if (click.button() == 1) {
                    listening = null;
                    m.switchMode(mc);
                }
                return true;
            }
        }
        listening = null;
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (listening != null) {
            int code = input.key();
            if (code != GLFW.GLFW_KEY_ESCAPE && listening.getKeyBinding() != null) {
                listening.getKeyBinding().setBoundKey(InputUtil.Type.KEYSYM.createFromCode(code));
                KeyBinding.updateKeysByCode();
                MinecraftClient.getInstance().options.write();
            }
            listening = null;
            return true;
        }
        return super.keyPressed(input);
    }
}
