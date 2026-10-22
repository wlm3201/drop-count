package com.dropcount.client;

import com.dropcount.client.config.ItemTagsConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DropCountClient implements ClientModInitializer {
    public static final String MOD_ID = "dropcount";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitializeClient() {
        ItemTagsConfigManager.init();
    }
}
