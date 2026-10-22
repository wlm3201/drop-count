package com.dropcount.client.mixin;

import com.dropcount.client.itemtags.ItemTagsKeybinding;
import com.dropcount.client.itemtags.ItemTagsModule;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftTickMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void dropCount$onClientTick(final CallbackInfo info) {
        while (ItemTagsKeybinding.TOGGLE_ITEM_TAGS.consumeClick()) {
            ItemTagsModule.toggle();
        }

        ItemTagsModule.onClientTick(Minecraft.getInstance());
    }
}
