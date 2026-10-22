package com.dropcount.client.mixin;

import com.dropcount.client.itemtags.ItemTagsModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class OverlayRenderStateMixin {

    @Inject(
            method =
                    "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At("TAIL"))
    private void dropCount$renderItemTags(
            final GuiGraphicsExtractor graphics,
            final DeltaTracker deltaTracker,
            final CallbackInfo info) {
        ItemTagsModule.render(graphics, deltaTracker);
    }
}
