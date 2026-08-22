/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2026 DaPorkchop_
 * Portions Copyright (c) 2026 FarPlane contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package dev.farplane.engine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

import static dev.farplane.engine.EngineConstants.*;

/**
 * Bakes voxel tiles into renderable quad geometry.
 * Simplified version of FarPlaneTwo {@code VoxelBaker}.
 * <p>
 * For v1, this produces simple colored quads for each visible face.
 * The upstream baker uses custom GLSL shaders with atlas UVs — that's Phase 4.
 *
 * @author DaPorkchop_ (original algorithm)
 */
public class VoxelBaker {

    /**
     * A baked mesh ready for rendering.
     */
    public record BakedMesh(List<float[]> quads, int level) {
        public boolean isEmpty() { return quads.isEmpty(); }
    }

    /**
     * Bakes a tile and its 7 neighbors into a renderable mesh.
     * <p>
     * Neighbors are indexed as: self(0) + +X(1) + +Y(2) + +Z(3) octant neighbors.
     * For v1, we only use the primary tile (index 0) and skip neighbor stitching.
     *
     * @param tile  the primary tile
     * @param level the LoD level
     * @param pos   the tile position (for world-space offset)
     * @return a baked mesh
     */
    public BakedMesh bake(Tile tile, int level, TilePos pos) {
        if (tile == null || tile.isEmpty()) {
            return new BakedMesh(List.of(), level);
        }

        List<float[]> quads = new ArrayList<>();
        TileData data = new TileData();

        float scale = 1 << level; // world-space scale per voxel unit

        // World-space origin of this tile
        float baseX = pos.minBlockX();
        float baseY = pos.minBlockY();
        float baseZ = pos.minBlockZ();

        for (int i = 0; i < tile.count(); i++) {
            int cellPos = tile.get(i, data);
            if (cellPos < 0) continue;

            int cx = (cellPos >> (T_SHIFT << 1)) & T_MASK;
            int cy = (cellPos >> T_SHIFT) & T_MASK;
            int cz = cellPos & T_MASK;

            // World position of this voxel cell
            float vx = baseX + cx * scale;
            float vy = baseY + cy * scale;
            float vz = baseZ + cz * scale;

            // Dual-contour vertex position (fractional, scaled)
            float dcx = vx + (data.x / (float) POS_ONE) * scale;
            float dcy = vy + (data.y / (float) POS_ONE) * scale;
            float dcz = vz + (data.z / (float) POS_ONE) * scale;

            int edges = data.edges;
            // Fix Y-backwards quirk from FP2
            if ((((edges >> 2) ^ (edges >> 3)) & 1) != 0) {
                edges ^= EDGE_DIR_MASK << 2;
            }

            // Emit quads for each crossing edge
            for (int edge = 0; edge < EDGE_COUNT; edge++) {
                int edgeDir = (edges >> (edge << 1)) & EDGE_DIR_MASK;
                if (edgeDir == EDGE_DIR_NONE) continue;

                // Get the 4 connection vertices for this edge
                float[] verts = new float[12]; // 4 vertices × 3 components
                boolean valid = true;

                for (int ci = 0; ci < CONNECTION_INDEX_COUNT; ci++) {
                    int j = CONNECTION_INDICES[edge * CONNECTION_INDEX_COUNT + ci];
                    int ddx = cx + ((j >> 2) & 1);
                    int ddy = cy + ((j >> 1) & 1);
                    int ddz = cz + (j & 1);

                    // For v1, use the cell corner positions (blocky mesh)
                    // A full implementation would look up neighbor tile data for smooth vertices
                    verts[ci * 3 + 0] = baseX + ddx * scale;
                    verts[ci * 3 + 1] = baseY + ddy * scale;
                    verts[ci * 3 + 2] = baseZ + ddz * scale;
                }

                // Determine face color based on edge direction and block type
                float r, g, b;
                int stateId = data.states[edge];
                // Simple color: use biome tint for grass-like blocks, gray for stone
                // Full implementation needs texture atlas lookup (Phase 4)
                r = 0.4f; g = 0.7f; b = 0.3f; // default green

                // Emit the quad (two triangles)
                if ((edgeDir & EDGE_DIR_NEGATIVE) != 0) {
                    addQuad(quads, verts, r, g, b, true);
                }
                if ((edgeDir & EDGE_DIR_POSITIVE) != 0) {
                    addQuad(quads, verts, r, g, b, false);
                }
            }
        }

        return new BakedMesh(quads, level);
    }

    private void addQuad(List<float[]> quads, float[] verts, float r, float g, float b, boolean flip) {
        // Quad as two triangles: v0-v1-v2, v0-v2-v3
        // Each vertex: x, y, z, r, g, b, a = 7 floats
        float a = 1.0f;

        if (flip) {
            // Reverse winding
            quads.add(new float[]{
                    verts[0], verts[1], verts[2], r, g, b, a,
                    verts[6], verts[7], verts[8], r, g, b, a,
                    verts[3], verts[4], verts[5], r, g, b, a,

                    verts[0], verts[1], verts[2], r, g, b, a,
                    verts[9], verts[10], verts[11], r, g, b, a,
                    verts[6], verts[7], verts[8], r, g, b, a,
            });
        } else {
            quads.add(new float[]{
                    verts[0], verts[1], verts[2], r, g, b, a,
                    verts[3], verts[4], verts[5], r, g, b, a,
                    verts[6], verts[7], verts[8], r, g, b, a,

                    verts[0], verts[1], verts[2], r, g, b, a,
                    verts[6], verts[7], verts[8], r, g, b, a,
                    verts[9], verts[10], verts[11], r, g, b, a,
            });
        }
    }

    /**
     * Renders a baked mesh using the debug quads render type.
     */
    public static void renderMesh(BakedMesh mesh, PoseStack poseStack, MultiBufferSource bufferSource, double camX, double camY, double camZ) {
        if (mesh.isEmpty()) return;

        VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.debugQuads());
        Matrix4f matrix = poseStack.last().pose();

        for (float[] quad : mesh.quads()) {
            // 6 vertices per quad (2 triangles)
            for (int v = 0; v < 6; v++) {
                int off = v * 7;
                float x = quad[off] - (float) camX;
                float y = quad[off + 1] - (float) camY;
                float z = quad[off + 2] - (float) camZ;
                float r = quad[off + 3];
                float g = quad[off + 4];
                float b = quad[off + 5];
                float a = quad[off + 6];

                consumer.addVertex(matrix, x, y, z)
                        .setColor(r, g, b, a);
            }
        }
    }
}
