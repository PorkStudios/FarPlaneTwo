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

package net.daporkchop.fp2.impl.mc.forge1_12_2.client;

import lombok.NonNull;
import net.daporkchop.fp2.core.client.FP2Client;
import net.daporkchop.fp2.core.client.render.TerrainRenderingBlockedTracker;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.at.client.renderer.ATRenderChunk1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.at.client.renderer.ATRenderGlobal1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.at.client.renderer.ATRenderGlobal__ContainerLocalRenderInformation1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.at.client.renderer.ATViewFrustum1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.cc.FP2CubicChunks1_12;
import net.daporkchop.lib.common.math.PMath;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.util.EnumFacing;
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
public class TerrainRenderingBlockedTracker1_12 extends TerrainRenderingBlockedTracker {
    static {
        int[] expectedFaceOffsets = Stream.of(EnumFacing.VALUES)
            .flatMapToInt(direction -> IntStream.of(direction.getXOffset(), direction.getYOffset(), direction.getZOffset()))
            .toArray();
        if (!Arrays.equals(expectedFaceOffsets, FACE_OFFSETS)) {
            throw new IllegalStateException("FACE_OFFSETS isn't correct!" +
                    "\n  Expected:     " + Arrays.toString(expectedFaceOffsets) +
                    "\n  FACE_OFFSETS: " + Arrays.toString(FACE_OFFSETS));
        }
    }

    protected static long visibilityFlags(CompiledChunk compiledChunk) {
        long flags = 0L;
        int shift = SHIFT_VISIBILITY;
        for (EnumFacing outFace : EnumFacing.VALUES) {
            for (EnumFacing inFace : EnumFacing.VALUES) {
                if (compiledChunk.isVisible(inFace, outFace)) {
                    flags |= 1L << shift;
                }
                shift++;
            }
        }
        return flags;
    }

    protected static long renderDirectionFlags(ATRenderGlobal__ContainerLocalRenderInformation1_12 containerLocalRenderInformation) {
        long flags = 0L;
        int shift = SHIFT_RENDER_DIRECTIONS;
        for (EnumFacing inFace : EnumFacing.VALUES) {
            if (containerLocalRenderInformation.invokeHasDirection(inFace)) {
                flags |= 1L << shift;
            }
            shift++;
        }
        return flags;
    }

    protected static long inFaceFlags(ATRenderGlobal__ContainerLocalRenderInformation1_12 containerLocalRenderInformation) {
        EnumFacing direction = containerLocalRenderInformation.getFacing();
        if (direction != null) {
            return (direction.ordinal() + 1L) << SHIFT_INFACE;
        } else { //direction is null, meaning this is the origin chunk section
            return 0L; //return an invalid face index, which will become negative when decoded
        }
    }

    public TerrainRenderingBlockedTracker1_12(FP2Client client) {
        super(client);
    }

    /**
     * Updates this tracker instance based on the state of the given {@link RenderGlobal}.
     */
    public void update(@NonNull RenderGlobal renderGlobal, @NonNull ICamera camera, int frameCount) {
        //figure out the maximum extents of all of the renderChunks in the current ViewFrustum
        ViewFrustum viewFrustum = ((ATRenderGlobal1_12) renderGlobal).getViewFrustum();
        boolean cubic = FP2CubicChunks1_12.isCubicWorld(((ATViewFrustum1_12) viewFrustum).getWorld());
        int sizeX = ((ATViewFrustum1_12) viewFrustum).getCountChunksX();
        int sizeY = ((ATViewFrustum1_12) viewFrustum).getCountChunksY();
        int sizeZ = ((ATViewFrustum1_12) viewFrustum).getCountChunksZ();

        int renderPosX = floorI(((ATRenderGlobal1_12) renderGlobal).getFrustumUpdatePosX()) - 8;
        int renderPosY = floorI(((ATRenderGlobal1_12) renderGlobal).getFrustumUpdatePosY()) - 8;
        int renderPosZ = floorI(((ATRenderGlobal1_12) renderGlobal).getFrustumUpdatePosZ()) - 8;

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
        ((ATRenderGlobal1_12) renderGlobal).getRenderInfos().forEach(containerLocalRenderInformation -> {
            long flags = 0L;
            flags |= renderDirectionFlags(containerLocalRenderInformation);
            flags |= inFaceFlags(containerLocalRenderInformation);

            flags |= FLAG_SELECTED;

            BlockPos pos = containerLocalRenderInformation.getRenderChunk().getPosition();
            int chunkX = pos.getX() >> 4;
            int chunkY = pos.getY() >> 4;
            int chunkZ = pos.getZ() >> 4;
            assert chunkX >= minChunkX && chunkX < maxChunkX && chunkY >= minChunkY && chunkY < maxChunkY && chunkZ >= minChunkZ && chunkZ < maxChunkZ
                    : "RenderChunk(" + chunkX + ',' + chunkY + ',' + chunkZ + ") is outside of render distance: from (" + minChunkX + ',' + minChunkY + ',' + minChunkZ + ") to (" + maxChunkX + ',' + maxChunkY + ',' + maxChunkZ + ')';
            srcFlags[((chunkX + offsetChunkX) * factorChunkY + (chunkY + offsetChunkY)) * factorChunkZ + (chunkZ + offsetChunkZ)] = flags;
        });

        //iterate over all the RenderChunks, because they indicate:
        // - whether or not a RenderChunk's frame index is up-to-date, which can be used to determine whether a RenderChunk would have been selected but failed the frustum check
        // - whether or not a RenderChunk has been baked
        // - the RenderChunk's neighbor visibility graph
        Stream.of(viewFrustum.renderChunks).forEach(renderChunk -> {
            long flags = 0L;

            if (((ATRenderChunk1_12) renderChunk).getFrameIndex() == frameCount) {
                flags |= FLAG_RENDERABLE;
            }

            if (camera.isBoundingBoxInFrustum(renderChunk.boundingBox)) {
                flags |= FLAG_INFRUSTUM;
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
            assert chunkX >= minChunkX && chunkX < maxChunkX && chunkY >= minChunkY && chunkY < maxChunkY && chunkZ >= minChunkZ && chunkZ < maxChunkZ
                    : "RenderChunk(" + chunkX + ',' + chunkY + ',' + chunkZ + ") is outside of render distance: from (" + minChunkX + ',' + minChunkY + ',' + minChunkZ + ") to (" + maxChunkX + ',' + maxChunkY + ',' + maxChunkZ + ')';
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

        /*for (int x = scanMinX; x < scanMaxX; x++) {
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
        }*/

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
