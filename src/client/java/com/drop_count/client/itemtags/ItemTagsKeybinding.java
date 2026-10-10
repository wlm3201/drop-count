package com.drop_count.client.itemtags;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

/**
 * The vanilla key binding used to toggle the ItemTags overlay.
 *
 * <p>{@link InputConstants#KEY_B} is a GLFW key code up to Minecraft 26.2 and an SDL scancode
 * from 26.3 on, the two argument constructor maps it to the matching input type.</p>
 */
public final class ItemTagsKeybinding {

    public static final String TOGGLE_TRANSLATION_KEY = "key.drop_count.toggle_item_tags";

    public static final KeyMapping TOGGLE_ITEM_TAGS = new KeyMapping(
        TOGGLE_TRANSLATION_KEY,
        InputConstants.KEY_B,
        KeyMapping.Category.MISC
    );

    private ItemTagsKeybinding() {
    }
}
