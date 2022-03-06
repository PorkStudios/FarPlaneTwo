/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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
 *
 */

package net.daporkchop.fp2.impl.mc.forge1_12_2.client;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.common.util.alloc.Allocator;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.core.client.render.TerrainRenderingBlockedTracker;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.cc.FP2CubicChunks;
import net.daporkchop.fp2.impl.mc.forge1_12_2.old.client.gl.object.GLBuffer;
import net.daporkchop.lib.common.math.BinMath;
import net.daporkchop.lib.common.math.PMath;
import net.daporkchop.lib.common.misc.refcount.AbstractRefCounted;
import net.daporkchop.lib.common.util.PorkUtil;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.daporkchop.lib.unsafe.util.exception.AlreadyReleasedException;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.stream.Stream;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.fp2.core.util.math.MathUtil.*;
import static net.daporkchop.fp2.impl.mc.forge1_12_2.compat.of.OFHelper.*;
import static net.daporkchop.lib.common.math.PMath.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class TerrainRenderingBlockedTracker1_12_2 extends AbstractRefCounted implements TerrainRenderingBlockedTracker {
    protected static final long HEADERS_OFFSET = 0L;
    protected static final long FLAGS_OFFSET = HEADERS_OFFSET + 2L * (4 * INT_SIZE);

    protected static final int FACE_COUNT = EnumFacing.VALUES.length;

    protected static final int SHIFT_BAKED = 0;
    protected static final int SIZE_BAKED = 1;
    protected static final long FLAG_BAKED = 1L << SHIFT_BAKED;

    protected static final int SHIFT_RENDERABLE = SHIFT_BAKED + SIZE_BAKED;
    protected static final int SIZE_RENDERABLE = 1;
    protected static final long FLAG_RENDERABLE = 1L << SHIFT_RENDERABLE;

    protected static final int SHIFT_SELECTED = SHIFT_RENDERABLE + SIZE_RENDERABLE;
    protected static final int SIZE_SELECTED = 1;
    protected static final long FLAG_SELECTED = 1L << SHIFT_SELECTED;

    protected static final int SHIFT_VISIBILITY = SHIFT_SELECTED + SIZE_SELECTED;
    protected static final int SIZE_VISIBILITY = sq(FACE_COUNT);

    protected static final int SHIFT_RENDER_DIRECTIONS = SHIFT_VISIBILITY + SIZE_VISIBILITY;
    protected static final int SIZE_RENDER_DIRECTIONS = FACE_COUNT;

    protected static final int SHIFT_INFACE = SHIFT_RENDER_DIRECTIONS + SIZE_RENDER_DIRECTIONS;
    protected static final int SIZE_INFACE = BinMath.getNumBitsNeededFor(FACE_COUNT - 1);

    protected static long visibilityFlags(CompiledChunk compiledChunk) {
        long flags = 0L;
        int shift = SHIFT_VISIBILITY;
        for (EnumFacing inFace : EnumFacing.VALUES) {
            for (EnumFacing outFace : EnumFacing.VALUES) {
                if (compiledChunk.isVisible(inFace, outFace)) {
                    flags |= 1L << shift;
                }
                shift++;
            }
        }
        return flags;
    }

    protected static boolean isVisible(long flags, int inFace, int outFace) {
        return (flags & ((1L << SHIFT_VISIBILITY) << (inFace * FACE_COUNT + outFace))) != 0L;
    }

    protected static long renderDirectionFlags(RenderGlobal.ContainerLocalRenderInformation containerLocalRenderInformation) {
        long flags = 0L;
        int shift = SHIFT_RENDER_DIRECTIONS;
        for (EnumFacing inFace : EnumFacing.VALUES) {
            if (containerLocalRenderInformation.hasDirection(inFace)) {
                flags |= 1L << shift;
            }
            shift++;
        }
        return flags;
    }

    protected static boolean hasRenderDirection(long flags, int face) {
        return (flags & ((1L << SHIFT_RENDER_DIRECTIONS) << face)) != 0L;
    }

    protected static long inFaceFlags(RenderGlobal.ContainerLocalRenderInformation containerLocalRenderInformation) {
        return ((long) PorkUtil.fallbackIfNull(containerLocalRenderInformation.facing, EnumFacing.DOWN).ordinal()) << SHIFT_INFACE;
    }

    protected static int getInFace(long flags) {
        return ((int) (flags >>> SHIFT_INFACE)) & ((1 << SIZE_INFACE) - 1);
    }

    protected static int getOppositeFace(int face) {
        return face ^ 1;
    }

    protected final Allocator alloc = new DirectMemoryAllocator();
    protected final GLBuffer glBuffer = new GLBuffer(GL_STREAM_DRAW);

    protected int offsetX;
    protected int offsetY;
    protected int offsetZ;
    protected int sizeX;
    protected int sizeY;
    protected int sizeZ;

    protected long sizeBytes;
    protected long addr;

    @Override
    public TerrainRenderingBlockedTracker1_12_2 retain() throws AlreadyReleasedException {
        super.retain();
        return this;
    }

    @Override
    protected void doRelease() {
        if (this.addr != 0L) {
            this.alloc.free(this.addr);
        }
        this.glBuffer.delete();
    }

    /**
     * Updates this tracker instance based on the state of the given {@link RenderGlobal}.
     */
    public void update(@NonNull RenderGlobal renderGlobal, int frameCount) {
        //figure out the maximum extents of all of the renderChunks in the current ViewFrustum
        ViewFrustum viewFrustum = renderGlobal.viewFrustum;
        boolean cubic = FP2CubicChunks.isCubicWorld(viewFrustum.world);
        int sizeX = viewFrustum.countChunksX + 2;
        int sizeY = viewFrustum.countChunksY + 2;
        int sizeZ = viewFrustum.countChunksZ + 2;

        int radiusX = (sizeX - 1) >> 1;
        int radiusY = (sizeY - 1) >> 1;
        int radiusZ = (sizeZ - 1) >> 1;

        int renderPosX = floorI(renderGlobal.frustumUpdatePosX) - 8;
        int renderPosY = floorI(renderGlobal.frustumUpdatePosY) - 8;
        int renderPosZ = floorI(renderGlobal.frustumUpdatePosZ) - 8;

        int minChunkX = (renderPosX >> 4) - radiusX;
        int maxChunkX = (renderPosX >> 4) + radiusX;
        int minChunkY;
        int maxChunkY;
        int minChunkZ = (renderPosZ >> 4) - radiusZ;
        int maxChunkZ = (renderPosZ >> 4) + radiusZ;

        if (cubic) {
            minChunkY = (renderPosY >> 4) - radiusY;
            maxChunkY = (renderPosY >> 4) + radiusY;
        } else {
            minChunkY = 0;
            maxChunkY = 16;
        }

        int offsetChunkX = -minChunkX + 1;
        int offsetChunkY = -minChunkY + 1;
        int offsetChunkZ = -minChunkZ + 1;
        int factorChunkX = maxChunkX - minChunkX + 3;
        int factorChunkY = maxChunkY - minChunkY + 3;
        int factorChunkZ = maxChunkZ - minChunkZ + 3;

        long[] srcFlags = new long[factorChunkX * factorChunkY * factorChunkZ];
        //iterate over all the ContainerLocalRenderInformation instances, because they indicate:
        // - which direction to enter a RenderChunk from
        // - whether or not a RenderChunk is actually selected to be rendered
        renderGlobal.renderInfos.forEach(containerLocalRenderInformation -> {
            long flags = 0L;
            flags |= renderDirectionFlags(containerLocalRenderInformation);
            flags |= inFaceFlags(containerLocalRenderInformation);

            flags |= FLAG_SELECTED;

            BlockPos pos = containerLocalRenderInformation.renderChunk.getPosition();
            int chunkX = pos.getX() >> 4;
            int chunkY = pos.getY() >> 4;
            int chunkZ = pos.getZ() >> 4;
            srcFlags[((chunkX + offsetChunkX) * factorChunkY + (chunkY + offsetChunkY)) * factorChunkZ + (chunkZ + offsetChunkZ)] = flags;
        });

        //iterate over all the RenderChunks, because they indicate:
        // - whether or not a RenderChunk's frame index is up-to-date, which can be used to determine whether a RenderChunk would have been selected but failed the frustum check
        // - whether or not a RenderChunk has been baked
        // - the RenderChunk's neighbor visibility graph
        Stream.of(viewFrustum.renderChunks).forEach(renderChunk -> {
            long flags = 0L;

            if (renderChunk.frameIndex == frameCount) {
                flags |= FLAG_RENDERABLE;
            }

            CompiledChunk compiledChunk = renderChunk.getCompiledChunk();
            if (compiledChunk != CompiledChunk.DUMMY) {
                flags |= FLAG_BAKED;
                flags |= visibilityFlags(compiledChunk);
            }

            BlockPos pos = renderChunk.getPosition();
            int chunkX = pos.getX() >> 4;
            int chunkY = pos.getY() >> 4;
            int chunkZ = pos.getZ() >> 4;
            srcFlags[((chunkX + offsetChunkX) * factorChunkY + (chunkY + offsetChunkY)) * factorChunkZ + (chunkZ + offsetChunkZ)] |= flags;
        });

        //iterate over the whole grid again, setting the flag to indicate fp2 rendering is blocked by the RenderChunk at the given position if
        //  the following applies:
        //  - the RenderChunk has been compiled
        //  - all of the RenderChunk's visible neighbors have been compiled

        int offset0 = (EnumFacing.DOWN.getXOffset() * factorChunkY + EnumFacing.DOWN.getYOffset()) * factorChunkZ + EnumFacing.DOWN.getZOffset();
        int offset1 = (EnumFacing.UP.getXOffset() * factorChunkY + EnumFacing.UP.getYOffset()) * factorChunkZ + EnumFacing.UP.getZOffset();
        int offset2 = (EnumFacing.NORTH.getXOffset() * factorChunkY + EnumFacing.NORTH.getYOffset()) * factorChunkZ + EnumFacing.NORTH.getZOffset();
        int offset3 = (EnumFacing.SOUTH.getXOffset() * factorChunkY + EnumFacing.SOUTH.getYOffset()) * factorChunkZ + EnumFacing.SOUTH.getZOffset();
        int offset4 = (EnumFacing.WEST.getXOffset() * factorChunkY + EnumFacing.WEST.getYOffset()) * factorChunkZ + EnumFacing.WEST.getZOffset();
        int offset5 = (EnumFacing.EAST.getXOffset() * factorChunkY + EnumFacing.EAST.getYOffset()) * factorChunkZ + EnumFacing.EAST.getZOffset();
        int[] offsets = { offset0, offset1, offset2, offset3, offset4, offset5 };

        long sizeBits = factorChunkX * factorChunkY * factorChunkZ;
        long sizeBytes = FLAGS_OFFSET + (PMath.roundUp(sizeBits, 32) >> 2);

        long addr = this.alloc.alloc(sizeBytes);
        PUnsafe.setMemory(addr, sizeBytes, (byte) 0);

        //optifine is kinda weird and i can't be bothered at this point to figure out what the difference is. my shitty workaround is
        //  just to increase the overlap radius :P
        final int PADDING_RADIUS = OF ? 3 : 2;
        for (int x = 1 + PADDING_RADIUS; x < factorChunkX - (2 + PADDING_RADIUS); x++) {
            for (int y = 1 + PADDING_RADIUS; y < factorChunkY - (2 + PADDING_RADIUS); y++) {
                for (int z = 1 + PADDING_RADIUS; z < factorChunkZ - (2 + PADDING_RADIUS); z++) {
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
                            if (!isVisible(centerFlags, getOppositeFace(inFace), outFace)) {
                                continue;
                            }

                            long neighborFlags = srcFlags[idx + offsets[outFace]];

                            //if the neighboring RenderChunk is renderable but neither baked nor selected, it means that the tile would be a valid neighbor except it hasn't yet been baked
                            //  because it's never passed the frustum check. skip these to avoid fp2 terrain drawing over vanilla along the edges of the screen when first loading a world.
                            if ((neighborFlags & (FLAG_BAKED | FLAG_RENDERABLE | FLAG_SELECTED)) == (FLAG_RENDERABLE)) {
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

        if (this.addr != 0L) {
            this.alloc.free(this.addr);
        }

        {
            long headersAddr = addr + HEADERS_OFFSET;

            //ivec3 offset
            PUnsafe.putInt(headersAddr + 0 * INT_SIZE, offsetChunkX);
            PUnsafe.putInt(headersAddr + 1 * INT_SIZE, offsetChunkY);
            PUnsafe.putInt(headersAddr + 2 * INT_SIZE, offsetChunkZ);
            headersAddr += 4 * INT_SIZE;

            //ivec3 size
            PUnsafe.putInt(headersAddr + 0 * INT_SIZE, factorChunkX);
            PUnsafe.putInt(headersAddr + 1 * INT_SIZE, factorChunkY);
            PUnsafe.putInt(headersAddr + 2 * INT_SIZE, factorChunkZ);
            headersAddr += 4 * INT_SIZE;
        }

        this.offsetX = offsetChunkX;
        this.offsetY = offsetChunkY;
        this.offsetZ = offsetChunkZ;
        this.sizeX = factorChunkX;
        this.sizeY = factorChunkY;
        this.sizeZ = factorChunkZ;
        this.sizeBytes = sizeBytes;
        this.addr = addr;

        try (GLBuffer buffer = this.glBuffer.bind(GL_SHADER_STORAGE_BUFFER)) {
            buffer.upload(addr, sizeBytes);
        }
        this.glBuffer.bindBase(GL_SHADER_STORAGE_BUFFER, 7);
    }

    @Override
    public boolean renderingBlocked(int chunkX, int chunkY, int chunkZ) {
        int x = chunkX + this.offsetX;
        int y = chunkY + this.offsetY;
        int z = chunkZ + this.offsetZ;

        if (x < 0 || x >= this.sizeX || y < 0 || y >= this.sizeY || z < 0 || z >= this.sizeZ) {
            return false;
        }

        int idx = (x * this.sizeY + y) * this.sizeZ + z;
        return (PUnsafe.getInt(this.addr + FLAGS_OFFSET + (idx >> 5 << 2)) & (1 << idx)) != 0;
    }
}
