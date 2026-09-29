package com.example.chatbubbles;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class ChatBubblesClient implements ClientModInitializer {
    public static final String MOD_ID = "chatbubbles";

    /** Открывает меню настроек. */
    public static KeyMapping SETTINGS_KEY;

    @Override
    public void onInitializeClient() {
        BubbleConfig.load();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        SETTINGS_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.chatbubbles.settings", InputConstants.Type.KEYSYM, InputConstants.KEY_O, category));

        // Сообщения игроков и системные сообщения сервера.
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, timestamp) ->
                BubbleManager.get().add(message));
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay && BubbleConfig.get().showSystemMessages) {
                BubbleManager.get().add(message);
            }
        });

        // Облачка в мире.
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> BubbleManager.get().render(context));

        // Клик ЛКМ по облачку: обрабатываем до того, как клик увидит игра.
        ClientTickEvents.START_CLIENT_TICK.register(client -> BubbleManager.get().tryDismissAimed(client));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (SETTINGS_KEY.consumeClick()) {
                if (client.player != null && client.gui.screen() == null) {
                    client.gui.setScreen(new SettingsScreen(null));
                }
            }
        });
    }
}
