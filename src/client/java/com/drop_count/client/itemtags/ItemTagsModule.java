package com.drop_count.client.itemtags;

import com.drop_count.client.config.ItemTagsConfig;
import com.drop_count.client.config.ItemTagsConfigManager;
import com.drop_count.client.platform.Platform;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shows the tags of dropped items in the world, grouped the same way vanilla merges item entities
 * and scaled like a vanilla name tag.
 */
public final class ItemTagsModule {

    private static final Comparator<ItemStack> STACK_COMPARATOR =
        Comparator.comparingInt((ItemStack stack) -> stack.getCount()).reversed()
            .thenComparing(stack -> stack.getItem().getDescriptionId());

    /** Horizontal expansion of the hitbox used by {@code ItemEntity#mergeWithNeighbours}. */
    private static final double MERGE_EXPAND_XZ = 0.5;

    private static final float MIN_DISTANCE = 0.5F;
    private static final float MIN_SCALE = ItemTagsConfig.MIN_SCALE;
    private static final float MAX_SCALE = ItemTagsConfig.MAX_SCALE;

    private static final List<Cluster> clusters = new ArrayList<>();

    private static final Matrix4f projectionBuffer = new Matrix4f();
    private static final Vector4f transformBuffer = new Vector4f();

    private ItemTagsModule() {
    }

    /**
     * Flips the module state and shows the new state above the hotbar.
     */
    public static void toggle() {
        ItemTagsConfigManager.toggleEnabled();

        final Minecraft minecraft = Minecraft.getInstance();
        final boolean enabled = ItemTagsConfigManager.get().enabled;
        Platform.hooks().showOverlayMessage(
            minecraft,
            Component.translatable(enabled ? "drop-count.item_tags.enabled" : "drop-count.item_tags.disabled")
        );
    }

    /**
     * Recomputes the item entity clusters, called once per client tick.
     */
    public static void onClientTick(final Minecraft minecraft) {
        clusters.clear();

        final ItemTagsConfig config = ItemTagsConfigManager.get();
        if (!config.enabled || minecraft.level == null) {
            return;
        }

        final List<ItemEntity> entities = new ArrayList<>();
        for (final Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof ItemEntity itemEntity) || itemEntity.isRemoved()) {
                continue;
            }

            final ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty() || !config.allows(stack.getItem())) {
                continue;
            }

            entities.add(itemEntity);
        }

        if (entities.isEmpty()) {
            return;
        }

        final Vec3 cameraPosition = Platform.hooks().getCamera(minecraft).position();
        final double maxDistanceSquared = (double) config.maxDistance * (double) config.maxDistance;
        final List<ItemEntity> visible = new ArrayList<>();

        // Only item entities within the configured distance get a tag
        for (final ItemEntity entity : entities) {
            if (entity.position().distanceToSqr(cameraPosition) > maxDistanceSquared) {
                continue;
            }

            visible.add(entity);
        }

        if (visible.isEmpty()) {
            return;
        }

        // Grouping works like the vanilla item entity merging (ItemEntity#mergeWithNeighbours):
        // two entities belong to the same group when their hitboxes, expanded horizontally by
        // 0.5 blocks, intersect.
        final int size = visible.size();
        final int[] parents = new int[size];
        for (int i = 0; i < size; i++) {
            parents[i] = i;
        }

        for (int i = 0; i < size; i++) {
            final AABB box = visible.get(i).getBoundingBox().inflate(MERGE_EXPAND_XZ, 0.0, MERGE_EXPAND_XZ);

            for (int j = i + 1; j < size; j++) {
                if (box.intersects(visible.get(j).getBoundingBox())) {
                    union(parents, i, j);
                }
            }
        }

        final Map<Integer, List<ItemEntity>> groups = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            groups.computeIfAbsent(find(parents, i), key -> new ArrayList<>()).add(visible.get(i));
        }

        for (final List<ItemEntity> group : groups.values()) {
            final List<ItemStack> stacks = new ArrayList<>(group.size());
            for (final ItemEntity itemEntity : group) {
                stacks.add(itemEntity.getItem());
            }

            clusters.add(new Cluster(group, merge(config.mergeMode, stacks)));
        }
    }

    private static int find(final int[] parents, final int index) {
        int root = index;
        while (parents[root] != root) {
            root = parents[root];
        }

        parents[index] = root;
        return root;
    }

    private static void union(final int[] parents, final int first, final int second) {
        final int firstRoot = find(parents, first);
        final int secondRoot = find(parents, second);
        if (firstRoot != secondRoot) {
            parents[secondRoot] = firstRoot;
        }
    }

    /**
     * Draws the tags on top of the HUD.
     */
    public static void render(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
        final Minecraft minecraft = Minecraft.getInstance();
        final ItemTagsConfig config = ItemTagsConfigManager.get();

        if (!config.enabled || minecraft.level == null || clusters.isEmpty()) {
            return;
        }

        final float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(false);
        final Camera camera = Platform.hooks().getCamera(minecraft);
        final List<ItemTagsRenderer.Box> boxes = new ArrayList<>();

        for (final Cluster cluster : clusters) {
            final Vec3 worldPosition = cluster.center(tickDelta).add(config.renderOffset);
            if (worldPosition.distanceTo(camera.position()) > config.maxDistance) {
                continue;
            }

            final Vector3f screenPosition = project(worldPosition, camera);
            if (screenPosition == null) {
                continue;
            }

            // The perspective shrink depends on the depth along the view axis, not on the plain
            // distance, otherwise tags off to the side are scaled too small.
            final float scale = nameTagScale(minecraft, camera, screenPosition.z, config);
            boxes.add(ItemTagsRenderer.Box.of(cluster.stacks, null, screenPosition.x, screenPosition.y, scale));

            if (!config.shulkerEnabled) {
                continue;
            }

            for (final ItemStack stack : cluster.stacks) {
                final ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
                if (contents == null) {
                    continue;
                }

                final ItemStack[] inner = contents.nonEmptyItemCopyStream().toArray(ItemStack[]::new);
                if (inner.length == 0) {
                    continue;
                }

                final List<ItemStack> innerStacks = config.shulkerMergeStacks
                    ? merge(config.mergeMode, Arrays.asList(inner))
                    : Arrays.asList(inner);

                boxes.add(ItemTagsRenderer.Box.of(
                    innerStacks,
                    config.shulkerShowTitle ? stack.getHoverName() : null,
                    screenPosition.x,
                    screenPosition.y,
                    scale
                ));
            }
        }

        if (boxes.isEmpty()) {
            return;
        }

        for (final ItemTagsRenderer.Box box : boxes) {
            box.measure(minecraft.font, config.rowLength);
        }

        if (config.preventOverlap) {
            ItemTagsRenderer.rearrange(boxes);
        }

        graphics.nextStratum();
        for (final ItemTagsRenderer.Box box : boxes) {
            ItemTagsRenderer.draw(graphics, box, config);
        }
    }

    public static List<ItemStack> merge(final ItemTagsConfig.MergeMode mode, final List<ItemStack> stacks) {
        return switch (mode) {
            case NONE -> {
                final List<ItemStack> result = new ArrayList<>(stacks);
                result.sort(STACK_COMPARATOR);
                yield result;
            }
            case BY_ITEM -> {
                final Map<Item, List<ItemStack>> byItem = new HashMap<>();
                for (final ItemStack stack : stacks) {
                    byItem.computeIfAbsent(stack.getItem(), key -> new ArrayList<>()).add(stack);
                }

                final List<ItemStack> result = new ArrayList<>(byItem.size());
                for (final List<ItemStack> group : byItem.values()) {
                    if (group.size() == 1) {
                        result.add(group.get(0));
                        continue;
                    }

                    int count = 0;
                    for (final ItemStack stack : group) {
                        count += stack.getCount();
                    }

                    result.add(new ItemStack(group.get(0).getItem(), count));
                }

                result.sort(STACK_COMPARATOR);
                yield result;
            }
            case BY_COMPONENTS -> {
                final Map<ItemAndComponents, Integer> counts = new LinkedHashMap<>();
                for (final ItemStack stack : stacks) {
                    counts.merge(new ItemAndComponents(stack), stack.getCount(), Integer::sum);
                }

                final List<ItemStack> result = new ArrayList<>(counts.size());
                counts.forEach((key, count) -> result.add(key.toStack(count)));
                result.sort(STACK_COMPARATOR);
                yield result;
            }
        };
    }

    /**
     * Size of a vanilla name tag: one font pixel is {@link EntityRenderer#NAMETAG_SCALE} blocks
     * wide, so the tag keeps a constant size in the world and shrinks on screen with distance.
     * Because the tag is drawn in GUI space, that perspective shrink has to be reproduced here.
     * <p>
     * The shrink is {@code 1 / depth ^ falloff} with a reference distance of one block, so a falloff
     * of 1 is exactly vanilla. Lower values keep distant tags readable, 0 removes the shrink
     * entirely. The result is multiplied by {@code scale} and clamped.
     */
    private static float nameTagScale(
        final Minecraft minecraft,
        final Camera camera,
        final float distance,
        final ItemTagsConfig config
    ) {
        final float guiHeight = minecraft.getWindow().getGuiScaledHeight();
        final float halfFov = (float) Math.tan(Math.toRadians(camera.getFov()) * 0.5);
        final float depth = (float) Math.pow(Math.max(distance, MIN_DISTANCE), config.distanceFalloff);
        final float pixelsPerBlock = guiHeight / (2.0F * depth * halfFov);

        return Mth.clamp(EntityRenderer.NAMETAG_SCALE * config.scale * pixelsPerBlock, MIN_SCALE, MAX_SCALE);
    }

    /**
     * Projects a world position to GUI pixels, the z component is the depth along the view axis.
     */
    private static Vector3f project(final Vec3 position, final Camera camera) {
        final Minecraft minecraft = Minecraft.getInstance();
        final Matrix4f viewProjection = camera.getViewRotationProjectionMatrix(projectionBuffer);
        final Vec3 cameraPosition = camera.position();

        transformBuffer.set(
            (float) (position.x - cameraPosition.x),
            (float) (position.y - cameraPosition.y),
            (float) (position.z - cameraPosition.z),
            1.0F
        ).mul(viewProjection);

        if (transformBuffer.w <= 0.0F) {
            return null;
        }

        final float ndcX = transformBuffer.x / transformBuffer.w;
        final float ndcY = transformBuffer.y / transformBuffer.w;

        return new Vector3f(
            (ndcX + 1.0F) * 0.5F * minecraft.getWindow().getGuiScaledWidth(),
            (1.0F - ndcY) * 0.5F * minecraft.getWindow().getGuiScaledHeight(),
            transformBuffer.w
        );
    }

    private record ItemAndComponents(Item item, DataComponentPatch componentsPatch) {

        ItemAndComponents(final ItemStack stack) {
            this(stack.getItem(), stack.getComponentsPatch());
        }

        ItemStack toStack(final int count) {
            return new ItemStack(BuiltInRegistries.ITEM.wrapAsHolder(this.item), count, this.componentsPatch);
        }
    }

    private static final class Cluster {
        private final List<ItemEntity> entities;
        private final List<ItemStack> stacks;

        private Cluster(final List<ItemEntity> entities, final List<ItemStack> stacks) {
            this.entities = entities;
            this.stacks = stacks;
        }

        private Vec3 center(final float tickDelta) {
            double x = 0.0;
            double y = 0.0;
            double z = 0.0;

            for (final ItemEntity entity : this.entities) {
                final Vec3 position = entity.getPosition(tickDelta);
                x += position.x;
                y += position.y;
                z += position.z;
            }

            final int size = this.entities.size();
            return new Vec3(x / size, y / size, z / size);
        }
    }
}
