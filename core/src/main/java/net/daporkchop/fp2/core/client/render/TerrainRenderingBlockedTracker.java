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

import lombok.val;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.core.client.FP2Client;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLExtensionSet;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.buffer.GLMutableBuffer;
import net.daporkchop.fp2.gl.util.GLRequires;
import net.daporkchop.lib.common.math.BinMath;
import net.daporkchop.lib.common.math.PMath;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.Arrays;

import static net.daporkchop.fp2.core.util.math.MathUtil.*;
import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
public abstract class TerrainRenderingBlockedTracker implements AutoCloseable {
    /**
     * The set of {@link GLExtension OpenGL extensions} which must be supported in order for the terrain rendering blocked tracker to be usable on the GPU.
     * <p>
     * See {@code resources/assets/fp2/shaders/util/vanilla_renderability.glsl}.
     */
    public static final GLExtensionSet REQUIRED_EXTENSIONS_GPU = GLExtensionSet.empty()
            .add(GLExtension.GL_ARB_uniform_buffer_object)
            .add(GLExtension.GL_ARB_shader_storage_buffer_object); //TODO: we could probably emulate this functionality using texture buffers, but I doubt there's any hardware which could benefit from GPU culling while not supporting SSBOs (leaving aside Mac devices)

    protected static final long HEADERS_OFFSET = 0L;
    protected static final long FLAGS_OFFSET = HEADERS_OFFSET + 2L * (4 * Integer.BYTES);

    protected static final int FACE_COUNT = 6;

    //@formatter:off
    protected static final int[] FACE_OFFSETS = {
             0, -1,  0, //DOWN
             0,  1,  0, //UP
             0,  0, -1, //NORTH
             0,  0,  1, //SOUTH
            -1,  0,  0, //WEST
             1,  0,  0, //EAST
    };
    //@formatter:on

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

    private final DirectMemoryAllocator alloc = new DirectMemoryAllocator();

    private final GLMutableBuffer glBuffer; //this is null if REQUIRED_EXTENSIONS_GPU aren't supported

    private int offsetX;
    private int offsetY;
    private int offsetZ;
    private int sizeX;
    private int sizeY;
    private int sizeZ;

    private long sizeBytes = -1L;
    private long addr;

    public TerrainRenderingBlockedTracker(FP2Client client) {
        assert Arrays.equals(this.getExpectedFaceOffsets(), FACE_OFFSETS)
                : "FACE_OFFSETS isn't correct!" +
                "\n  Expected:     " + Arrays.toString(this.getExpectedFaceOffsets()) +
                "\n  FACE_OFFSETS: " + Arrays.toString(FACE_OFFSETS);

        OpenGL gl = client.gl();
        this.glBuffer = gl.supports(REQUIRED_EXTENSIONS_GPU) ? GLMutableBuffer.create(gl) : null;
    }

    @Override
    public void close() {
        try (val ignored0 = this.alloc;
             val ignored1 = this.glBuffer) {
            this.alloc.free(this.addr);
        }
    }

    /**
     * @return gets the expected value of {@link #FACE_OFFSETS}
     * @apiNote this is only used for a sanity check on startup
     */
    protected abstract int[] getExpectedFaceOffsets();

    private static long allocationSizeBytes(int factorChunkX, int factorChunkY, int factorChunkZ) {
        long sizeBits = (long) factorChunkX * factorChunkY * factorChunkZ;
        return FLAGS_OFFSET + (PMath.roundUp(sizeBits, 32) >> 2);
    }

    @SuppressWarnings("PointlessArithmeticExpression")
    private static void initializeHeaders(long addr,
                                          int factorChunkX, int factorChunkY, int factorChunkZ,
                                          int offsetChunkX, int offsetChunkY, int offsetChunkZ) {
        long headersAddr = addr + HEADERS_OFFSET;

        //ivec3 offset
        PUnsafe.putInt(headersAddr + 0 * Integer.BYTES, offsetChunkX);
        PUnsafe.putInt(headersAddr + 1 * Integer.BYTES, offsetChunkY);
        PUnsafe.putInt(headersAddr + 2 * Integer.BYTES, offsetChunkZ);
        headersAddr += 4 * Integer.BYTES;

        //ivec3 size
        PUnsafe.putInt(headersAddr + 0 * Integer.BYTES, factorChunkX);
        PUnsafe.putInt(headersAddr + 1 * Integer.BYTES, factorChunkY);
        PUnsafe.putInt(headersAddr + 2 * Integer.BYTES, factorChunkZ);
        headersAddr += 4 * Integer.BYTES;
    }

    protected final long preTransformFlags(int factorChunkX, int factorChunkY, int factorChunkZ,
                                           int offsetChunkX, int offsetChunkY, int offsetChunkZ) {
        long sizeBytes = allocationSizeBytes(factorChunkX, factorChunkY, factorChunkZ);
        if (sizeBytes != this.sizeBytes) {
            this.addr = this.alloc.alloc(sizeBytes);
            this.sizeBytes = sizeBytes;
        }

        PUnsafe.setMemory(this.addr, sizeBytes, (byte) 0);
        initializeHeaders(this.addr, factorChunkX, factorChunkY, factorChunkZ, offsetChunkX, offsetChunkY, offsetChunkZ);

        this.offsetX = offsetChunkX;
        this.offsetY = offsetChunkY;
        this.offsetZ = offsetChunkZ;
        this.sizeX = factorChunkX;
        this.sizeY = factorChunkY;
        this.sizeZ = factorChunkZ;

        return this.addr;
    }

    protected final void postTransformFlags() {
        //glBuffer may be null if REQUIRED_EXTENSIONS_GPU aren't supported, in which case there's no point in updating the GPU buffer
        if (this.glBuffer != null) {
            this.glBuffer.upload(this.addr, this.sizeBytes, BufferUsage.STATIC_DRAW);
        }
    }

    @SuppressWarnings("UnnecessaryUnaryMinus")
    private static int[] getFaceIndexOffsets(int factorChunkX, int factorChunkY, int factorChunkZ) {
        //@formatter:off
        return new int[] {
                ( 0 * factorChunkY + -1) * factorChunkZ +  0, //DOWN
                ( 0 * factorChunkY +  1) * factorChunkZ +  0, //UP
                ( 0 * factorChunkY +  0) * factorChunkZ + -1, //NORTH
                ( 0 * factorChunkY +  0) * factorChunkZ +  1, //SOUTH
                (-1 * factorChunkY +  0) * factorChunkZ +  0, //WEST
                ( 1 * factorChunkY +  0) * factorChunkZ +  0, //EAST
        };
        //@formatter:on
    }

    //TODO: document whatever the fuck this is, lmao
    protected static void transformFlags(int scanMinX, int scanMaxX, int scanMinY, int scanMaxY, int scanMinZ, int scanMaxZ,
                                         int minChunkX, int maxChunkX, int minChunkY, int maxChunkY, int minChunkZ, int maxChunkZ,
                                         int factorChunkX, int factorChunkY, int factorChunkZ,
                                         int offsetChunkX, int offsetChunkY, int offsetChunkZ,
                                         long[] srcFlags,
                                         long addr) {
        int[] faceIndexOffsets = getFaceIndexOffsets(factorChunkX, factorChunkY, factorChunkZ);

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

                            long neighborFlags = srcFlags[idx + faceIndexOffsets[outFace]];

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

    /**
     * Binds the buffer(s) containing vanilla renderability information to the OpenGL context.
     *
     * @param gl               the OpenGL context
     * @param uboBindingIndex  the binding index for the vanilla renderability UBO
     * @param ssboBindingIndex the binding index for the vanilla renderability SSBO
     * @throws UnsupportedOperationException if the {@link #REQUIRED_EXTENSIONS_GPU required OpenGL extensions} aren't supported
     */
    @GLRequires({ GLExtension.GL_ARB_uniform_buffer_object, GLExtension.GL_ARB_shader_storage_buffer_object })
    public final void bindGlBuffers(OpenGL gl, int uboBindingIndex, int ssboBindingIndex) throws UnsupportedOperationException {
        gl.checkSupported(REQUIRED_EXTENSIONS_GPU); //TODO: check if glBuffer is null instead?

        gl.glBindBufferRange(GL_UNIFORM_BUFFER, uboBindingIndex, this.glBuffer.id(), 0L, FLAGS_OFFSET);
        gl.glBindBufferRange(GL_SHADER_STORAGE_BUFFER, ssboBindingIndex, this.glBuffer.id(), FLAGS_OFFSET, this.sizeBytes - FLAGS_OFFSET);
    }
}
