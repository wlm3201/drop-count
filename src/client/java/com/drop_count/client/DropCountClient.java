package com.drop_count.client;

import com.drop_count.client.config.ItemTagsConfigManager;
import net.fabricmc.api.ClientModInitializer;

public class DropCountClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ItemTagsConfigManager.init();
	}
}
