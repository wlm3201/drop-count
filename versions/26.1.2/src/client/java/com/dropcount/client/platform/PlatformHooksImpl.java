package com.dropcount.client.platform;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

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
