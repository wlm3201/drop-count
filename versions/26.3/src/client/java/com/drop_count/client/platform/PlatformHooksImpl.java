package com.drop_count.client.platform;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Minecraft 26.3: same layout as 26.2, kept separate so that later changes (26.3 switched
 * from GLFW to SDL input) stay local to this version.
 */
public final class PlatformHooksImpl implements PlatformHooks {

    @Override
    public Camera getCamera(final Minecraft minecraft) {
        return minecraft.gameRenderer.mainCamera();
    }

    @Override
    public void showOverlayMessage(final Minecraft minecraft, final Component message) {
        minecraft.gui.hud.setOverlayMessage(message, false);
    }
}
