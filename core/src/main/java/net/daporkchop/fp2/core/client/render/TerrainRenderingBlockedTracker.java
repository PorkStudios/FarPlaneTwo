/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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

package net.daporkchop.fp2.core.client.render;

import lombok.Getter;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.core.client.FP2Client;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.buffer.GLMutableBuffer;
import net.daporkchop.lib.common.math.BinMath;
import net.daporkchop.lib.unsafe.PUnsafe;

import static net.daporkchop.fp2.core.util.math.MathUtil.*;

/**
 * @author DaPorkchop_
 */
public abstract class TerrainRenderingBlockedTracker implements AutoCloseable {
    protected static final long HEADERS_OFFSET = 0L;
    protected static final long FLAGS_OFFSET = HEADERS_OFFSET + 2L * (4 * Integer.BYTES);

    protected static final int FACE_COUNT = 6;

    protected static final int[] FACE_OFFSETS = {
            0, -1, 0, //DOWN
            0, 1, 0, //UP
            0, 0, -1, //NORTH
            0, 0, 1, //SOUTH
            -1, 0, 0, //WEST
            1, 0, 0, //EAST
    };

    protected static final int SHIFT_BAKED = 0;
    protected static final int SIZE_BAKED = 1;
    protected static final long FLAG_BAKED = 1L << SHIFT_BAKED;

    protected static final int SHIFT_RENDERABLE = SHIFT_BAKED + SIZE_BAKED;
    protected static final int SIZE_RENDERABLE = 1;
    protected static final long FLAG_RENDERABLE = 1L << SHIFT_RENDERABLE;

    protected static final int SHIFT_SELECTED = SHIFT_RENDERABLE + SIZE_RENDERABLE;
    protected static final int SIZE_SELECTED = 1;
    protected static final long FLAG_SELECTED = 1L << SHIFT_SELECTED;

    protected static final int SHIFT_INFRUSTUM = SHIFT_SELECTED + SIZE_SELECTED;
    protected static final int SIZE_INFRUSTUM = 1;
    protected static final long FLAG_INFRUSTUM = 1L << SHIFT_INFRUSTUM;

    protected static final int SHIFT_VISIBILITY = SHIFT_INFRUSTUM + SIZE_INFRUSTUM;
    protected static final int SIZE_VISIBILITY = sq(FACE_COUNT);

    protected static final int SHIFT_RENDER_DIRECTIONS = SHIFT_VISIBILITY + SIZE_VISIBILITY;
    protected static final int SIZE_RENDER_DIRECTIONS = FACE_COUNT;

    protected static final int SHIFT_INFACE = SHIFT_RENDER_DIRECTIONS + SIZE_RENDER_DIRECTIONS;
    protected static final int SIZE_INFACE = BinMath.getNumBitsNeededFor((FACE_COUNT + 1) - 1);

    //visibilityFlags()

    protected static boolean isVisible(long flags, int inFace, int outFace, int chunkX, int chunkY, int chunkZ, int minChunkX, int maxChunkX, int minChunkY, int maxChunkY, int minChunkZ, int maxChunkZ) {
        //if the neighbor would be outside the renderable area, it isn't visible even if the flags indicate that it would be
        if (chunkX + FACE_OFFSETS[outFace * 3 + 0] < minChunkX || chunkX + FACE_OFFSETS[outFace * 3 + 0] >= maxChunkX
                || chunkY + FACE_OFFSETS[outFace * 3 + 1] < minChunkY || chunkY + FACE_OFFSETS[outFace * 3 + 1] >= maxChunkY
                || chunkZ + FACE_OFFSETS[outFace * 3 + 2] < minChunkZ || chunkZ + FACE_OFFSETS[outFace * 3 + 2] >= maxChunkZ) {
            return false;
        }

        return inFace < -100 //if inFace is invalid (i.e. sourceDirection was null), all directions are visible
                || (flags & ((1L << SHIFT_VISIBILITY) << (outFace * FACE_COUNT + inFace))) != 0L;
    }

    //renderDirectionFlags()

    protected static boolean hasRenderDirection(long flags, int face) {
        return (flags & ((1L << SHIFT_RENDER_DIRECTIONS) << face)) != 0L;
    }

    //inFaceFlags()

    protected static int getInFace(long flags) {
        return (((int) (flags >>> SHIFT_INFACE)) & ((1 << SIZE_INFACE) - 1)) - 1;
    }

    protected static int getOppositeFace(int face) {
        return face ^ 1;
    }

    protected final DirectMemoryAllocator alloc = new DirectMemoryAllocator();

    protected final OpenGL gl;
    @Getter
    protected final GLMutableBuffer glBuffer;

    protected int offsetX;
    protected int offsetY;
    protected int offsetZ;
    protected int sizeX;
    protected int sizeY;
    protected int sizeZ;

    protected long sizeBytes;
    protected long addr;

    public TerrainRenderingBlockedTracker(FP2Client client) {
        this.gl = client.gl();
        this.glBuffer = GLMutableBuffer.create(this.gl);
    }

    @Override
    public void close() {
        this.glBuffer.close();

        this.alloc.free(this.addr);
        this.alloc.close();
    }

    //TODO: document whatever the fuck this is, lmao
    protected static void transformFlags(int scanMinX, int scanMaxX, int scanMinY, int scanMaxY, int scanMinZ, int scanMaxZ,
                                         int minChunkX, int maxChunkX, int minChunkY, int maxChunkY, int minChunkZ, int maxChunkZ,
                                         int factorChunkX, int factorChunkY, int factorChunkZ,
                                         int offsetChunkX, int offsetChunkY, int offsetChunkZ,
                                         int[] faceOffsets,
                                         long[] srcFlags,
                                         long addr) {
        for (int x = scanMinX; x < scanMaxX; x++) {
            for (int y = scanMinY; y < scanMaxY; y++) {
                for (int z = scanMinZ; z < scanMaxZ; z++) {
                    int idx = (x * factorChunkY + y) * factorChunkZ + z;
                    long centerFlags = srcFlags[idx];

                    boolean out = false;

                    //if a RenderChunk is baked, renderable and selected, it's a possible candidate for blocking fp2 terrain
                    if ((centerFlags & (FLAG_BAKED | FLAG_RENDERABLE | FLAG_SELECTED)) == (FLAG_BAKED | FLAG_RENDERABLE | FLAG_SELECTED)) {
                        int inFace = getInFace(centerFlags);

                        out = true;

                        //scan neighbors to make sure none of them are un-selectable
                        for (int outFace = 0; outFace < FACE_COUNT; outFace++) {
                            //this direction is the opposite of one of the directions that was traversed to get to this RenderChunk, skip it
                            if (hasRenderDirection(centerFlags, getOppositeFace(outFace))) {
                                continue;
                            }

                            //the neighboring RenderChunk isn't visible from the direction that the current RenderChunk was entered from, skip it
                            if (!isVisible(centerFlags, getOppositeFace(inFace), outFace, x - offsetChunkX, y - offsetChunkY, z - offsetChunkZ, minChunkX, maxChunkX, minChunkY, maxChunkY, minChunkZ, maxChunkZ)) {
                                continue;
                            }

                            long neighborFlags = srcFlags[idx + faceOffsets[outFace]];

                            //if the neighboring RenderChunk is renderable but neither baked nor selected, it means that the tile would be a valid neighbor except it hasn't yet been baked
                            //  because it's never passed the frustum check. skip these to avoid fp2 terrain drawing over vanilla along the edges of the screen when first loading a world.
                            if ((neighborFlags & (FLAG_BAKED | FLAG_RENDERABLE | FLAG_SELECTED | FLAG_INFRUSTUM)) == (FLAG_RENDERABLE)) {
                                continue;
                            }

                            //if the neighboring RenderChunk isn't baked, the current RenderChunk has at least one neighbor which isn't rendered and is therefore unable to block fp2 rendering.
                            if ((neighborFlags & (FLAG_BAKED)) == 0) {
                                out = false;
                                break;
                            }
                        }
                    }

                    if (out) {
                        long wordAddr = addr + FLAGS_OFFSET + (idx >> 5 << 2);
                        PUnsafe.putInt(wordAddr, PUnsafe.getInt(wordAddr) | (1 << idx));
                    }
                }
            }
        }
    }

    /**
     * Checks whether or not the chunk section at the given coordinates.
     *
     * @param chunkX the chunk section's X coordinate
     * @param chunkY the chunk section's Y coordinate
     * @param chunkZ the chunk section's Z coordinate
     * @return whether or not vanilla terrain at the given chunk section would prevent us from rendering level-0 FP2 terrain
     */
    public final boolean renderingBlocked(int chunkX, int chunkY, int chunkZ) {
        int x = chunkX + this.offsetX;
        int y = chunkY + this.offsetY;
        int z = chunkZ + this.offsetZ;

        if (x < 0 || x >= this.sizeX || y < 0 || y >= this.sizeY || z < 0 || z >= this.sizeZ) {
            return false;
        }

        int idx = (x * this.sizeY + y) * this.sizeZ + z;
        //this is carefully crafted so that HotSpot C2 on OpenJDK 8 on x86_64 can optimize this into just three instructions
        // by taking advantage of complex addressing
        return (PUnsafe.getInt(this.addr + FLAGS_OFFSET + ((long) idx >> 5 << 2)) & (1 << idx)) != 0;
    }
}
