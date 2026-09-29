package com.example.chatbubbles;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Облачка в 3D-мире вокруг игрока. Каждое облачко занимает своё место на кольце,
 * всегда повёрнуто к камере. ЛКМ, когда целишься в облачко, убирает его.
 */
public final class BubbleManager {
    private static final BubbleManager INSTANCE = new BubbleManager();
    private static final int MAX_TEXT_WIDTH = 150;
    private static final int MAX_LINES = 6;
    private static final long FADE_MS = 1000L;
    private static final float BASE_SCALE = 0.025f;   // как у ников над головой
    private static final double BASE_RADIUS = 2.0;     // блоков от игрока
    private static final int FULL_BRIGHT = 0xF000F0;

    public static BubbleManager get() {
        return INSTANCE;
    }

    private static final class Bubble {
        final Component text;
        final long created;
        final int slot;
        List<FormattedCharSequence> lines;
        int maxWidth;
        int heightPx;

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

    private void ensureLines(Bubble b, Font font) {
        if (b.lines != null) return;
        List<FormattedCharSequence> split = font.split(b.text, MAX_TEXT_WIDTH);
        b.lines = split.size() > MAX_LINES ? new ArrayList<>(split.subList(0, MAX_LINES)) : split;
        int w = 0;
        for (FormattedCharSequence line : b.lines) w = Math.max(w, font.width(line));
        b.maxWidth = w;
        b.heightPx = b.lines.size() * font.lineHeight;
    }

    /** Место облачка в мире: кольцо вокруг глаз игрока, по три высоты. */
    private Vec3 worldPos(Bubble b, Vec3 eye, BubbleConfig c) {
        double angle = 2 * Math.PI * b.slot / c.maxBubbles;
        double r = BASE_RADIUS * c.radiusPercent / 100.0;
        double dy = -0.2 + (b.slot % 3) * 0.45;
        return eye.add(Math.cos(angle) * r, dy, Math.sin(angle) * r);
    }

    /** Облачко, в которое сейчас целится прицел (или null). */
    private Bubble findAimed(Minecraft mc, float partialTick) {
        var player = mc.player;
        if (player == null) return null;
        BubbleConfig c = BubbleConfig.get();
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 dir = player.getViewVector(partialTick);
        float s = BASE_SCALE * c.scalePercent / 100f;

        Bubble best = null;
        double bestT = Double.MAX_VALUE;
        for (Bubble b : bubbles) {
            if (b.lines == null) continue;
            Vec3 rel = worldPos(b, eye, c).subtract(eye);
            double t = rel.dot(dir);
            if (t <= 0.3) continue;
            double distSq = rel.lengthSqr() - t * t;
            double rad = Math.max(b.maxWidth, b.heightPx) * s / 2.0 + 0.1;
            if (distSq <= rad * rad && t < bestT) {
                best = b;
                bestT = t;
            }
        }
        return best;
    }

    /**
     * Вызывается в начале тика. Если прицел на облачке, клик ЛКМ убирает облачко
     * и не доходит до игры (не бьёшь и не ломаешь блок).
     */
    public void tryDismissAimed(Minecraft mc) {
        if (mc.player == null || mc.gui.screen() != null || bubbles.isEmpty()) return;
        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Bubble aimed = findAimed(mc, pt);
        if (aimed == null) return;

        var attack = mc.options.keyAttack;
        boolean clicked = false;
        while (attack.consumeClick()) clicked = true;
        attack.setDown(false);
        if (clicked) bubbles.remove(aimed);
    }

    /** Отправляет облачка на отрисовку в мире. */
    public void render(LevelRenderContext ctx) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        BubbleConfig c = BubbleConfig.get();
        prune();
        if (player == null || !c.enabled || bubbles.isEmpty()) return;

        Font font = mc.font;
        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 eye = player.getEyePosition(pt);
        Vec3 cam = ctx.levelState().cameraRenderState.pos;
        for (Bubble b : bubbles) ensureLines(b, font);
        Bubble aimed = findAimed(mc, pt);

        var pose = ctx.poseStack();
        var collector = ctx.submitNodeCollector();
        float s = BASE_SCALE * c.scalePercent / 100f;
        long now = System.currentTimeMillis();
        long life = c.lifetimeSeconds * 1000L;

        for (Bubble b : bubbles) {
            long age = now - b.created;
            float fade = age > life - FADE_MS ? Math.max(0f, (life - age) / (float) FADE_MS) : 1f;
            int a = (int) (255 * fade * c.opacityPercent / 100f);
            if (a < 8) continue;

            Vec3 p = worldPos(b, eye, c);
            double dx = cam.x - p.x;
            double dy = cam.y - p.y;
            double dz = cam.z - p.z;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 0.05) continue;

            // Поворот так, чтобы облачко смотрело на камеру.
            Vector3f zAxis = new Vector3f((float) (dx / len), (float) (dy / len), (float) (dz / len));
            Vector3f xAxis = new Vector3f(0, 1, 0).cross(zAxis);
            if (xAxis.lengthSquared() < 1e-6f) xAxis.set(1, 0, 0);
            else xAxis.normalize();
            Vector3f yAxis = new Vector3f(zAxis).cross(xAxis);
            Quaternionf rot = new Quaternionf().setFromNormalized(new Matrix3f(xAxis, yAxis, zAxis));

            int rgb = (b == aimed) ? 0xFFD84A : 0xFFFFFF;
            int color = (a << 24) | rgb;
            int bg = ((int) (a * 0.55f)) << 24;

            pose.pushPose();
            pose.translate(p.x - cam.x, p.y - cam.y, p.z - cam.z);
            pose.mulPose(rot);
            pose.scale(s, -s, s);

            float y0 = -b.heightPx / 2f;
            for (int i = 0; i < b.lines.size(); i++) {
                FormattedCharSequence line = b.lines.get(i);
                float x = -font.width(line) / 2f;
                float y = y0 + i * font.lineHeight;
                collector.submitText(pose, x, y, line, false, Font.DisplayMode.SEE_THROUGH,
                        FULL_BRIGHT, color, bg, 0);
            }
            pose.popPose();
        }
    }
}
