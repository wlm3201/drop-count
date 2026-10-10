package com.drop_count.client.platform;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Minecraft 26.1.2: the HUD is still part of {@link net.minecraft.client.gui.Gui} and
 * the camera is exposed by {@code GameRenderer#getMainCamera()}.
 */
public final class PlatformHooksImpl implements PlatformHooks {

    @Override
    public Camera getCamera(final Minecraft minecraft) {
        return minecraft.gameRenderer.getMainCamera();
    }

    @Override
    public void showOverlayMessage(final Minecraft minecraft, final Component message) {
        minecraft.gui.setOverlayMessage(message, false);
    }
}
