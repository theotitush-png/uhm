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

import java.util.ArrayList;
import java.util.List;

public class ClickGuiScreen extends Screen {
    private static final int W = 200;
    private static final int ROW = 22;
    private static final int HEADER = 28;
    private static final int GAP = 12;
    private static final int TOP = 44;
    private static final int BTN = 12;

    private static final int PANEL_BG = 0xF00A1510;
    private static final int HEADER_BG = 0xF0060D09;
    private static final int ACCENT = 0xFF22C55E;
    private static final int ROW_OFF = 0xFF0E1D15;
    private static final int ROW_HOVER = 0xFF16301F;
    private static final int ROW_ON = 0xFF1E6B3A;
    private static final int KEY_BG = 0xFF070F0A;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFF8BA394;
    private static final int TEXT_ACCENT = 0xFFD1FADF;

    private Module listening = null;

    public ClickGuiScreen() {
        super(Text.literal(RealityClient.NAME));
    }

    // ---- layout helpers ----

    private List<String> categories() {
        List<String> out = new ArrayList<>();
        for (Module m : RealityClient.MODULES) {
            if (!out.contains(m.getCategory())) out.add(m.getCategory());
        }
        return out;
    }

    private List<Module> inCategory(String cat) {
        List<Module> out = new ArrayList<>();
        for (Module m : RealityClient.MODULES) {
            if (m.getCategory().equals(cat)) out.add(m);
        }
        return out;
    }

    private int panelX(int index, int count) {
        int total = count * W + (count - 1) * GAP;
        return width / 2 - total / 2 + index * (W + GAP);
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

    private int keyBoxX(Module m, int px) { return px + W - keyBoxWidth(m) - 6; }

    private int tagX(Module m, int px) {
        return keyBoxX(m, px) - textRenderer.getWidth(modeTag(m)) - 8;
    }

    private int plusX(Module m, int px) { return tagX(m, px) - 8 - BTN; }

    private int valueX(Module m, int px) {
        int dw = textRenderer.getWidth(String.valueOf(m.getDelay()));
        return plusX(m, px) - 4 - dw;
    }

    private int minusX(Module m, int px) { return valueX(m, px) - 4 - BTN; }

    // ---- drawing ----

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0x99040A06);
        ctx.drawTextWithShadow(textRenderer, RealityClient.NAME, 14, 14, ACCENT);

        List<String> cats = categories();
        String tip = null;

        for (int c = 0; c < cats.size(); c++) {
            List<Module> mods = inCategory(cats.get(c));
            int px = panelX(c, cats.size());
            int py = TOP;
            int total = HEADER + mods.size() * ROW + 4;

            ctx.fill(px - 1, py - 1, px + W + 1, py + total + 1, ACCENT);
            ctx.fill(px, py, px + W, py + total, PANEL_BG);
            ctx.fill(px, py, px + W, py + HEADER, HEADER_BG);
            ctx.fill(px, py + HEADER - 1, px + W, py + HEADER, ACCENT);
            ctx.drawTextWithShadow(textRenderer, cats.get(c), px + 12, py + 10, TEXT);

            for (int i = 0; i < mods.size(); i++) {
                Module m = mods.get(i);
                int ry = py + HEADER + i * ROW;
                boolean hover = mouseX >= px && mouseX < px + W && mouseY >= ry && mouseY < ry + ROW;
                boolean on = m.isEnabled();

                ctx.fill(px, ry, px + W, ry + ROW, on ? ROW_ON : (hover ? ROW_HOVER : ROW_OFF));
                if (on) ctx.fill(px, ry, px + 3, ry + ROW, ACCENT);

                ctx.drawTextWithShadow(textRenderer, m.getName(), px + 12, ry + 7, on ? TEXT_ACCENT : TEXT);

                // key box
                String kt = keyText(m);
                int kw = keyBoxWidth(m);
                int kx = keyBoxX(m, px);
                ctx.fill(kx, ry + 4, kx + kw, ry + ROW - 4, KEY_BG);
                ctx.drawTextWithShadow(textRenderer, kt, kx + 4, ry + 7, listening == m ? ACCENT : TEXT_ACCENT);

                // mode tag
                ctx.drawTextWithShadow(textRenderer, modeTag(m), tagX(m, px), ry + 7, TEXT_DIM);

                // delay widget
                if (m.hasDelay()) {
                    int mx0 = minusX(m, px);
                    int px0 = plusX(m, px);
                    ctx.fill(mx0, ry + 5, mx0 + BTN, ry + ROW - 5, KEY_BG);
                    ctx.drawTextWithShadow(textRenderer, "-", mx0 + 4, ry + 7, TEXT_ACCENT);
                    ctx.drawTextWithShadow(textRenderer, String.valueOf(m.getDelay()), valueX(m, px), ry + 7, TEXT);
                    ctx.fill(px0, ry + 5, px0 + BTN, ry + ROW - 5, KEY_BG);
                    ctx.drawTextWithShadow(textRenderer, "+", px0 + 3, ry + 7, TEXT_ACCENT);
                }

                if (hover) tip = m.getDescription() + "  (click key box: rebind, right click: hold/toggle)";
            }
        }

        if (listening != null) tip = "Press a key to bind it, Escape to cancel";
        if (tip != null) ctx.drawTextWithShadow(textRenderer, tip, 14, height - 18, TEXT_DIM);

        super.render(ctx, mouseX, mouseY, delta);
    }

    // ---- input ----

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        List<String> cats = categories();
        double mx = click.x();
        double my = click.y();

        for (int c = 0; c < cats.size(); c++) {
            List<Module> mods = inCategory(cats.get(c));
            int px = panelX(c, cats.size());
            for (int i = 0; i < mods.size(); i++) {
                Module m = mods.get(i);
                int ry = TOP + HEADER + i * ROW;
                if (mx >= px && mx < px + W && my >= ry && my < ry + ROW) {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    int kw = keyBoxWidth(m);
                    int kx = keyBoxX(m, px);

                    if (click.button() == 0 && mx >= kx && mx < kx + kw) {
                        listening = m;
                    } else if (click.button() == 0 && m.hasDelay()
                            && mx >= minusX(m, px) && mx < minusX(m, px) + BTN) {
                        listening = null;
                        m.changeDelay(-1);
                    } else if (click.button() == 0 && m.hasDelay()
                            && mx >= plusX(m, px) && mx < plusX(m, px) + BTN) {
                        listening = null;
                        m.changeDelay(1);
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
