/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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

package net.daporkchop.fp2.client;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.client.gl.object.GLBuffer;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.common.util.alloc.Allocator;
import net.daporkchop.lib.common.math.PMath;
import net.daporkchop.lib.common.misc.refcount.AbstractRefCounted;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.daporkchop.lib.unsafe.util.exception.AlreadyReleasedException;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.stream.Stream;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.lib.common.math.PMath.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * Keeps track of which chunk sections are able to be rendered by vanilla, and therefore should not be rendered by fp2 at detail level 0.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class VanillaRenderabilityTracker extends AbstractRefCounted {
    protected static final long HEADERS_OFFSET = 0L;
    protected static final long FLAGS_OFFSET = HEADERS_OFFSET + 2L * IVEC3_SIZE;

    protected static int visibilityMask(@NonNull CompiledChunk compiledChunk) {
        int mask = 0;
        for (EnumFacing facing : EnumFacing.VALUES) {
            if (compiledChunk.isVisible(facing, facing)) {
                mask |= 1 << facing.ordinal();
            }
        }
        return mask;
    }

    @NonNull
    protected final Allocator alloc;
    protected final GLBuffer glBuffer = new GLBuffer(GL_STREAM_DRAW);

    protected int offsetX;
    protected int offsetY;
    protected int offsetZ;
    protected int sizeX;
    protected int sizeY;
    protected int sizeZ;

    protected long sizeBytes;
    protected long addr;

    protected boolean dirty;

    @Override
    public VanillaRenderabilityTracker retain() throws AlreadyReleasedException {
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
    public void update(@NonNull RenderGlobal renderGlobal) {
        //figure out the maximum extents of all of the renderChunks in the current ViewFrustum
        ViewFrustum viewFrustum = renderGlobal.viewFrustum;
        boolean cubic = Constants.isCubicWorld(viewFrustum.world);
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

        //iterate over all the RenderChunks which have been compiled, setting bits indicating whether or not they exist and which edges' neighboring
        //  chunk sections are visible
        byte[] srcBits = new byte[factorChunkX * factorChunkY * factorChunkZ];
        Stream.of(viewFrustum.renderChunks).forEach(renderChunk -> {
            CompiledChunk compiledChunk = renderChunk.getCompiledChunk();
            if (compiledChunk == CompiledChunk.DUMMY) {
                return;
            }

            BlockPos pos = renderChunk.getPosition();
            int chunkX = pos.getX() >> 4;
            int chunkY = pos.getY() >> 4;
            int chunkZ = pos.getZ() >> 4;

            byte flags = (byte) (0x80 | visibilityMask(compiledChunk));
            srcBits[((chunkX + offsetChunkX) * factorChunkY + (chunkY + offsetChunkY)) * factorChunkZ + (chunkZ + offsetChunkZ)] = flags;
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

        long sizeBits = factorChunkX * factorChunkY * factorChunkZ;
        long sizeBytes = FLAGS_OFFSET + (PMath.roundUp(sizeBits, 32) >> 2);

        long addr = this.alloc.alloc(sizeBytes);
        PUnsafe.setMemory(addr, sizeBytes, (byte) 0);

        for (int x = 1; x < factorChunkX - 2; x++) {
            for (int y = 1; y < factorChunkY - 2; y++) {
                for (int z = 1; z < factorChunkZ - 2; z++) {
                    int idx = (x * factorChunkY + y) * factorChunkZ + z;
                    int centerFlags = srcBits[idx] & 0xFF;

                    //look ma, no branches!
                    long wordAddr = addr + FLAGS_OFFSET + (idx >> 5 << 2);
                    PUnsafe.putInt(wordAddr, PUnsafe.getInt(wordAddr)
                                             | (((centerFlags >> 7)
                                                 & (((~centerFlags >> 0) & 1) | (srcBits[idx + offset0] >> 7))
                                                 & (((~centerFlags >> 1) & 1) | (srcBits[idx + offset1] >> 7))
                                                 & (((~centerFlags >> 2) & 1) | (srcBits[idx + offset2] >> 7))
                                                 & (((~centerFlags >> 3) & 1) | (srcBits[idx + offset3] >> 7))
                                                 & (((~centerFlags >> 4) & 1) | (srcBits[idx + offset4] >> 7))
                                                 & (((~centerFlags >> 5) & 1) | (srcBits[idx + offset5] >> 7))) << idx));

                    //equivalent code:
                    /*flags[idx] = (centerFlags & 0x80) != 0
                                 && ((centerFlags & (1 << 0)) == 0 || (srcBits[idx + offset0] & 0x80) != 0)
                                 && ((centerFlags & (1 << 1)) == 0 || (srcBits[idx + offset1] & 0x80) != 0)
                                 && ((centerFlags & (1 << 2)) == 0 || (srcBits[idx + offset2] & 0x80) != 0)
                                 && ((centerFlags & (1 << 3)) == 0 || (srcBits[idx + offset3] & 0x80) != 0)
                                 && ((centerFlags & (1 << 4)) == 0 || (srcBits[idx + offset4] & 0x80) != 0)
                                 && ((centerFlags & (1 << 5)) == 0 || (srcBits[idx + offset5] & 0x80) != 0);*/

                    //equivalent code:
                    /*IF:
                    if ((centerFlags & 0x80) != 0) {
                        for (int i = 0; i < offsets.length; i++) {
                            if ((centerFlags & (1 << i)) != 0 && (srcBits[idx + offsets[i]] & 0x80) == 0) {
                                break GOTO;
                            }
                        }
                        flags[idx] = true;
                    }*/
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
            headersAddr += IVEC3_SIZE;

            //ivec3 size
            PUnsafe.putInt(headersAddr + 0 * INT_SIZE, factorChunkX);
            PUnsafe.putInt(headersAddr + 1 * INT_SIZE, factorChunkY);
            PUnsafe.putInt(headersAddr + 2 * INT_SIZE, factorChunkZ);
            headersAddr += IVEC3_SIZE;
        }

        this.offsetX = offsetChunkX;
        this.offsetY = offsetChunkY;
        this.offsetZ = offsetChunkZ;
        this.sizeX = factorChunkX;
        this.sizeY = factorChunkY;
        this.sizeZ = factorChunkZ;
        this.sizeBytes = sizeBytes;
        this.addr = addr;
        this.dirty = true;
    }

    /**
     * Checks whether or not vanilla terrain at the given chunk section would prevent us from rendering level-0 FP2 terrain.
     *
     * @param chunkX the chunk section's X coordinate
     * @param chunkY the chunk section's Y coordinate
     * @param chunkZ the chunk section's Z coordinate
     * @return whether or not vanilla terrain at the given chunk section would prevent us from rendering level-0 FP2 terrain
     */
    public boolean vanillaBlocksFP2RenderingAtLevel0(int chunkX, int chunkY, int chunkZ) {
        int x = chunkX + this.offsetX;
        int y = chunkY + this.offsetY;
        int z = chunkZ + this.offsetZ;

        if (x < 0 || x >= this.sizeX || y < 0 || y >= this.sizeY || z < 0 || z >= this.sizeZ) {
            return false;
        }

        int idx = (x * this.sizeY + y) * this.sizeZ + z;
        return (PUnsafe.getInt(this.addr + FLAGS_OFFSET + (idx >> 5 << 2)) & (1 << idx)) != 0;
    }

    /**
     * Binds the current state of this tracker to be accessed by shaders.
     */
    public void bindForShaderUse() {
        if (this.dirty) { //re-upload data if needed
            this.dirty = false;

            try (GLBuffer buffer = this.glBuffer.bind(GL_SHADER_STORAGE_BUFFER)) {
                buffer.upload(this.addr, this.sizeBytes);
            }
        }

        this.glBuffer.bindBase(GL_SHADER_STORAGE_BUFFER, 6);
    }
}
