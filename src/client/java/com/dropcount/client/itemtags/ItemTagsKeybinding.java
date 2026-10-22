package com.dropcount.client.itemtags;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

public final class ItemTagsKeybinding {

    public static final String TOGGLE_TRANSLATION_KEY = "key.drop_count.toggle_item_tags";

    public static final KeyMapping TOGGLE_ITEM_TAGS =
            new KeyMapping(TOGGLE_TRANSLATION_KEY, InputConstants.KEY_B, KeyMapping.Category.MISC);

    private ItemTagsKeybinding() {}
}
