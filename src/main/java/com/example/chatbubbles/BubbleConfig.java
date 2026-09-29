package com.example.chatbubbles;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Настройки мода. Хранятся в config/chatbubbles.json. */
public class BubbleConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static BubbleConfig instance;

    public boolean enabled = true;
    public boolean showSystemMessages = true;
    public int lifetimeSeconds = 10;
    public int maxBubbles = 8;
    public int scalePercent = 100;
    public int opacityPercent = 90;
    public int radiusPercent = 100;

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("chatbubbles.json");
    }

    public static BubbleConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        BubbleConfig loaded = null;
        Path f = file();
        if (Files.exists(f)) {
            try (Reader r = Files.newBufferedReader(f)) {
                loaded = GSON.fromJson(r, BubbleConfig.class);
            } catch (Exception e) {
                System.err.println("[chatbubbles] Не удалось прочитать конфиг: " + e);
            }
        }
        instance = loaded != null ? loaded : new BubbleConfig();
        instance.clamp();
    }

    public static void save() {
        if (instance == null) return;
        instance.clamp();
        try (Writer w = Files.newBufferedWriter(file())) {
            GSON.toJson(instance, w);
        } catch (Exception e) {
            System.err.println("[chatbubbles] Не удалось сохранить конфиг: " + e);
        }
    }

    private void clamp() {
        lifetimeSeconds = Math.max(1, Math.min(60, lifetimeSeconds));
        maxBubbles = Math.max(1, Math.min(12, maxBubbles));
        scalePercent = Math.max(50, Math.min(200, scalePercent));
        opacityPercent = Math.max(20, Math.min(100, opacityPercent));
        radiusPercent = Math.max(50, Math.min(150, radiusPercent));
    }
}
