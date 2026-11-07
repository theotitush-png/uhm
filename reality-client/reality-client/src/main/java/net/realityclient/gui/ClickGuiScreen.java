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
    private static final int W = 170;
    private static final int ROW = 22;
    private static final int HEADER = 26;
    private static final int GAP = 12;
    private static final int TOP = 40;
    private static final int MARGIN = 20;
    private static final int BTN = 12;

    private static final List<String> BASE_CATEGORIES =
            List.of("COMBAT", "MOVEMENT", "RENDER", "MISC", "CLIENT");

    // liquid glass palette
    private static final int GLASS = 0x38E8FFF0;
    private static final int GLASS_HEADER = 0x58E8FFF0;
    private static final int BORDER = 0x90C9F7DA;
    private static final int SHEEN = 0x80FFFFFF;
    private static final int ROW_HOVER = 0x30FFFFFF;
    private static final int ROW_ON = 0x6022C55E;
    private static final int ACCENT = 0xFF22C55E;
    private static final int ACCENT_SOFT = 0xAA22C55E;
    private static final int KEY_BG = 0x50000000;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFFD2E8DB;
    private static final int TEXT_ACCENT = 0xFFD1FADF;

    private Module listening = null;

    public ClickGuiScreen() {
        super(Text.literal(RealityClient.NAME));
    }

    // ---- layout helpers ----

    private List<String> categories() {
        List<String> out = new ArrayList<>(BASE_CATEGORIES);
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

    /** Returns {x, y, height} for every panel, in the same order as categories(). */
    private List<int[]> layout(List<String> cats) {
        List<int[]> out = new ArrayList<>();
        int x = MARGIN;
        int y = TOP;
        int rowH = 0;
        for (String c : cats) {
            int n = Math.max(1, inCategory(c).size());
            int h = HEADER + n * ROW + 4;
            if (x + W > width - MARGIN && x > MARGIN) {
                x = MARGIN;
                y += rowH + GAP;
                rowH = 0;
            }
            out.add(new int[]{x, y, h});
            x += W + GAP;
            rowH = Math.max(rowH, h);
        }
        return out;
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

    private int keyBoxX(Module m, int px) { return px + W - keyBoxWidth(m) - 8; }

    private int tagX(Module m, int px) {
        return keyBoxX(m, px) - textRenderer.getWidth(modeTag(m)) - 8;
    }

    private int plusX(Module m, int px) { return tagX(m, px) - 8 - BTN; }

    private int valueX(Module m, int px) {
        int dw = textRenderer.getWidth(String.valueOf(m.getDelay()));
        return plusX(m, px) - 4 - dw;
    }

    private int minusX(Module m, int px) { return valueX(m, px) - 4 - BTN; }

    // ---- glass drawing helpers ----

    /** Filled rectangle with slightly rounded corners (no overlapping fills, so translucency stays even). */
    private void glass(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x + 2, y, x + w - 2, y + 1, color);
        ctx.fill(x + 1, y + 1, x + w - 1, y + 2, color);
        ctx.fill(x, y + 2, x + w, y + h - 2, color);
        ctx.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, color);
        ctx.fill(x + 2, y + h - 1, x + w - 2, y + h, color);
    }

    private void outline(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x + 2, y, x + w - 2, y + 1, color);
        ctx.fill(x + 2, y + h - 1, x + w - 2, y + h, color);
        ctx.fill(x, y + 2, x + 1, y + h - 2, color);
        ctx.fill(x + w - 1, y + 2, x + w, y + h - 2, color);
        ctx.fill(x + 1, y + 1, x + 2, y + 2, color);
        ctx.fill(x + w - 2, y + 1, x + w - 1, y + 2, color);
        ctx.fill(x + 1, y + h - 2, x + 2, y + h - 1, color);
        ctx.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, color);
    }

    // ---- drawing ----

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // vanilla blurred background first, our glass goes on top of it
        super.render(ctx, mouseX, mouseY, delta);

        ctx.drawTextWithShadow(textRenderer, RealityClient.NAME, MARGIN, 16, ACCENT);

        List<String> cats = categories();
        List<int[]> rects = layout(cats);
        String tip = null;

        for (int c = 0; c < cats.size(); c++) {
            List<Module> mods = inCategory(cats.get(c));
            int px = rects.get(c)[0];
            int py = rects.get(c)[1];
            int ph = rects.get(c)[2];

            // panel body
            glass(ctx, px, py, W, ph, GLASS);
            // header
            ctx.fill(px + 2, py + 1, px + W - 2, py + HEADER, GLASS_HEADER);
            ctx.fill(px + 4, py + 1, px + W - 4, py + 2, SHEEN);
            ctx.fill(px + 10, py + HEADER, px + W - 10, py + HEADER + 1, ACCENT_SOFT);
            ctx.drawTextWithShadow(textRenderer, cats.get(c), px + 12, py + 9, TEXT);
            outline(ctx, px, py, W, ph, BORDER);

            if (mods.isEmpty()) {
                ctx.drawTextWithShadow(textRenderer, "No modules yet", px + 12, py + HEADER + 7, TEXT_DIM);
                continue;
            }

            for (int i = 0; i < mods.size(); i++) {
                Module m = mods.get(i);
                int ry = py + HEADER + 2 + i * ROW;
                boolean hover = mouseX >= px && mouseX < px + W && mouseY >= ry && mouseY < ry + ROW;
                boolean on = m.isEnabled();

                if (on) {
                    ctx.fill(px + 3, ry, px + W - 3, ry + ROW, ROW_ON);
                    ctx.fill(px + 3, ry, px + 5, ry + ROW, ACCENT);
                } else if (hover) {
                    ctx.fill(px + 3, ry, px + W - 3, ry + ROW, ROW_HOVER);
                }

                ctx.drawTextWithShadow(textRenderer, m.getName(), px + 12, ry + 7, on ? TEXT_ACCENT : TEXT);

                // key box
                int kw = keyBoxWidth(m);
                int kx = keyBoxX(m, px);
                ctx.fill(kx, ry + 4, kx + kw, ry + ROW - 4, KEY_BG);
                ctx.drawTextWithShadow(textRenderer, keyText(m), kx + 4, ry + 7, listening == m ? ACCENT : TEXT_ACCENT);

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
        if (tip != null) ctx.drawTextWithShadow(textRenderer, tip, MARGIN, height - 18, TEXT_DIM);
    }

    // ---- input ----

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        List<String> cats = categories();
        List<int[]> rects = layout(cats);
        double mx = click.x();
        double my = click.y();

        for (int c = 0; c < cats.size(); c++) {
            List<Module> mods = inCategory(cats.get(c));
            int px = rects.get(c)[0];
            int py = rects.get(c)[1];
            for (int i = 0; i < mods.size(); i++) {
                Module m = mods.get(i);
                int ry = py + HEADER + 2 + i * ROW;
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
