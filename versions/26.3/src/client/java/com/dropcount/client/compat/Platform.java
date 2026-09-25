package com.dropcount.client.compat;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

/**
 * The handful of calls that are not spelled the same in every game version we build against.
 * Everything else in the mod compiles unchanged; if a new version breaks something else, it belongs
 * in here.
 *
 * <p>26.3: {@code PoseStack.mulPose} no longer takes a quaternion, and the GLFW key codes are gone
 * -- keys are named by {@code InputConstants} now, and {@code Type.KEYSYM} became {@code KEYBOARD}.
 */
public final class Platform {
    private Platform() {
    }

    public static void showActionBar(Minecraft mc, Component message) {
        mc.gui.hud.setOverlayMessage(message, false);
    }

    public static void faceCamera(PoseStack poseStack, CameraRenderState camera) {
        poseStack.mulPose(camera.orientation.get(new Matrix4f()));
    }

    public static KeyMapping createToggleKey(KeyMapping.Category category) {
        return new KeyMapping("key.dropcount.toggle", InputConstants.Type.KEYBOARD, InputConstants.KEY_O, category);
    }
}
