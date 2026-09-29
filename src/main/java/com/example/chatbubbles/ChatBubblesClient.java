package com.example.chatbubbles;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class ChatBubblesClient implements ClientModInitializer {
    public static final String MOD_ID = "chatbubbles";

    /** Открывает меню настроек. */
    public static KeyMapping SETTINGS_KEY;
    /** Включает режим курсора, чтобы кликать по облачкам. */
    public static KeyMapping CURSOR_KEY;

    @Override
    public void onInitializeClient() {
        BubbleConfig.load();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        SETTINGS_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.chatbubbles.settings", InputConstants.Type.KEYSYM, InputConstants.KEY_O, category));
        CURSOR_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.chatbubbles.cursor", InputConstants.Type.KEYSYM, InputConstants.KEY_K, category));

        // Сообщения игроков и системные сообщения сервера.
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, timestamp) ->
                BubbleManager.get().add(message));
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay && BubbleConfig.get().showSystemMessages) {
                BubbleManager.get().add(message);
            }
        });

        // Рисуем облачка на HUD прямо перед чатом.
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "bubbles"),
                (graphics, delta) -> BubbleManager.get().render(graphics, Minecraft.getInstance().font));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (SETTINGS_KEY.consumeClick()) {
                if (client.player != null && client.gui.screen() == null) {
                    client.gui.setScreen(new SettingsScreen(null));
                }
            }
            while (CURSOR_KEY.consumeClick()) {
                if (client.player != null && client.gui.screen() == null) {
                    client.gui.setScreen(new CursorScreen());
                }
            }
        });
    }
}
