package com.drop_count.client.mixin;

import com.drop_count.client.itemtags.ItemTagsModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minecraft 26.1.2 renders the HUD inside {@link Gui}, later versions use {@code Hud}.
 */
@Mixin(Gui.class)
public class OverlayRenderStateMixin {

    @Inject(
        method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
        at = @At("TAIL")
    )
    private void dropCount$renderItemTags(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker, final CallbackInfo info) {
        ItemTagsModule.render(graphics, deltaTracker);
    }
}
