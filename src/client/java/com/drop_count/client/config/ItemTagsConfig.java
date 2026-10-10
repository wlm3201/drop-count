package com.drop_count.client.config;

import com.drop_count.DropCount;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Settings of the ItemTags feature. The defaults are the defaults of LiquidBounce's ItemTags module.
 */
public final class ItemTagsConfig {

    public static final int DEFAULT_BACKGROUND_COLOR = 0x80000000;
    public static final int DEFAULT_ROW_LENGTH = 100;
    /** Only item entities closer than this (in blocks) get a tag, same limit as vanilla name tags. */
    public static final int DEFAULT_MAX_DISTANCE = 32;
    public static final int MIN_MAX_DISTANCE = 4;
    public static final int MAX_MAX_DISTANCE = 256;

    /** Extra multiplier applied to every tag, 1 keeps the vanilla name tag size. */
    public static final float DEFAULT_SCALE = 1.0F;
    public static final float MIN_SCALE = 0.05F;
    public static final float MAX_SCALE = 4.0F;

    /** Exponent of the perspective shrink, 1 is the vanilla {@code 1 / distance} behaviour. */
    public static final float DEFAULT_DISTANCE_FALLOFF = 1.0F;
    public static final float MIN_DISTANCE_FALLOFF = 0.0F;
    public static final float MAX_DISTANCE_FALLOFF = 2.0F;

    public static final ItemTagsConfig DEFAULT = new ItemTagsConfig(
        true,
        Filter.BLACKLIST,
        List.of(),
        DEFAULT_BACKGROUND_COLOR,
        new Vec3(0D, 1D, 0D),
        DEFAULT_ROW_LENGTH,
        true,
        MergeMode.BY_COMPONENTS,
        DEFAULT_MAX_DISTANCE,
        DEFAULT_SCALE,
        DEFAULT_DISTANCE_FALLOFF,
        true,
        true,
        false
    );

    public final boolean enabled;
    public final Filter filter;
    public final List<String> itemIds;
    public final Set<Item> items;
    public final int backgroundColor;
    public final Vec3 renderOffset;
    public final int rowLength;
    public final boolean preventOverlap;
    public final MergeMode mergeMode;
    public final int maxDistance;
    public final float scale;
    public final float distanceFalloff;
    public final boolean shulkerEnabled;
    public final boolean shulkerMergeStacks;
    public final boolean shulkerShowTitle;

    public ItemTagsConfig(
        final boolean enabled,
        final Filter filter,
        final List<String> itemIds,
        final int backgroundColor,
        final Vec3 renderOffset,
        final int rowLength,
        final boolean preventOverlap,
        final MergeMode mergeMode,
        final int maxDistance,
        final float scale,
        final float distanceFalloff,
        final boolean shulkerEnabled,
        final boolean shulkerMergeStacks,
        final boolean shulkerShowTitle
    ) {
        this.enabled = enabled;
        this.filter = filter;
        this.itemIds = List.copyOf(itemIds);
        this.items = resolveItems(itemIds);
        this.backgroundColor = backgroundColor;
        this.renderOffset = renderOffset;
        this.rowLength = Math.max(1, rowLength);
        this.preventOverlap = preventOverlap;
        this.mergeMode = mergeMode;
        this.maxDistance = Math.min(Math.max(maxDistance, MIN_MAX_DISTANCE), MAX_MAX_DISTANCE);
        this.scale = Math.min(Math.max(scale, MIN_SCALE), MAX_SCALE);
        this.distanceFalloff = Math.min(Math.max(distanceFalloff, MIN_DISTANCE_FALLOFF), MAX_DISTANCE_FALLOFF);
        this.shulkerEnabled = shulkerEnabled;
        this.shulkerMergeStacks = shulkerMergeStacks;
        this.shulkerShowTitle = shulkerShowTitle;
    }

    public ItemTagsConfig withEnabled(final boolean enabled) {
        return new ItemTagsConfig(
            enabled,
            this.filter,
            this.itemIds,
            this.backgroundColor,
            this.renderOffset,
            this.rowLength,
            this.preventOverlap,
            this.mergeMode,
            this.maxDistance,
            this.scale,
            this.distanceFalloff,
            this.shulkerEnabled,
            this.shulkerMergeStacks,
            this.shulkerShowTitle
        );
    }

    public boolean allows(final Item item) {
        return this.filter.allows(item, this.items);
    }

    /**
     * Serializes the config, keeping coordinate arrays on a single line so that the file is
     * pleasant to edit by hand.
     */
    public String toPrettyString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("{\n");

        appendValue(builder, 1, "enabled", String.valueOf(this.enabled), true);
        appendValue(builder, 1, "filter", quote(this.filter.jsonName()), true);
        appendValue(builder, 1, "items", inlineArray(quoted(this.itemIds)), true);
        appendValue(builder, 1, "backgroundColor", quote(toHexColor(this.backgroundColor)), true);
        appendValue(
            builder,
            1,
            "renderOffset",
            inlineArray(List.of(number(this.renderOffset.x), number(this.renderOffset.y), number(this.renderOffset.z))),
            true
        );
        appendValue(builder, 1, "rowLength", String.valueOf(this.rowLength), true);
        appendValue(builder, 1, "preventOverlap", String.valueOf(this.preventOverlap), true);
        appendValue(builder, 1, "mergeMode", quote(this.mergeMode.jsonName()), true);
        appendValue(builder, 1, "maxDistance", String.valueOf(this.maxDistance), true);
        appendValue(builder, 1, "scale", number(this.scale), true);
        appendValue(builder, 1, "distanceFalloff", number(this.distanceFalloff), true);

        appendObjectStart(builder, 1, "shulker");
        appendValue(builder, 2, "enabled", String.valueOf(this.shulkerEnabled), true);
        appendValue(builder, 2, "mergeStacks", String.valueOf(this.shulkerMergeStacks), true);
        appendValue(builder, 2, "showTitle", String.valueOf(this.shulkerShowTitle), false);
        appendObjectEnd(builder, 1, false);

        builder.append("}\n");
        return builder.toString();
    }

    private static void appendValue(
        final StringBuilder builder,
        final int level,
        final String key,
        final String value,
        final boolean comma
    ) {
        builder.append(indent(level)).append(quote(key)).append(": ").append(value);
        builder.append(comma ? ",\n" : "\n");
    }

    private static void appendObjectStart(final StringBuilder builder, final int level, final String key) {
        builder.append(indent(level)).append(quote(key)).append(": {\n");
    }

    private static void appendObjectEnd(final StringBuilder builder, final int level, final boolean comma) {
        builder.append(indent(level)).append(comma ? "},\n" : "}\n");
    }

    private static String inlineArray(final List<String> values) {
        return values.isEmpty() ? "[]" : "[" + String.join(", ", values) + "]";
    }

    private static List<String> quoted(final List<String> values) {
        final List<String> result = new ArrayList<>(values.size());
        for (final String value : values) {
            result.add(quote(value));
        }

        return result;
    }

    private static String indent(final int level) {
        return "  ".repeat(level);
    }

    private static String number(final double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1.0E9) {
            return String.format(Locale.ROOT, "%.1f", value);
        }

        return String.valueOf(value);
    }

    private static String quote(final String value) {
        final StringBuilder builder = new StringBuilder(value.length() + 2);
        builder.append('"');

        for (int i = 0; i < value.length(); i++) {
            final char character = value.charAt(i);
            switch (character) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (character < 0x20) {
                        builder.append(String.format(Locale.ROOT, "\\u%04x", (int) character));
                    } else {
                        builder.append(character);
                    }
                }
            }
        }

        return builder.append('"').toString();
    }

    public static ItemTagsConfig read(final JsonObject root) {
        final Filter filter = Filter.parse(getString(root, "filter"), DEFAULT.filter);
        final List<String> itemIds = getStringList(root, "items", DEFAULT.itemIds);
        final int backgroundColor = getColor(root, "backgroundColor", DEFAULT.backgroundColor);
        final Vec3 renderOffset = getVec3(root, "renderOffset", DEFAULT.renderOffset);
        final int rowLength = getInt(root, "rowLength", DEFAULT.rowLength);
        final boolean preventOverlap = getBoolean(root, "preventOverlap", DEFAULT.preventOverlap);
        final MergeMode mergeMode = MergeMode.parse(getString(root, "mergeMode"), DEFAULT.mergeMode);
        final int maxDistance = getInt(root, "maxDistance", DEFAULT.maxDistance);
        final float scale = getFloat(root, "scale", DEFAULT.scale);
        final float distanceFalloff = getFloat(root, "distanceFalloff", DEFAULT.distanceFalloff);

        final JsonObject shulker = root.has("shulker") && root.get("shulker").isJsonObject()
            ? root.getAsJsonObject("shulker")
            : null;
        final boolean shulkerEnabled = shulker != null
            ? getBoolean(shulker, "enabled", DEFAULT.shulkerEnabled)
            : DEFAULT.shulkerEnabled;
        final boolean shulkerMergeStacks = shulker != null
            ? getBoolean(shulker, "mergeStacks", DEFAULT.shulkerMergeStacks)
            : DEFAULT.shulkerMergeStacks;
        final boolean shulkerShowTitle = shulker != null
            ? getBoolean(shulker, "showTitle", DEFAULT.shulkerShowTitle)
            : DEFAULT.shulkerShowTitle;

        return new ItemTagsConfig(
            getBoolean(root, "enabled", DEFAULT.enabled),
            filter,
            itemIds,
            backgroundColor,
            renderOffset,
            rowLength,
            preventOverlap,
            mergeMode,
            maxDistance,
            scale,
            distanceFalloff,
            shulkerEnabled,
            shulkerMergeStacks,
            shulkerShowTitle
        );
    }

    private static Set<Item> resolveItems(final List<String> itemIds) {
        if (itemIds.isEmpty()) {
            return Set.of();
        }

        final Set<Item> items = new LinkedHashSet<>();
        for (final String rawId : itemIds) {
            final Item item = resolveItem(rawId);
            if (item != null) {
                items.add(item);
            }
        }

        return Collections.unmodifiableSet(items);
    }

    private static Item resolveItem(final String rawId) {
        try {
            final Identifier identifier = rawId.indexOf(':') == -1
                ? Identifier.withDefaultNamespace(rawId)
                : Identifier.parse(rawId);

            return BuiltInRegistries.ITEM.getOptional(identifier).orElse(null);
        } catch (final Exception exception) {
            DropCount.LOGGER.warn("[drop-count] Unknown item id in ItemTags config: {}", rawId);
            return null;
        }
    }

    private static Vec3 getVec3(final JsonObject root, final String key, final Vec3 fallback) {
        if (!root.has(key) || !root.get(key).isJsonArray()) {
            return fallback;
        }

        final JsonArray array = root.getAsJsonArray(key);
        if (array.size() < 3) {
            return fallback;
        }

        return new Vec3(
            getNumber(array.get(0), (float) fallback.x),
            getNumber(array.get(1), (float) fallback.y),
            getNumber(array.get(2), (float) fallback.z)
        );
    }

    private static List<String> getStringList(final JsonObject root, final String key, final List<String> fallback) {
        if (!root.has(key) || !root.get(key).isJsonArray()) {
            return fallback;
        }

        final List<String> result = new ArrayList<>();
        for (final JsonElement element : root.getAsJsonArray(key)) {
            if (element.isJsonPrimitive()) {
                result.add(element.getAsString());
            }
        }

        return result;
    }

    private static int getColor(final JsonObject root, final String key, final int fallback) {
        if (!root.has(key)) {
            return fallback;
        }

        final JsonElement element = root.get(key);
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return element.getAsInt();
        }

        final String raw = element.getAsString();
        try {
            if (raw.startsWith("#")) {
                final String hex = raw.substring(1);
                if (hex.length() == 6) {
                    return 0xFF000000 | Integer.parseInt(hex, 16);
                }

                return (int) Long.parseLong(hex, 16);
            }

            return (int) Long.parseLong(raw, 16);
        } catch (final NumberFormatException exception) {
            DropCount.LOGGER.warn("[drop-count] Invalid color '{}' for '{}', using default", raw, key);
            return fallback;
        }
    }

    private static String toHexColor(final int argb) {
        return "#" + String.format("%08X", argb);
    }

    private static boolean getBoolean(final JsonObject root, final String key, final boolean fallback) {
        final JsonElement element = root.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }

        if (element.getAsJsonPrimitive().isBoolean()) {
            return element.getAsBoolean();
        }

        if (element.getAsJsonPrimitive().isNumber()) {
            return element.getAsInt() != 0;
        }

        return Boolean.parseBoolean(element.getAsString());
    }

    private static int getInt(final JsonObject root, final String key, final int fallback) {
        final JsonElement element = root.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            return fallback;
        }

        return element.getAsInt();
    }

    private static float getFloat(final JsonObject root, final String key, final float fallback) {
        return getNumber(root.get(key), fallback);
    }

    private static String getString(final JsonObject root, final String key) {
        final JsonElement element = root.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }

    private static float getNumber(final JsonElement element, final float fallback) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            return fallback;
        }

        return element.getAsFloat();
    }

    public enum Filter {
        BLACKLIST("blacklist"),
        WHITELIST("whitelist");

        private final String jsonName;

        Filter(final String jsonName) {
            this.jsonName = jsonName;
        }

        public String jsonName() {
            return this.jsonName;
        }

        public boolean allows(final Item item, final Set<Item> items) {
            return this == BLACKLIST ? !items.contains(item) : items.contains(item);
        }

        public static Filter parse(final String value, final Filter fallback) {
            if (value == null) {
                return fallback;
            }

            final String normalized = value.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
            for (final Filter filter : values()) {
                if (filter.jsonName.replace("_", "").equals(normalized)) {
                    return filter;
                }
            }

            return fallback;
        }
    }

    public enum MergeMode {
        NONE("none"),
        BY_ITEM("by_item"),
        BY_COMPONENTS("by_components");

        private final String jsonName;

        MergeMode(final String jsonName) {
            this.jsonName = jsonName;
        }

        public String jsonName() {
            return this.jsonName;
        }

        public static MergeMode parse(final String value, final MergeMode fallback) {
            if (value == null) {
                return fallback;
            }

            final String normalized = value.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
            for (final MergeMode mode : values()) {
                if (mode.jsonName.replace("_", "").equals(normalized)) {
                    return mode;
                }
            }

            return fallback;
        }
    }
}
