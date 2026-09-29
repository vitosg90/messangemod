package com.example.chatbubbles;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** Меню настроек мода. */
public class SettingsScreen extends Screen {
    private final Screen parent;

    public SettingsScreen(Screen parent) {
        super(Component.translatable("chatbubbles.settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        BubbleConfig c = BubbleConfig.get();
        int w = 220;
        int x = this.width / 2 - w / 2;
        int step = 24;
        int y = Math.max(28, (this.height - 8 * step) / 2);

        addRenderableWidget(toggle(x, y, w, "chatbubbles.option.enabled", () -> c.enabled, v -> c.enabled = v));
        y += step;
        addRenderableWidget(toggle(x, y, w, "chatbubbles.option.system", () -> c.showSystemMessages, v -> c.showSystemMessages = v));
        y += step;
        addRenderableWidget(new IntSlider(x, y, w, "chatbubbles.option.lifetime", 1, 60, c.lifetimeSeconds, v -> c.lifetimeSeconds = v));
        y += step;
        addRenderableWidget(new IntSlider(x, y, w, "chatbubbles.option.max", 1, 12, c.maxBubbles, v -> c.maxBubbles = v));
        y += step;
        addRenderableWidget(new IntSlider(x, y, w, "chatbubbles.option.scale", 50, 200, c.scalePercent, v -> c.scalePercent = v));
        y += step;
        addRenderableWidget(new IntSlider(x, y, w, "chatbubbles.option.opacity", 20, 100, c.opacityPercent, v -> c.opacityPercent = v));
        y += step;
        addRenderableWidget(new IntSlider(x, y, w, "chatbubbles.option.radius", 50, 150, c.radiusPercent, v -> c.radiusPercent = v));
        y += step;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose())
                .bounds(x, y, w, 20).build());
    }

    private Button toggle(int x, int y, int w, String key, BooleanSupplier get, Consumer<Boolean> set) {
        return Button.builder(toggleLabel(key, get.getAsBoolean()), b -> {
            boolean now = !get.getAsBoolean();
            set.accept(now);
            b.setMessage(toggleLabel(key, now));
        }).bounds(x, y, w, 20).build();
    }

    private static Component toggleLabel(String key, boolean on) {
        return Component.translatable(key, Component.translatable(on ? "options.on" : "options.off"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        String t = this.title.getString();
        graphics.text(this.font, t, (this.width - this.font.width(t)) / 2, 12, 0xFFFFFFFF, true);
    }

    @Override
    public void onClose() {
        BubbleConfig.save();
        this.minecraft.gui.setScreen(parent);
    }

    /** Ползунок с целым значением. */
    private static class IntSlider extends AbstractSliderButton {
        private final String key;
        private final int min;
        private final int max;
        private final IntConsumer setter;

        IntSlider(int x, int y, int w, String key, int min, int max, int current, IntConsumer setter) {
            super(x, y, w, 20, Component.empty(), (current - min) / (double) (max - min));
            this.key = key;
            this.min = min;
            this.max = max;
            this.setter = setter;
            updateMessage();
        }

        private int current() {
            return min + (int) Math.round(this.value * (max - min));
        }

        @Override
        protected void updateMessage() {
            if (key == null) return; // вызов из конструктора родителя
            setMessage(Component.translatable(key, current()));
        }

        @Override
        protected void applyValue() {
            setter.accept(current());
        }
    }
}
