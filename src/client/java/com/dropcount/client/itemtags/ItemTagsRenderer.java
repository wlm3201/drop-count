package com.dropcount.client.itemtags;

import com.dropcount.client.config.ItemTagsConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

final class ItemTagsRenderer {

    private static final int ITEM_SIZE = GuiRenderer.DEFAULT_ITEM_SIZE;
    private static final float MARGIN = 2.0F;
    private static final int MAX_REARRANGE_ITERATIONS = 16;

    private ItemTagsRenderer() {}

    static final class Box {
        private final List<ItemStack> stacks;
        private final Component title;
        private final float scale;

        private float centerX;
        private float centerY;
        private int contentWidth;
        private int contentHeight;
        private float halfWidth;
        private float halfHeight;

        private Box(
                final List<ItemStack> stacks,
                final Component title,
                final float centerX,
                final float centerY,
                final float scale) {
            this.stacks = stacks;
            this.title = title;
            this.centerX = centerX;
            this.centerY = centerY;
            this.scale = scale;
        }

        static Box of(
                final List<ItemStack> stacks,
                final Component title,
                final float centerX,
                final float centerY,
                final float scale) {
            return new Box(stacks, title, centerX, centerY, scale);
        }

        void measure(final Font font, final int rowLength) {
            int width = ITEM_SIZE * Math.min(this.stacks.size(), rowLength);
            int rows =
                    this.stacks.size() / rowLength + (this.stacks.size() % rowLength != 0 ? 1 : 0);
            int height = ITEM_SIZE * rows;

            if (this.title != null) {
                width = Math.max(width, font.width(this.title));
                height += font.lineHeight + (this.stacks.isEmpty() ? 0 : 2);
            }

            this.contentWidth = width;
            this.contentHeight = height;
            this.halfWidth = (width + MARGIN * 2F) * this.scale * 0.5F;
            this.halfHeight = (height + MARGIN * 2F) * this.scale * 0.5F;
        }

        private void offset(final float dx, final float dy) {
            this.centerX += dx;
            this.centerY += dy;
        }
    }

    static void draw(
            final GuiGraphicsExtractor graphics, final Box box, final ItemTagsConfig config) {
        final Font font = Minecraft.getInstance().font;
        final int margin = (int) MARGIN;

        graphics.pose().pushMatrix();
        graphics.pose().translate(box.centerX, box.centerY);
        graphics.pose().scale(box.scale, box.scale);
        graphics.pose().translate(-box.contentWidth * 0.5F, -box.contentHeight * 0.5F);

        graphics.fill(
                -margin,
                -margin,
                box.contentWidth + margin,
                box.contentHeight + margin,
                config.backgroundColor);

        if (box.title != null) {
            graphics.centeredText(font, box.title, box.contentWidth / 2, 0, 0xFFFFFFFF);
            graphics.pose().translate(0F, font.lineHeight + 2F);
        }

        for (int i = 0; i < box.stacks.size(); i++) {
            final ItemStack stack = box.stacks.get(i);
            if (stack.isEmpty()) {
                continue;
            }

            final int x = i % config.rowLength * ITEM_SIZE;
            final int y = i / config.rowLength * ITEM_SIZE;
            graphics.item(stack, x, y);
            graphics.itemDecorations(font, stack, x, y);
        }

        graphics.pose().popMatrix();
    }

    static void rearrange(final List<Box> boxes) {
        if (boxes.size() <= 1) {
            return;
        }

        final List<Box> sorted = new ArrayList<>(boxes);
        sorted.sort(
                Comparator.comparingDouble((Box box) -> box.centerY)
                        .thenComparingDouble(box -> box.centerX));

        for (int iteration = 0; iteration < MAX_REARRANGE_ITERATIONS; iteration++) {
            boolean moved = false;

            for (int i = 0; i < sorted.size(); i++) {
                final Box a = sorted.get(i);

                for (int j = i + 1; j < sorted.size(); j++) {
                    final Box b = sorted.get(j);

                    final float dx = a.halfWidth + b.halfWidth - Math.abs(a.centerX - b.centerX);
                    final float dy = a.halfHeight + b.halfHeight - Math.abs(a.centerY - b.centerY);

                    if (dx <= 0F || dy <= 0F) {
                        continue;
                    }

                    if (dx < dy) {
                        b.offset(a.centerX < b.centerX ? dx : -dx, 0F);
                    } else {
                        b.offset(0F, a.centerY < b.centerY ? dy : -dy);
                    }

                    moved = true;
                }
            }

            if (!moved) {
                break;
            }
        }
    }
}
