package com.dropcount.client.config;

import com.dropcount.client.DropCountClient;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;
import net.fabricmc.loader.api.FabricLoader;

public final class ItemTagsConfigManager {

    private static final Path PATH =
            FabricLoader.getInstance().getConfigDir().resolve("dropcount.json");
    private static final long SETTLE_DELAY_MILLIS = 120L;
    private static final long POLL_INTERVAL_MILLIS = 400L;

    private static volatile ItemTagsConfig config = ItemTagsConfig.DEFAULT;
    private static long lastModified = -1L;

    private ItemTagsConfigManager() {}

    public static Path path() {
        return PATH;
    }

    public static ItemTagsConfig get() {
        return config;
    }

    public static void init() {
        if (Files.isRegularFile(PATH)) {
            loadFromDisk(false);
        } else {
            config = ItemTagsConfig.DEFAULT;
            save();
        }

        DropCountClient.LOGGER.info("[dropcount] ItemTags config: {}", PATH);
        startWatcher();
    }

    public static synchronized void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, config.toPrettyString(), StandardCharsets.UTF_8);
            lastModified = readLastModified();
        } catch (final IOException exception) {
            DropCountClient.LOGGER.error("[dropcount] Failed to write {}", PATH, exception);
        }
    }

    public static void set(final ItemTagsConfig newConfig) {
        config = newConfig;
    }

    public static void toggleEnabled() {
        config = config.withEnabled(!config.enabled);
        save();
    }

    private static void loadFromDisk(final boolean announce) {
        try {
            final JsonObject root =
                    JsonParser.parseString(Files.readString(PATH, StandardCharsets.UTF_8))
                            .getAsJsonObject();
            config = ItemTagsConfig.read(root);
            lastModified = readLastModified();

            if (announce) {
                DropCountClient.LOGGER.info(
                        "[dropcount] ItemTags config reloaded from {}", PATH.getFileName());
            }
        } catch (final IOException | JsonSyntaxException | IllegalStateException exception) {
            lastModified = readLastModified();
            DropCountClient.LOGGER.warn(
                    "[dropcount] Invalid config in {}, keeping previous values ({})",
                    PATH.getFileName(),
                    exception.getMessage());
        }
    }

    private static void startWatcher() {
        final Thread thread = new Thread(() -> watchLoop(), "dropcount-config-watcher");
        thread.setDaemon(true);
        thread.start();
    }

    private static void watchLoop() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            Path directory = PATH.getParent();
            if (directory == null) {
                return;
            }

            Files.createDirectories(directory);
            directory.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);

            while (!Thread.currentThread().isInterrupted()) {
                final WatchKey key = watchService.poll(POLL_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
                if (key != null) {
                    key.pollEvents();
                    key.reset();
                }

                final long modified = readLastModified();
                if (modified > 0L
                        && modified != lastModified
                        && System.currentTimeMillis() - modified >= SETTLE_DELAY_MILLIS) {
                    loadFromDisk(true);
                }
            }
        } catch (final IOException exception) {
            DropCountClient.LOGGER.warn(
                    "[dropcount] Config watcher stopped: {}", exception.getMessage());
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static long readLastModified() {
        try {
            return Files.getLastModifiedTime(PATH).toMillis();
        } catch (final IOException exception) {
            return -1L;
        }
    }
}
