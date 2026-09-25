package com.dropcount.client;

import com.dropcount.DropCount;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Just the one setting: whether the labels are drawn at all. */
public final class DropCountConfig {
    private static final Gson GSON = new Gson();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("drop-count.json");

    private static boolean enabled = true;

    private DropCountConfig() {
    }

    public static boolean enabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        save();
    }

    public static void load() {
        if (!Files.isRegularFile(PATH)) return;

        try (Reader reader = Files.newBufferedReader(PATH)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json != null && json.has("enabled")) {
                enabled = json.get("enabled").getAsBoolean();
            }
        } catch (IOException | RuntimeException e) {
            DropCount.LOGGER.warn("Failed to read drop-count config", e);
        }
    }

    private static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            JsonObject json = new JsonObject();
            json.addProperty("enabled", enabled);
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            DropCount.LOGGER.warn("Failed to write drop-count config", e);
        }
    }
}
