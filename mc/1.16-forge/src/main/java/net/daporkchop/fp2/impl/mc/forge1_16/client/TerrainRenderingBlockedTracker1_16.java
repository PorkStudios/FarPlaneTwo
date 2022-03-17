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

package net.daporkchop.fp2.impl.mc.forge1_16.client;

import lombok.NonNull;
import net.daporkchop.fp2.common.util.alloc.Allocator;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.core.client.render.TerrainRenderingBlockedTracker;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.at.client.renderer.ATViewFrustum1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.at.client.renderer.ATWorldRenderer1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.at.client.renderer.ATWorldRenderer__LocalRenderInformationContainer1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.at.client.renderer.chunk.ATChunkRenderDispatcher__ChunkRender1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.compat.of.OFHelper1_16;
import net.daporkchop.lib.common.math.BinMath;
import net.daporkchop.lib.common.math.PMath;
import net.daporkchop.lib.common.misc.refcount.AbstractRefCounted;
import net.daporkchop.lib.common.util.PorkUtil;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.daporkchop.lib.unsafe.util.exception.AlreadyReleasedException;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.fp2.core.util.math.MathUtil.*;
import static net.daporkchop.lib.common.math.PMath.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * @author DaPorkchop_
 */
public class TerrainRenderingBlockedTracker1_16 extends AbstractRefCounted implements TerrainRenderingBlockedTracker {
    protected static final long HEADERS_OFFSET = 0L;
    protected static final long FLAGS_OFFSET = HEADERS_OFFSET + 2L * (4 * INT_SIZE);

    protected static final Direction[] DIRECTIONS = Direction.values();
    protected static final int FACE_COUNT = DIRECTIONS.length;

    protected static final int[] FACE_OFFSETS = Stream.of(DIRECTIONS)
            .flatMapToInt(direction -> IntStream.of(direction.getStepX(), direction.getStepY(), direction.getStepZ()))
            .toArray();

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

    protected static long visibilityFlags(ChunkRenderDispatcher.CompiledChunk compiledChunk) {
        long flags = 0L;
        int shift = SHIFT_VISIBILITY;
        for (Direction outFace : DIRECTIONS) {
            for (Direction inFace : DIRECTIONS) {
                if (compiledChunk.facesCanSeeEachother(inFace, outFace)) {
                    flags |= 1L << shift;
                }
                shift++;
            }
        }
        return flags;
    }

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

    protected static long renderDirectionFlags(ATWorldRenderer__LocalRenderInformationContainer1_16 containerLocalRenderInformation) {
        long flags = 0L;
        int shift = SHIFT_RENDER_DIRECTIONS;
        for (Direction inFace : DIRECTIONS) {
            if (containerLocalRenderInformation.invokeHasDirection(inFace)) {
                flags |= 1L << shift;
            }
            shift++;
        }
        return flags;
    }

    protected static boolean hasRenderDirection(long flags, int face) {
        return (flags & ((1L << SHIFT_RENDER_DIRECTIONS) << face)) != 0L;
    }

    protected static long inFaceFlags(ATWorldRenderer__LocalRenderInformationContainer1_16 containerLocalRenderInformation) {
        Direction direction = containerLocalRenderInformation.getSourceDirection();
        if (direction != null) {
            return (direction.ordinal() + 1L) << SHIFT_INFACE;
        } else { //direction is null, meaning this is the origin chunk section
            return 0L; //return an invalid face index, which will become negative when decoded
        }
    }

    protected static int getInFace(long flags) {
        return (((int) (flags >>> SHIFT_INFACE)) & ((1 << SIZE_INFACE) - 1)) - 1;
    }

    protected static int getOppositeFace(int face) {
        return face ^ 1;
    }

    protected final Allocator alloc = new DirectMemoryAllocator();
    protected final int glBuffer = glGenBuffers();

    protected int offsetX;
    protected int offsetY;
    protected int offsetZ;
    protected int sizeX;
    protected int sizeY;
    protected int sizeZ;

    protected long sizeBytes;
    protected long addr;

    @Override
    public TerrainRenderingBlockedTracker1_16 retain() throws AlreadyReleasedException {
        super.retain();
        return this;
    }

    @Override
    protected void doRelease() {
        if (this.addr != 0L) {
            this.alloc.free(this.addr);
        }
        glDeleteBuffers(this.glBuffer);
    }

    /**
     * Updates this tracker instance based on the state of the given {@link WorldRenderer}.
     */
    public void update(@NonNull WorldRenderer worldRenderer, @NonNull ClippingHelper clippingHelper, int frameCount) {
        //figure out the maximum extents of all of the renderChunks in the current ViewFrustum
        ViewFrustum viewFrustum = ((ATWorldRenderer1_16) worldRenderer).getViewArea();
        boolean cubic = false;
        int sizeX = ((ATViewFrustum1_16) viewFrustum).getChunkGridSizeX();
        int sizeY = ((ATViewFrustum1_16) viewFrustum).getChunkGridSizeY();
        int sizeZ = ((ATViewFrustum1_16) viewFrustum).getChunkGridSizeZ();

        int renderPosX = floorI(((ATWorldRenderer1_16) worldRenderer).getLastCameraX()) - 8;
        int renderPosY = floorI(((ATWorldRenderer1_16) worldRenderer).getLastCameraY()) - 8;
        int renderPosZ = floorI(((ATWorldRenderer1_16) worldRenderer).getLastCameraZ()) - 8;

        int minChunkX = asrCeil(renderPosX - (sizeX << 4 >> 1), 4);
        int maxChunkX = asrCeil(renderPosX + (sizeX << 4 >> 1), 4);
        int minChunkY;
        int maxChunkY;
        int minChunkZ = asrCeil(renderPosZ - (sizeZ << 4 >> 1), 4);
        int maxChunkZ = asrCeil(renderPosZ + (sizeZ << 4 >> 1), 4);

        if (cubic) {
            minChunkY = asrCeil(renderPosY - (sizeY << 4 >> 1), 4);
            maxChunkY = asrCeil(renderPosY + (sizeY << 4 >> 1), 4);
        } else {
            minChunkY = 0;
            maxChunkY = 16;
        }

        int offsetChunkX = -minChunkX;
        int offsetChunkY = -minChunkY;
        int offsetChunkZ = -minChunkZ;
        int factorChunkX = maxChunkX - minChunkX;
        int factorChunkY = maxChunkY - minChunkY;
        int factorChunkZ = maxChunkZ - minChunkZ;

        long[] srcFlags = new long[factorChunkX * factorChunkY * factorChunkZ];
        //iterate over all the ContainerLocalRenderInformation instances, because they indicate:
        // - which direction to enter a RenderChunk from
        // - whether or not a RenderChunk is actually selected to be rendered
        ((ATWorldRenderer1_16) worldRenderer).getRenderChunks().forEach(containerLocalRenderInformation -> {
            long flags = 0L;
            flags |= renderDirectionFlags(containerLocalRenderInformation);
            flags |= inFaceFlags(containerLocalRenderInformation);

            flags |= FLAG_SELECTED;

            BlockPos pos = containerLocalRenderInformation.getChunk().getOrigin();
            int chunkX = pos.getX() >> 4;
            int chunkY = pos.getY() >> 4;
            int chunkZ = pos.getZ() >> 4;
            srcFlags[((chunkX + offsetChunkX) * factorChunkY + (chunkY + offsetChunkY)) * factorChunkZ + (chunkZ + offsetChunkZ)] = flags;
        });

        //iterate over all the RenderChunks, because they indicate:
        // - whether or not a RenderChunk's frame index is up-to-date, which can be used to determine whether a RenderChunk would have been selected but failed the frustum check
        // - whether or not a RenderChunk has been baked
        // - the RenderChunk's neighbor visibility graph
        Stream.of(viewFrustum.chunks).forEach(renderChunk -> {
            long flags = 0L;

            if (((ATChunkRenderDispatcher__ChunkRender1_16) renderChunk).getLastFrame() == frameCount) {
                flags |= FLAG_RENDERABLE;
            }

            if (clippingHelper.isVisible(renderChunk.bb)) {
                flags |= FLAG_INFRUSTUM;
            }

            ChunkRenderDispatcher.CompiledChunk compiledChunk = renderChunk.getCompiledChunk();
            if (compiledChunk != ChunkRenderDispatcher.CompiledChunk.UNCOMPILED) {
                flags |= FLAG_BAKED;
                flags |= visibilityFlags(compiledChunk);
            }

            BlockPos pos = renderChunk.getOrigin();
            int chunkX = pos.getX() >> 4;
            int chunkY = pos.getY() >> 4;
            int chunkZ = pos.getZ() >> 4;
            srcFlags[((chunkX + offsetChunkX) * factorChunkY + (chunkY + offsetChunkY)) * factorChunkZ + (chunkZ + offsetChunkZ)] |= flags;
        });

        //iterate over the whole grid again, setting the flag to indicate fp2 rendering is blocked by the RenderChunk at the given position if
        //  the following applies:
        //  - the RenderChunk has been compiled
        //  - all of the RenderChunk's visible neighbors have been compiled

        int offset0 = (Direction.DOWN.getStepX() * factorChunkY + Direction.DOWN.getStepY()) * factorChunkZ + Direction.DOWN.getStepZ();
        int offset1 = (Direction.UP.getStepX() * factorChunkY + Direction.UP.getStepY()) * factorChunkZ + Direction.UP.getStepZ();
        int offset2 = (Direction.NORTH.getStepX() * factorChunkY + Direction.NORTH.getStepY()) * factorChunkZ + Direction.NORTH.getStepZ();
        int offset3 = (Direction.SOUTH.getStepX() * factorChunkY + Direction.SOUTH.getStepY()) * factorChunkZ + Direction.SOUTH.getStepZ();
        int offset4 = (Direction.WEST.getStepX() * factorChunkY + Direction.WEST.getStepY()) * factorChunkZ + Direction.WEST.getStepZ();
        int offset5 = (Direction.EAST.getStepX() * factorChunkY + Direction.EAST.getStepY()) * factorChunkZ + Direction.EAST.getStepZ();
        int[] offsets = { offset0, offset1, offset2, offset3, offset4, offset5 };

        long sizeBits = factorChunkX * factorChunkY * factorChunkZ;
        long sizeBytes = FLAGS_OFFSET + (PMath.roundUp(sizeBits, 32) >> 2);

        long addr = this.alloc.alloc(sizeBytes);
        PUnsafe.setMemory(addr, sizeBytes, (byte) 0);

        int scanMinX = 1;
        int scanMaxX = factorChunkX - 1;
        int scanMinY;
        int scanMaxY;
        int scanMinZ = 1;
        int scanMaxZ = factorChunkZ - 1;

        if (cubic) {
            scanMinY = 1;
            scanMaxY = factorChunkY - 1;
        } else {
            scanMinY = 0;
            scanMaxY = 16;
        }

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

                            long neighborFlags = srcFlags[idx + offsets[outFace]];

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

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, this.glBuffer);
        nglBufferData(GL_SHADER_STORAGE_BUFFER, sizeBytes, addr, GL_STREAM_DRAW);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 7, this.glBuffer);
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
