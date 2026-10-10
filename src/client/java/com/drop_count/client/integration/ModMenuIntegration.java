package com.drop_count.client.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

/**
 * Registers the config screen shown by Mod Menu. Fabric only loads this class when Mod Menu
 * is installed, so the dependency stays optional.
 */
public final class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<Screen> getModConfigScreenFactory() {
        return ItemTagsConfigScreen::create;
    }
}
