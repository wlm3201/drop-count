package com.drop_count.client.mixin;

import com.drop_count.client.itemtags.ItemTagsKeybinding;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

/**
 * Adds the mod's key mapping to the vanilla list, so that it shows up in the controls screen
 * and is stored in {@code options.txt}.
 */
@Mixin(Options.class)
public class OptionsMixin {

    @Mutable
    @Shadow
    @Final
    public KeyMapping[] keyMappings;

    @Inject(method = "<init>(Lnet/minecraft/client/Minecraft;Ljava/io/File;)V", at = @At("TAIL"))
    private void dropCount$registerKeyMappings(final CallbackInfo info) {
        final KeyMapping mapping = ItemTagsKeybinding.TOGGLE_ITEM_TAGS;
        final KeyMapping[] original = this.keyMappings;

        for (final KeyMapping existing : original) {
            if (existing == mapping) {
                return;
            }
        }

        final KeyMapping[] extended = Arrays.copyOf(original, original.length + 1);
        extended[original.length] = mapping;
        this.keyMappings = extended;
    }
}
