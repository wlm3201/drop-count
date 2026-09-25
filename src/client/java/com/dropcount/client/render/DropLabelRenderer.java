package com.dropcount.client.render;

import com.dropcount.client.DropCountConfig;
import com.dropcount.client.compat.Platform;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws "name x<count>" labels above dropped items.
 *
 * <p>The label is submitted in world space the same way vanilla name tags are (see
 * {@code NameTagFeatureRenderer}), so it scales with distance like a name tag, is centred above the
 * drop, and is drawn twice: once ignoring depth so it stays readable behind terrain, once opaque on
 * top with a per-glyph outline.
 */
public final class DropLabelRenderer {
    /** Fullbright light coords; the label is information, not scenery. */
    private static final int FULL_BRIGHT = 15728880;
    private static final int BASE_COLOR = 0xFFFFFFFF;
    /** count colour. */
    private static final int COUNT_COLOR = 0xFF00FFFF;
    /** Opaque black. A value of 0 would be read as "no outline" by the text renderer. */
    private static final int OUTLINE_COLOR = 0xFF000000;

    /** Vanilla name-tag scale: one glyph pixel is 0.025 blocks. */
    private static final float TEXT_SCALE = 0.015F;
    /** Font line height in glyph pixels; also the height a label occupies in text space. */
    private static final float LABEL_HEIGHT = 9.0F;
    /** Distance between two stacked labels, in glyph pixels. */
    private static final float LINE_STEP = 9.0F;

    /** Labels are collected and drawn within this radius of the camera. */
    private static final double RANGE = 16.0;
    /** How far a drop's box is grown horizontally when looking for twins to merge with. */
    private static final double MERGE_EXPAND = 0.5;
    /** How far above the top of the drop(s) the label floats. */
    private static final double LABEL_LIFT = 0.35;

    private DropLabelRenderer() {
    }

    public static void render(PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!DropCountConfig.enabled()) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;

        Vec3 cam = camera.pos;
        double rangeSq = RANGE * RANGE;

        List<Drop> drops = new ArrayList<>();
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        AABB area = new AABB(cam.x - RANGE, cam.y - RANGE, cam.z - RANGE, cam.x + RANGE, cam.y + RANGE, cam.z + RANGE);
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, area, e -> !e.getItem().isEmpty())) {
            Vec3 pos = entity.getPosition(partialTick);
            if (pos.distanceToSqr(cam) > rangeSq) continue;
            if (!camera.cullFrustum.isVisible(entity.getBoundingBox())) continue;
            drops.add(new Drop(entity.getItem(), pos, entity.getBoundingBox(),
                pos.y + entity.getBbHeight(), pos.distanceToSqr(cam)));
        }
        if (drops.isEmpty()) return;

        List<Label> labels = new ArrayList<>();
        for (List<Drop> bucket : groupByItem(drops)) {
            for (List<Drop> cluster : groupByBox(bucket)) {
                labels.add(Label.of(cluster, mc.font));
            }
        }

        // Nearest first: the closest drop keeps the anchor position and the rest stack above it.
        labels.sort(Comparator.comparingDouble(Label::distanceToCameraSq));

        int[] slot = new int[labels.size()];
        for (int i = 0; i < labels.size(); i++) {
            int candidate = 0;
            boolean moved = true;
            while (moved) {
                moved = false;
                for (int j = 0; j < i; j++) {
                    if (slot[j] + 1 > candidate && overlaps(labels.get(i), candidate, labels.get(j), slot[j])) {
                        candidate = slot[j] + 1;
                        moved = true;
                    }
                }
            }
            slot[i] = candidate;
        }

        for (int i = 0; i < labels.size(); i++) {
            Label label = labels.get(i);
            poseStack.pushPose();
            poseStack.translate(label.anchor().x - cam.x, label.anchor().y - cam.y, label.anchor().z - cam.z);
            Platform.faceCamera(poseStack, camera);
            poseStack.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

            FormattedCharSequence ordered = label.text().getVisualOrderText();
            float x = -label.width() / 2.0F;
            // Text-space +y is down, so a higher slot goes up.
            float y = -slot[i] * LINE_STEP;

            // See-through pass: no outline here, because an outline makes the text renderer drop
            // the see-through flag, and this pass is the only reason a label survives behind a wall.
            collector.submitText(poseStack, x, y, ordered, false, Font.DisplayMode.SEE_THROUGH,
                FULL_BRIGHT, BASE_COLOR, 0, 0);
            // Opaque pass: background 0 (no box), black outline instead.
            collector.submitText(poseStack, x, y, ordered, false, Font.DisplayMode.NORMAL,
                FULL_BRIGHT, BASE_COLOR, 0, OUTLINE_COLOR);

            poseStack.popPose();
        }
    }

    /** Buckets drops by item id; only drops of the same item can ever share a label. */
    private static Collection<List<Drop>> groupByItem(List<Drop> drops) {
        Map<Integer, List<Drop>> byItem = new HashMap<>();
        for (Drop drop : drops) {
            byItem.computeIfAbsent(Item.getId(drop.stack().getItem()), id -> new ArrayList<>()).add(drop);
        }
        return byItem.values();
    }

    /**
     * Merges drops whose boxes touch once grown by {@value #MERGE_EXPAND} horizontally. Two stacks
     * only stay separate entities when their combined count exceeds one stack, so those are exactly
     * the pairs that should read as one amount.
     */
    private static Collection<List<Drop>> groupByBox(List<Drop> drops) {
        int size = drops.size();
        int[] parent = new int[size];
        for (int i = 0; i < size; i++) {
            parent[i] = i;
        }

        for (int i = 0; i < size; i++) {
            AABB grown = drops.get(i).box().inflate(MERGE_EXPAND, 0.0, MERGE_EXPAND);
            for (int j = i + 1; j < size; j++) {
                if (grown.intersects(drops.get(j).box())) {
                    union(parent, i, j);
                }
            }
        }

        Map<Integer, List<Drop>> clusters = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            clusters.computeIfAbsent(find(parent, i), root -> new ArrayList<>()).add(drops.get(i));
        }
        return clusters.values();
    }

    private static void union(int[] parent, int a, int b) {
        int rootA = find(parent, a);
        int rootB = find(parent, b);
        if (rootA != rootB) {
            parent[rootB] = rootA;
        }
    }

    private static int find(int[] parent, int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }

    /**
     * Would these two labels collide on screen? Both the label size and the gap are in blocks, and
     * both shrink by the same factor with distance, so the answer is distance independent and can be
     * measured straight in world space.
     */
    private static boolean overlaps(Label a, int slotA, Label b, int slotB) {
        // A bigger slot sits higher on screen, which is the same as being higher in the world here.
        double aY = a.anchor().y + slotA * TEXT_SCALE * LINE_STEP;
        double bY = b.anchor().y + slotB * TEXT_SCALE * LINE_STEP;
        if (Math.abs(aY - bY) >= TEXT_SCALE * LABEL_HEIGHT) return false;

        double dx = a.anchor().x - b.anchor().x;
        double dz = a.anchor().z - b.anchor().z;
        // Horizontal distance is used whole for the sideways gap: which way the camera faces decides
        // how much of it lands on the screen's x axis, and the whole is the worst case.
        return Math.sqrt(dx * dx + dz * dz) < a.halfWidth() + b.halfWidth();
    }

    /**
     * @param box   the real collision box, for the merge test
     * @param topY  top of the drop at the interpolated position -- the box itself only moves once per
     *              tick, and anchoring to it makes the label stutter next to a smoothly drawn item
     */
    private record Drop(ItemStack stack, Vec3 position, AABB box, double topY, double distanceToCameraSq) {
    }

    private record Label(Vec3 anchor, double distanceToCameraSq, Component text, float width, float halfWidth) {
        static Label of(List<Drop> cluster, Font font) {
            int count = 0;
            double x = 0.0;
            double z = 0.0;
            double top = Double.NEGATIVE_INFINITY;
            double nearest = Double.MAX_VALUE;

            for (Drop drop : cluster) {
                count += drop.stack().getCount();
                x += drop.position().x;
                z += drop.position().z;
                top = Math.max(top, drop.topY());
                nearest = Math.min(nearest, drop.distanceToCameraSq());
            }

            int size = cluster.size();
            // Between the drops horizontally, clear of the tallest one vertically.
            Vec3 anchor = new Vec3(x / size, top + LABEL_LIFT, z / size);

            MutableComponent text = Component.empty()
                .append(cluster.get(0).stack().getHoverName())
                .append(Component.literal(" x" + count).withColor(COUNT_COLOR));

            float width = font.width(text);
            return new Label(anchor, nearest, text, width, width * TEXT_SCALE / 2.0F);
        }
    }
}
