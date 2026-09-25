package com.dropcount.client;

import com.dropcount.DropCount;
import com.dropcount.client.compat.Platform;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class DropCountClient implements ClientModInitializer {
    public static final KeyMapping.Category KEY_CATEGORY =
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath(DropCount.MOD_ID, "dropcount"));

    public static KeyMapping toggleKey;

    @Override
    public void onInitializeClient() {
        DropCountConfig.load();

        toggleKey = Platform.createToggleKey(KEY_CATEGORY);
        KeyMappingHelper.registerKeyMapping(toggleKey);

        ClientTickEvents.END_CLIENT_TICK.register(DropCountClient::onClientTick);
    }

    private static void onClientTick(Minecraft mc) {
        if (toggleKey.consumeClick()) {
            boolean enabled = !DropCountConfig.enabled();
            DropCountConfig.setEnabled(enabled);
            Platform.showActionBar(mc, Component.translatable(
                enabled ? "dropcount.message.enabled" : "dropcount.message.disabled"));
        }
    }
}
