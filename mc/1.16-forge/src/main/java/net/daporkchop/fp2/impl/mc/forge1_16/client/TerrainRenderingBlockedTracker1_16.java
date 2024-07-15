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

package net.daporkchop.fp2.impl.mc.forge1_16.client;

import lombok.NonNull;
import net.daporkchop.fp2.core.client.FP2Client;
import net.daporkchop.fp2.core.client.render.TerrainRenderingBlockedTracker;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.at.client.renderer.ATViewFrustum1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.at.client.renderer.ATWorldRenderer1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.at.client.renderer.ATWorldRenderer__LocalRenderInformationContainer1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.at.client.renderer.chunk.ATChunkRenderDispatcher__ChunkRender1_16;
import net.daporkchop.lib.common.math.PMath;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.fp2.core.util.math.MathUtil.*;
import static net.daporkchop.lib.common.math.PMath.*;

/**
 * @author DaPorkchop_
 */
public class TerrainRenderingBlockedTracker1_16 extends TerrainRenderingBlockedTracker {
    protected static final Direction[] DIRECTIONS = Direction.values();

    static {
        int[] expectedFaceOffsets = Stream.of(DIRECTIONS)
            .flatMapToInt(direction -> IntStream.of(direction.getStepX(), direction.getStepY(), direction.getStepZ()))
            .toArray();
        if (!Arrays.equals(expectedFaceOffsets, FACE_OFFSETS)) {
            throw new IllegalStateException("FACE_OFFSETS isn't correct!" +
                    "\n  Expected:     " + Arrays.toString(expectedFaceOffsets) +
                    "\n  FACE_OFFSETS: " + Arrays.toString(FACE_OFFSETS));
        }
    }


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

    protected static long inFaceFlags(ATWorldRenderer__LocalRenderInformationContainer1_16 containerLocalRenderInformation) {
        Direction direction = containerLocalRenderInformation.getSourceDirection();
        if (direction != null) {
            return (direction.ordinal() + 1L) << SHIFT_INFACE;
        } else { //direction is null, meaning this is the origin chunk section
            return 0L; //return an invalid face index, which will become negative when decoded
        }
    }

    public TerrainRenderingBlockedTracker1_16(FP2Client client) {
        super(client);
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

        transformFlags(scanMinX, scanMaxX, scanMinY, scanMaxY, scanMinZ, scanMaxZ,
                minChunkX, maxChunkX, minChunkY, maxChunkY, minChunkZ, maxChunkZ,
                factorChunkX, factorChunkY, factorChunkZ,
                offsetChunkX, offsetChunkY, offsetChunkZ,
                offsets, srcFlags, addr);

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

        this.glBuffer.upload(addr, sizeBytes, BufferUsage.STATIC_DRAW);
    }
}
