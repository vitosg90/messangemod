package com.example.chatbubbles;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Прозрачный экран: освобождает курсор, чтобы можно было кликнуть ЛКМ по облачку.
 * Закрывается повторным нажатием клавиши или Esc.
 */
public class CursorScreen extends Screen {
    public CursorScreen() {
        super(Component.empty());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // super не вызываем намеренно: так не рисуется затемнение и мир остаётся чистым.
        BubbleManager.get().drawHover(graphics, mouseX, mouseY);
        String hint = Component.translatable("chatbubbles.cursor.hint").getString();
        graphics.text(this.font, hint, (this.width - this.font.width(hint)) / 2, this.height - 40, 0xFFFFFFFF, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && BubbleManager.get().dismissAt(event.x(), event.y())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (ChatBubblesClient.CURSOR_KEY != null && ChatBubblesClient.CURSOR_KEY.matches(event)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }
}
