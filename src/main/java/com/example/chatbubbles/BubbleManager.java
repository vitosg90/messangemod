package com.example.chatbubbles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Хранит облачка с сообщениями, рисует их вокруг центра экрана и обрабатывает клики. */
public final class BubbleManager {
    private static final BubbleManager INSTANCE = new BubbleManager();
    private static final int MAX_TEXT_WIDTH = 150;
    private static final int MAX_LINES = 6;
    private static final long FADE_MS = 1000L;

    public static BubbleManager get() {
        return INSTANCE;
    }

    private static final class Bubble {
        final Component text;
        final long created;
        final int slot;
        List<FormattedCharSequence> lines;
        int x1, y1, x2, y2;
        boolean visible;

        Bubble(Component text, long created, int slot) {
            this.text = text;
            this.created = created;
            this.slot = slot;
        }
    }

    private final List<Bubble> bubbles = new ArrayList<>();

    private void prune() {
        BubbleConfig c = BubbleConfig.get();
        long now = System.currentTimeMillis();
        long life = c.lifetimeSeconds * 1000L;
        bubbles.removeIf(b -> now - b.created >= life || b.slot >= c.maxBubbles);
    }

    public void add(Component text) {
        BubbleConfig c = BubbleConfig.get();
        if (!c.enabled) return;
        if (text.getString().isBlank()) return;
        prune();

        int slots = c.maxBubbles;
        boolean[] used = new boolean[slots];
        for (Bubble b : bubbles) used[b.slot] = true;

        int slot = -1;
        for (int i = 0; i < slots; i++) {
            if (!used[i]) {
                slot = i;
                break;
            }
        }
        if (slot < 0) {
            // Все места заняты: заменяем самое старое облачко.
            Bubble oldest = bubbles.get(0);
            slot = oldest.slot;
            bubbles.remove(oldest);
        }
        bubbles.add(new Bubble(text, System.currentTimeMillis(), slot));
    }

    public void clear() {
        bubbles.clear();
    }

    public void render(GuiGraphicsExtractor g, Font font) {
        BubbleConfig c = BubbleConfig.get();
        prune();
        if (!c.enabled || bubbles.isEmpty()) return;

        var window = Minecraft.getInstance().getWindow();
        int sw = window.getGuiScaledWidth();
        int sh = window.getGuiScaledHeight();
        float scale = c.scalePercent / 100f;
        double rx = sw * 0.30 * c.radiusPercent / 100.0;
        double ry = sh * 0.28 * c.radiusPercent / 100.0;
        long now = System.currentTimeMillis();
        long life = c.lifetimeSeconds * 1000L;

        for (Bubble b : bubbles) {
            long age = now - b.created;
            float fade = age > life - FADE_MS ? Math.max(0f, (life - age) / (float) FADE_MS) : 1f;
            int a = (int) (255 * fade * c.opacityPercent / 100f);
            if (a < 8) {
                b.visible = false;
                continue;
            }

            if (b.lines == null) {
                List<FormattedCharSequence> split = font.split(b.text, MAX_TEXT_WIDTH);
                b.lines = split.size() > MAX_LINES ? new ArrayList<>(split.subList(0, MAX_LINES)) : split;
            }
            int textW = 0;
            for (FormattedCharSequence line : b.lines) textW = Math.max(textW, font.width(line));
            int bw = textW + 8;
            int bh = b.lines.size() * font.lineHeight + 8;

            double angle = -Math.PI / 2 + 2 * Math.PI * b.slot / c.maxBubbles;
            double halfW = bw * scale / 2.0;
            double halfH = bh * scale / 2.0;
            double cx = sw / 2.0 + rx * Math.cos(angle);
            double cy = sh / 2.0 + ry * Math.sin(angle);
            cx = Math.max(halfW + 2, Math.min(sw - halfW - 2, cx));
            cy = Math.max(halfH + 2, Math.min(sh - halfH - 2, cy));

            b.x1 = (int) (cx - halfW);
            b.y1 = (int) (cy - halfH);
            b.x2 = (int) (cx + halfW);
            b.y2 = (int) (cy + halfH);
            b.visible = true;

            var pose = g.pose();
            pose.pushMatrix();
            pose.translate((float) cx, (float) cy);
            pose.scale(scale, scale);

            int x0 = -bw / 2;
            int y0 = -bh / 2;
            int border = (a << 24) | 0xFFFFFF;
            int bg = (((int) (a * 0.75f)) << 24) | 0x101018;
            g.fill(x0 - 1, y0 - 1, x0 + bw + 1, y0 + bh + 1, border);
            g.fill(x0, y0, x0 + bw, y0 + bh, bg);

            int ty = y0 + 4;
            int textColor = (a << 24) | 0xFFFFFF;
            for (FormattedCharSequence line : b.lines) {
                g.text(font, line, x0 + 4, ty, textColor, false);
                ty += font.lineHeight;
            }
            pose.popMatrix();
        }
    }

    /** Подсветка облачка под курсором (для режима курсора). */
    public void drawHover(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        Bubble b = find(mouseX, mouseY);
        if (b == null) return;
        int col = 0xFFFFD84A;
        g.fill(b.x1 - 3, b.y1 - 3, b.x2 + 3, b.y1 - 1, col);
        g.fill(b.x1 - 3, b.y2 + 1, b.x2 + 3, b.y2 + 3, col);
        g.fill(b.x1 - 3, b.y1 - 1, b.x1 - 1, b.y2 + 1, col);
        g.fill(b.x2 + 1, b.y1 - 1, b.x2 + 3, b.y2 + 1, col);
    }

    /** Убирает облачко под курсором. Возвращает true, если что-то убрали. */
    public boolean dismissAt(double mx, double my) {
        Bubble b = find(mx, my);
        if (b == null) return false;
        bubbles.remove(b);
        return true;
    }

    private Bubble find(double mx, double my) {
        for (int i = bubbles.size() - 1; i >= 0; i--) {
            Bubble b = bubbles.get(i);
            if (b.visible && mx >= b.x1 && mx <= b.x2 && my >= b.y1 && my <= b.y2) return b;
        }
        return null;
    }
}
