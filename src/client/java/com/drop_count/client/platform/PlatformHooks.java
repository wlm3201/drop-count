package com.drop_count.client.platform;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * The parts of the Minecraft API that changed between the supported versions.
 * {@link PlatformHooksImpl} is provided by each version sub project.
 */
public interface PlatformHooks {

    /**
     * @see net.minecraft.client.renderer.GameRenderer
     */
    Camera getCamera(Minecraft minecraft);

    /**
     * Shows the message above the hotbar.
     */
    void showOverlayMessage(Minecraft minecraft, Component message);
}
