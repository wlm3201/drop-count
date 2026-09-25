package com.dropcount.client.compat;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * The handful of calls that are not spelled the same in every game version we build against.
 * Everything else in the mod compiles unchanged; if a new version breaks something else, it belongs
 * in here.
 *
 * <p>26.1: the overlay message is on {@code Gui}, {@code PoseStack} takes a quaternion and keys are
 * named by GLFW.
 */
public final class Platform {
    private Platform() {
    }

    public static void showActionBar(Minecraft mc, Component message) {
        mc.gui.setOverlayMessage(message, false);
    }

    public static void faceCamera(PoseStack poseStack, CameraRenderState camera) {
        poseStack.mulPose(camera.orientation);
    }

    public static KeyMapping createToggleKey(KeyMapping.Category category) {
        return new KeyMapping("key.dropcount.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, category);
    }
}
