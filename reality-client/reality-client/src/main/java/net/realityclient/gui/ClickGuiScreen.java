package net.realityclient.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.realityclient.RealityClient;
import net.realityclient.module.Module;
import net.realityclient.module.ShieldBreaker;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClickGuiScreen extends Screen {
    private static final int W = 210;
    private static final int ROW = 38;
    private static final int SROW = 26;
    private static final int HEADER = 34;
    private static final int GAP = 14;
    private static final int TOP = 106;
    private static final int MARGIN = 20;
    private static final int BTN = 14;

    private static final List<String> BASE_CATEGORIES =
            List.of("COMBAT", "MOVEMENT", "RENDER", "MISC", "CLIENT");

    // click sound (assets/realityclient/sounds/click.ogg + assets/realityclient/sounds.json)
    private static final SoundEvent CLICK_SOUND = SoundEvent.of(Identifier.of("realityclient", "click"));

    // liquid glass palette
    private static final int GLASS = 0x584A7198;
    private static final int GLASS_HEADER = 0x70658DB8;
    private static final int BORDER = 0x80618DBB;
    private static final int SHEEN = 0x70FFFFFF;
    private static final int ROW_HOVER = 0x38FFFFFF;
    private static final int ROW_ON = 0xFF55A9FF;
    private static final int SET_BG = 0x38000000;
    private static final int ACCENT = 0xFF55A9FF;
    private static final int ACCENT_SOFT = 0xAA55A9FF;
    private static final int KEY_BG = 0x50000000;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFFE0EAF5;
    private static final int TEXT_ACCENT = 0xFFD5EAFF;

    private final Set<String> collapsed = new HashSet<>();
    private final Set<Module> expanded = new HashSet<>();
    private Module listening = null;

    public ClickGuiScreen() {
        super(Text.literal(RealityClient.NAME));
    }

    // ---- data helpers ----

    private String titleCase(String value) {
        if (value == null || value.isEmpty()) return value;
        String lower = value.toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

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

    private int settingCount(Module m) {
        return m instanceof ShieldBreaker ? 5 : (m.hasDelay() ? 3 : 2);
    }

    private int panelHeight(String cat) {
        int h = HEADER + 4;
        if (collapsed.contains(cat)) return h;
        List<Module> mods = inCategory(cat);
        if (mods.isEmpty()) return h + ROW;
        for (Module m : mods) {
            h += ROW;
            if (expanded.contains(m)) h += settingCount(m) * SROW;
        }
        return h;
    }

    /** Returns {x, y, height} for every panel, in the same order as categories(). */
    private List<int[]> layout(List<String> cats) {
        List<int[]> out = new ArrayList<>();
        int x = MARGIN;
        int y = TOP;
        int rowH = 0;
        for (String c : cats) {
            int h = panelHeight(c);
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

    private String modeTag(Module m) {
        return m.isHoldMode() ? "HOLD" : "TOGGLE";
    }

    // delay widget positions (right aligned inside the settings row)
    private int rightEdge(int px) { return px + W - 8; }
    private int plusX(int px) { return rightEdge(px) - BTN; }
    private int valueX(Module m, int px) {
        return plusX(px) - 4 - textRenderer.getWidth(String.valueOf(m.getDelay()));
    }
    private int minusX(Module m, int px) { return valueX(m, px) - 4 - BTN; }

    private void drawDelayRow(DrawContext ctx, int px, int dy, String label, int value) {
        ctx.drawTextWithShadow(textRenderer, label, px + 20, dy + 5, TEXT_DIM);
        int right = rightEdge(px);
        int plus = right - BTN;
        int valueRight = plus - 4;
        String valueText = value * 50 + " ms";
        int valueX = valueRight - textRenderer.getWidth(valueText);
        int minus = valueX - 4 - BTN;
        ctx.fill(minus, dy + 3, minus + BTN, dy + SROW - 3, KEY_BG);
        ctx.drawTextWithShadow(textRenderer, "-", minus + 4, dy + 5, TEXT_ACCENT);
        ctx.drawTextWithShadow(textRenderer, valueText, valueX, dy + 5, TEXT);
        ctx.fill(plus, dy + 3, plus + BTN, dy + SROW - 3, KEY_BG);
        ctx.drawTextWithShadow(textRenderer, "+", plus + 3, dy + 5, TEXT_ACCENT);
    }

    private int delayMinusX(int px, int value) {
        String valueText = value * 50 + " ms";
        int plus = rightEdge(px) - BTN;
        return plus - 4 - textRenderer.getWidth(valueText) - 4 - BTN;
    }

    // ---- glass drawing helpers ----

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
        super.render(ctx, mouseX, mouseY, delta);

        // Screenshot-style navigation bar.
        int navW = Math.min(650, width - 40);
        int navX = (width - navW) / 2;
        glass(ctx, navX, 10, navW, 42, 0x706F6A7B);
        outline(ctx, navX, 10, navW, 42, 0x906A6275);
        String[] tabs = {"Modules", "HUD", "Friends", "Theme", "Config", "Donut"};
        int tabW = navW / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            int tx = navX + i * tabW + 4;
            if (i == 0) glass(ctx, tx, 14, tabW - 8, 34, ACCENT);
            int tw = textRenderer.getWidth(tabs[i]);
            ctx.drawTextWithShadow(textRenderer, tabs[i],
                    tx + (tabW - 8 - tw) / 2, 26, i == 0 ? 0xFF241A25 : TEXT_DIM);
        }

        // Search field visual, aligned below the navigation.
        int searchW = Math.min(400, width - 80);
        int searchX = (width - searchW) / 2;
        glass(ctx, searchX, 62, searchW, 31, 0x704C4857);
        outline(ctx, searchX, 62, searchW, 31, 0x806B6475);
        ctx.drawTextWithShadow(textRenderer, "⌕  Search modules...", searchX + 14, 73, 0xFFBDB6C5);

        List<String> cats = categories();
        List<int[]> rects = layout(cats);
        String tip = null;

        for (int c = 0; c < cats.size(); c++) {
            String cat = cats.get(c);
            int px = rects.get(c)[0];
            int py = rects.get(c)[1];
            int ph = rects.get(c)[2];
            boolean isCollapsed = collapsed.contains(cat);

            // panel body + header
            glass(ctx, px, py, W, ph, GLASS);
            ctx.fill(px + 2, py + 1, px + W - 2, py + HEADER, GLASS_HEADER);
            ctx.fill(px + 4, py + 1, px + W - 4, py + 2, SHEEN);
            ctx.fill(px + 10, py + HEADER, px + W - 10, py + HEADER + 1, ACCENT_SOFT);
            ctx.drawTextWithShadow(textRenderer, titleCase(cat), px + 12, py + 9, TEXT);
            ctx.drawTextWithShadow(textRenderer, isCollapsed ? ">" : "v", px + W - 16, py + 9, TEXT_DIM);
            outline(ctx, px, py, W, ph, BORDER);

            if (isCollapsed) continue;

            List<Module> mods = inCategory(cat);
            if (mods.isEmpty()) {
                ctx.drawTextWithShadow(textRenderer, "No modules yet", px + 12, py + HEADER + 7, TEXT_DIM);
                continue;
            }

            int y = py + HEADER + 2;
            for (Module m : mods) {
                boolean hover = mouseX >= px && mouseX < px + W && mouseY >= y && mouseY < y + ROW;
                boolean on = m.isEnabled();

                if (on) {
                    ctx.fill(px + 3, y, px + W - 3, y + ROW, ROW_ON);
                    ctx.fill(px + 3, y, px + 5, y + ROW, ACCENT);
                } else if (hover) {
                    ctx.fill(px + 3, y, px + W - 3, y + ROW, ROW_HOVER);
                }

                ctx.drawTextWithShadow(textRenderer, m.getName(), px + 12, y + 7, on ? TEXT_ACCENT : TEXT);
                ctx.drawTextWithShadow(textRenderer, expanded.contains(m) ? "v" : ">", px + W - 16, y + 7, TEXT_DIM);

                if (hover) tip = m.getDescription() + "  (left click: on/off, right click: settings)";
                y += ROW;

                if (expanded.contains(m)) {
                    int n = settingCount(m);
                    ctx.fill(px + 3, y, px + W - 3, y + n * SROW, SET_BG);

                    if (m instanceof ShieldBreaker sb) {
                        ctx.drawTextWithShadow(textRenderer, "Enabled", px + 20, y + 5, TEXT_DIM);
                        String enabled = sb.isEnabled() ? "ON" : "OFF";
                        int ex = rightEdge(px) - 38;
                        ctx.fill(ex, y + 2, rightEdge(px), y + SROW - 2, KEY_BG);
                        ctx.drawTextWithShadow(textRenderer, enabled,
                                ex + (38 - textRenderer.getWidth(enabled)) / 2, y + 5,
                                sb.isEnabled() ? ACCENT : TEXT_DIM);

                        int ay = y + SROW;
                        ctx.drawTextWithShadow(textRenderer, "Attack Mode", px + 20, ay + 5, TEXT_DIM);
                        String attackMode = sb.getAttackMode() == ShieldBreaker.AttackMode.AUTO ? "AUTO" : "MANUAL";
                        ctx.drawTextWithShadow(textRenderer, attackMode, rightEdge(px) - textRenderer.getWidth(attackMode), ay + 5, TEXT_ACCENT);

                        int by = y + 2 * SROW;
                        ctx.drawTextWithShadow(textRenderer, "Break Type", px + 20, by + 5, TEXT_DIM);
                        String breakMode = sb.getBreakMode() == ShieldBreaker.BreakMode.STUN ? "STUN" : "NORMAL";
                        ctx.drawTextWithShadow(textRenderer, breakMode, rightEdge(px) - textRenderer.getWidth(breakMode), by + 5, TEXT_ACCENT);

                        drawDelayRow(ctx, px, y + 3 * SROW, "Switch Delay", m.getDelay());
                        drawDelayRow(ctx, px, y + 4 * SROW, "Switch Back Delay", sb.getSwitchBackDelay());
                    } else {
                    // keybind row
                    ctx.drawTextWithShadow(textRenderer, "Keybind", px + 20, y + 5, TEXT_DIM);
                    String kt = keyText(m);
                    int kw = textRenderer.getWidth(kt) + 8;
                    int kx = rightEdge(px) - kw;
                    ctx.fill(kx, y + 2, kx + kw, y + SROW - 2, KEY_BG);
                    ctx.drawTextWithShadow(textRenderer, kt, kx + 4, y + 5, listening == m ? ACCENT : TEXT_ACCENT);

                    // mode row
                    int my2 = y + SROW;
                    ctx.drawTextWithShadow(textRenderer, "Mode", px + 20, my2 + 5, TEXT_DIM);
                    String tag = modeTag(m);
                    ctx.drawTextWithShadow(textRenderer, tag, rightEdge(px) - textRenderer.getWidth(tag), my2 + 5, TEXT_ACCENT);

                    // delay row
                    if (m.hasDelay()) {
                        int dy = y + 2 * SROW;
                        ctx.drawTextWithShadow(textRenderer, "Switch Delay", px + 20, dy + 5, TEXT_DIM);
                        int mx0 = minusX(m, px);
                        int px0 = plusX(px);
                        ctx.fill(mx0, dy + 3, mx0 + BTN, dy + SROW - 3, KEY_BG);
                        ctx.drawTextWithShadow(textRenderer, "-", mx0 + 4, dy + 5, TEXT_ACCENT);
                        ctx.drawTextWithShadow(textRenderer, String.valueOf(m.getDelay()), valueX(m, px), dy + 5, TEXT);
                        ctx.fill(px0, dy + 3, px0 + BTN, dy + SROW - 3, KEY_BG);
                        ctx.drawTextWithShadow(textRenderer, "+", px0 + 3, dy + 5, TEXT_ACCENT);
                    }

                    if (m instanceof ShieldBreaker sb) {
                        int ay = y + 3 * SROW;
                        ctx.drawTextWithShadow(textRenderer, "Attack Mode", px + 20, ay + 5, TEXT_DIM);
                        String attackMode = sb.getAttackMode() == ShieldBreaker.AttackMode.AUTO ? "AUTO" : "MANUAL";
                        ctx.drawTextWithShadow(textRenderer, attackMode, rightEdge(px) - textRenderer.getWidth(attackMode), ay + 5, TEXT_ACCENT);

                        int by = y + 4 * SROW;
                        ctx.drawTextWithShadow(textRenderer, "Break Type", px + 20, by + 5, TEXT_DIM);
                        String breakMode = sb.getBreakMode() == ShieldBreaker.BreakMode.STUN ? "STUN" : "NORMAL";
                        ctx.drawTextWithShadow(textRenderer, breakMode, rightEdge(px) - textRenderer.getWidth(breakMode), by + 5, TEXT_ACCENT);
                    }

                    }                    y += n * SROW;
                }
            }
        }

        if (listening != null) tip = "Press a key to bind it, Escape to cancel";
        if (tip != null) ctx.drawTextWithShadow(textRenderer, tip, MARGIN, height - 18, TEXT_DIM);
    }

    // ---- input ----

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        boolean handled = handleClick(click, doubled);
        if (handled) {
            MinecraftClient.getInstance().getSoundManager()
                    .play(PositionedSoundInstance.master(CLICK_SOUND, 1.0f));
        }
        return handled;
    }

    private boolean handleClick(Click click, boolean doubled) {
        List<String> cats = categories();
        List<int[]> rects = layout(cats);
        double mx = click.x();
        double my = click.y();
        MinecraftClient mc = MinecraftClient.getInstance();

        for (int c = 0; c < cats.size(); c++) {
            String cat = cats.get(c);
            int px = rects.get(c)[0];
            int py = rects.get(c)[1];
            if (mx < px || mx >= px + W) continue;

            // header: collapse / expand the whole panel
            if (my >= py && my < py + HEADER) {
                listening = null;
                if (!collapsed.remove(cat)) collapsed.add(cat);
                return true;
            }
            if (collapsed.contains(cat)) continue;

            int y = py + HEADER + 2;
            for (Module m : inCategory(cat)) {
                // module row
                if (my >= y && my < y + ROW) {
                    listening = null;
                    if (click.button() == 0) {
                        m.toggle(mc);
                    } else if (click.button() == 1) {
                        if (!expanded.remove(m)) expanded.add(m);
                    }
                    return true;
                }
                y += ROW;

                // settings rows
                if (expanded.contains(m)) {
                    int n = settingCount(m);
                    if (my >= y && my < y + n * SROW) {
                        int idx = (int) ((my - y) / SROW);
                        if (click.button() == 0) handleSetting(m, idx, mx, px);
                        return true;
                    }
                    y += n * SROW;
                }
            }
        }
        listening = null;
        return super.mouseClicked(click, doubled);
    }

    private void handleSetting(Module m, int idx, double mx, int px) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (m instanceof ShieldBreaker sb) {
            if (idx == 0) {
                listening = null;
                sb.toggle(mc);
            } else if (idx == 1) {
                listening = null;
                sb.cycleAttackMode();
            } else if (idx == 2) {
                listening = null;
                sb.cycleBreakMode();
            } else if (idx == 3) {
                listening = null;
                int mx0 = delayMinusX(px, m.getDelay());
                int px0 = plusX(px);
                if (mx >= mx0 && mx < mx0 + BTN) m.changeDelay(-1);
                else if (mx >= px0 && mx < px0 + BTN) m.changeDelay(1);
            } else if (idx == 4) {
                listening = null;
                int value = sb.getSwitchBackDelay();
                int mx0 = delayMinusX(px, value);
                int px0 = plusX(px);
                if (mx >= mx0 && mx < mx0 + BTN) sb.changeSwitchBackDelay(-1);
                else if (mx >= px0 && mx < px0 + BTN) sb.changeSwitchBackDelay(1);
            }
            return;
        }
        if (idx == 0) {
            listening = m;
        } else if (idx == 1) {
            listening = null;
            m.switchMode(mc);
        } else if (idx == 2 && m.hasDelay()) {
            listening = null;
            int mx0 = minusX(m, px);
            int px0 = plusX(px);
            if (mx >= mx0 && mx < mx0 + BTN) m.changeDelay(-1);
            else if (mx >= px0 && mx < px0 + BTN) m.changeDelay(1);
        }
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
