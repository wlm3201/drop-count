package com.dropcount.client.platform;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public interface PlatformHooks {

    Camera getCamera(Minecraft minecraft);

    void showOverlayMessage(Minecraft minecraft, Component message);
}
