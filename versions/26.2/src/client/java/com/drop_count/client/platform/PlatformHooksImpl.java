package com.drop_count.client.platform;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Minecraft 26.2: the HUD moved into {@link net.minecraft.client.gui.Hud} and the camera is
 * exposed by {@code GameRenderer#mainCamera()}.
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
