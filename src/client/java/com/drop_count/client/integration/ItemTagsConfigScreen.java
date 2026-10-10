package com.drop_count.client.integration;

import com.drop_count.client.config.ItemTagsConfig;
import com.drop_count.client.config.ItemTagsConfigManager;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.CyclingListControllerBuilder;
import dev.isxander.yacl3.api.controller.DoubleFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Builds the YACL screen for the ItemTags settings. The values are written back into
 * {@code config/drop-count.json}, so the GUI and manual file edits stay in sync.
 */
public final class ItemTagsConfigScreen {

    private ItemTagsConfigScreen() {
    }

    public static Screen create(final Screen parent) {
        final ItemTagsConfig defaults = ItemTagsConfig.DEFAULT;
        final ItemTagsConfig current = ItemTagsConfigManager.get();
        final Draft draft = Draft.of(current);

        return YetAnotherConfigLib.createBuilder()
            .title(Component.translatable("drop-count.config.title"))
            .category(generalCategory(defaults, draft))
            .category(appearanceCategory(defaults, draft))
            .category(shulkerCategory(defaults, draft))
            .save(() -> {
                ItemTagsConfigManager.set(draft.toConfig());
                ItemTagsConfigManager.save();
            })
            .build()
            .generateScreen(parent);
    }

    private static ConfigCategory generalCategory(final ItemTagsConfig defaults, final Draft draft) {
        return ConfigCategory.createBuilder()
            .name(text("category.general"))
            .option(Option.<Boolean>createBuilder()
                .name(text("option.enabled"))
                .description(OptionDescription.of(text("option.enabled.description")))
                .binding(defaults.enabled, () -> draft.enabled, value -> draft.enabled = value)
                .controller(TickBoxControllerBuilder::create)
                .build())
            .option(Option.<ItemTagsConfig.Filter>createBuilder()
                .name(text("option.filter"))
                .description(OptionDescription.of(text("option.filter.description")))
                .binding(defaults.filter, () -> draft.filter, value -> draft.filter = value)
                .controller(option -> CyclingListControllerBuilder.create(option)
                    .values(ItemTagsConfig.Filter.values())
                    .valueFormatter(filter -> text("filter." + filter.jsonName())))
                .build())
            .option(Option.<ItemTagsConfig.MergeMode>createBuilder()
                .name(text("option.merge_mode"))
                .description(OptionDescription.of(text("option.merge_mode.description")))
                .binding(defaults.mergeMode, () -> draft.mergeMode, value -> draft.mergeMode = value)
                .controller(option -> CyclingListControllerBuilder.create(option)
                    .values(ItemTagsConfig.MergeMode.values())
                    .valueFormatter(mode -> text("merge_mode." + mode.jsonName())))
                .build())
            // A ListOption is a group of its own, YACL rejects it as a plain option
            .group(ListOption.<String>createBuilder()
                .name(text("option.items"))
                .description(OptionDescription.of(text("option.items.description")))
                .binding(defaults.itemIds, () -> draft.items, value -> draft.items = new ArrayList<>(value))
                .controller(StringControllerBuilder::create)
                .initial("")
                .build())
            .build();
    }

    private static ConfigCategory appearanceCategory(final ItemTagsConfig defaults, final Draft draft) {
        return ConfigCategory.createBuilder()
            .name(text("category.appearance"))
            .option(Option.<Color>createBuilder()
                .name(text("option.background_color"))
                .binding(
                    new Color(defaults.backgroundColor, true),
                    () -> new Color(draft.backgroundColor, true),
                    value -> draft.backgroundColor = value.getRGB()
                )
                .controller(option -> ColorControllerBuilder.create(option).allowAlpha(true))
                .build())
            .option(Option.<Integer>createBuilder()
                .name(text("option.row_length"))
                .description(OptionDescription.of(text("option.row_length.description")))
                .binding(defaults.rowLength, () -> draft.rowLength, value -> draft.rowLength = value)
                .controller(option -> IntegerSliderControllerBuilder.create(option).range(1, 100).step(1))
                .build())
            .option(Option.<Boolean>createBuilder()
                .name(text("option.prevent_overlap"))
                .description(OptionDescription.of(text("option.prevent_overlap.description")))
                .binding(defaults.preventOverlap, () -> draft.preventOverlap, value -> draft.preventOverlap = value)
                .controller(TickBoxControllerBuilder::create)
                .build())
            .option(Option.<Integer>createBuilder()
                .name(text("option.max_distance"))
                .description(OptionDescription.of(text("option.max_distance.description")))
                .binding(defaults.maxDistance, () -> draft.maxDistance, value -> draft.maxDistance = value)
                .controller(option -> IntegerSliderControllerBuilder.create(option)
                    .range(ItemTagsConfig.MIN_MAX_DISTANCE, ItemTagsConfig.MAX_MAX_DISTANCE)
                    .step(1))
                .build())
            .option(Option.<Double>createBuilder()
                .name(text("option.scale"))
                .description(OptionDescription.of(text("option.scale.description")))
                .binding((double) defaults.scale, () -> draft.scale, value -> draft.scale = value)
                .controller(option -> DoubleSliderControllerBuilder.create(option)
                    .range((double) ItemTagsConfig.MIN_SCALE, (double) ItemTagsConfig.MAX_SCALE)
                    .step(0.05))
                .build())
            .option(Option.<Double>createBuilder()
                .name(text("option.distance_falloff"))
                .description(OptionDescription.of(text("option.distance_falloff.description")))
                .binding((double) defaults.distanceFalloff, () -> draft.distanceFalloff, value -> draft.distanceFalloff = value)
                .controller(option -> DoubleSliderControllerBuilder.create(option)
                    .range((double) ItemTagsConfig.MIN_DISTANCE_FALLOFF, (double) ItemTagsConfig.MAX_DISTANCE_FALLOFF)
                    .step(0.05))
                .build())
            .group(OptionGroup.createBuilder()
                .name(text("group.render_offset"))
                .option(doubleField("option.render_offset.x", defaults.renderOffset.x, () -> draft.offsetX, value -> draft.offsetX = value))
                .option(doubleField("option.render_offset.y", defaults.renderOffset.y, () -> draft.offsetY, value -> draft.offsetY = value))
                .option(doubleField("option.render_offset.z", defaults.renderOffset.z, () -> draft.offsetZ, value -> draft.offsetZ = value))
                .build())
            .build();
    }

    private static ConfigCategory shulkerCategory(final ItemTagsConfig defaults, final Draft draft) {
        return ConfigCategory.createBuilder()
            .name(text("category.shulker"))
            .option(Option.<Boolean>createBuilder()
                .name(text("option.shulker_enabled"))
                .description(OptionDescription.of(text("option.shulker_enabled.description")))
                .binding(defaults.shulkerEnabled, () -> draft.shulkerEnabled, value -> draft.shulkerEnabled = value)
                .controller(TickBoxControllerBuilder::create)
                .build())
            .option(Option.<Boolean>createBuilder()
                .name(text("option.shulker_merge_stacks"))
                .binding(defaults.shulkerMergeStacks, () -> draft.shulkerMergeStacks, value -> draft.shulkerMergeStacks = value)
                .controller(TickBoxControllerBuilder::create)
                .build())
            .option(Option.<Boolean>createBuilder()
                .name(text("option.shulker_show_title"))
                .binding(defaults.shulkerShowTitle, () -> draft.shulkerShowTitle, value -> draft.shulkerShowTitle = value)
                .controller(TickBoxControllerBuilder::create)
                .build())
            .build();
    }

    private static Option<Double> doubleField(
        final String key,
        final double defaultValue,
        final Supplier<Double> getter,
        final Consumer<Double> setter
    ) {
        return Option.<Double>createBuilder()
            .name(text(key))
            .binding(defaultValue, getter, setter)
            .controller(DoubleFieldControllerBuilder::create)
            .build();
    }

    private static Component text(final String key) {
        return Component.translatable("drop-count." + key);
    }

    /**
     * Mutable copy of the config, the bindings of all options write into it.
     */
    private static final class Draft {
        private boolean enabled;
        private ItemTagsConfig.Filter filter;
        private List<String> items;
        private int backgroundColor;
        private double offsetX;
        private double offsetY;
        private double offsetZ;
        private int rowLength;
        private boolean preventOverlap;
        private ItemTagsConfig.MergeMode mergeMode;
        private int maxDistance;
        private double scale;
        private double distanceFalloff;
        private boolean shulkerEnabled;
        private boolean shulkerMergeStacks;
        private boolean shulkerShowTitle;

        private static Draft of(final ItemTagsConfig config) {
            final Draft draft = new Draft();
            draft.enabled = config.enabled;
            draft.filter = config.filter;
            draft.items = new ArrayList<>(config.itemIds);
            draft.backgroundColor = config.backgroundColor;
            draft.offsetX = config.renderOffset.x;
            draft.offsetY = config.renderOffset.y;
            draft.offsetZ = config.renderOffset.z;
            draft.rowLength = config.rowLength;
            draft.preventOverlap = config.preventOverlap;
            draft.mergeMode = config.mergeMode;
            draft.maxDistance = config.maxDistance;
            draft.scale = config.scale;
            draft.distanceFalloff = config.distanceFalloff;
            draft.shulkerEnabled = config.shulkerEnabled;
            draft.shulkerMergeStacks = config.shulkerMergeStacks;
            draft.shulkerShowTitle = config.shulkerShowTitle;
            return draft;
        }

        private ItemTagsConfig toConfig() {
            return new ItemTagsConfig(
                this.enabled,
                this.filter,
                this.items,
                this.backgroundColor,
                new Vec3(this.offsetX, this.offsetY, this.offsetZ),
                this.rowLength,
                this.preventOverlap,
                this.mergeMode,
                this.maxDistance,
                (float) this.scale,
                (float) this.distanceFalloff,
                this.shulkerEnabled,
                this.shulkerMergeStacks,
                this.shulkerShowTitle
            );
        }

    }
}
