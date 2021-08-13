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

package net.daporkchop.fp2.client.gl;

import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.lib.common.misc.string.PStrings;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GLContext;

import java.util.Comparator;
import java.util.StringTokenizer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static net.daporkchop.fp2.debug.FP2Debug.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGetInteger;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.util.glu.GLU.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class OpenGL {
    public final int BYTE_SIZE = Byte.BYTES;
    public final int SHORT_SIZE = Short.BYTES;
    public final int MEDIUM_SIZE = SHORT_SIZE + BYTE_SIZE;
    public final int FLOAT_SIZE = Float.BYTES;
    public final int INT_SIZE = Integer.BYTES;
    public final int DOUBLE_SIZE = Double.BYTES;
    public final int LONG_SIZE = Long.BYTES;

    public final int VEC2_ELEMENTS = 2;
    public final int VEC2_SIZE = VEC2_ELEMENTS * FLOAT_SIZE;
    public final int IVEC2_SIZE = VEC2_ELEMENTS * INT_SIZE;
    public final int DVEC2_SIZE = VEC2_ELEMENTS * DOUBLE_SIZE;

    public final int VEC3_ELEMENTS = 3;
    public final int VEC3_SIZE_TIGHT = VEC3_ELEMENTS * FLOAT_SIZE;
    public final int IVEC3_SIZE_TIGHT = VEC3_ELEMENTS * INT_SIZE;
    public final int DVEC3_SIZE_TIGHT = VEC3_ELEMENTS * DOUBLE_SIZE;
    public final int VEC3_SIZE = (VEC3_ELEMENTS + 1) * FLOAT_SIZE;
    public final int IVEC3_SIZE = (VEC3_ELEMENTS + 1) * INT_SIZE;
    public final int DVEC3_SIZE = (VEC3_ELEMENTS + 1) * DOUBLE_SIZE;

    public final int VEC4_ELEMENTS = 4;
    public final int VEC4_SIZE = VEC4_ELEMENTS * FLOAT_SIZE;
    public final int IVEC4_SIZE = VEC4_ELEMENTS * INT_SIZE;
    public final int DVEC4_SIZE = VEC4_ELEMENTS * DOUBLE_SIZE;

    public final int MAT4_ELEMENTS = 4 * 4;
    public final int MAT4_SIZE = MAT4_ELEMENTS * FLOAT_SIZE;

    public final boolean OPENGL_33;
    public final boolean OPENGL_42;
    public final boolean OPENGL_43;
    public final boolean OPENGL_44;
    public final boolean OPENGL_45;
    public final boolean OPENGL_46;

    public final int MAX_COMPUTE_WORK_GROUP_COUNT_X;
    public final int MAX_COMPUTE_WORK_GROUP_COUNT_Y;
    public final int MAX_COMPUTE_WORK_GROUP_COUNT_Z;
    public final WorkGroupSize MAX_COMPUTE_WORK_GROUP_SIZE;
    public final int MAX_COMPUTE_WORK_GROUP_INVOCATIONS;

    static {
        ContextCapabilities capabilities = GLContext.getCapabilities();
        OPENGL_33 = capabilities.OpenGL33;
        OPENGL_42 = capabilities.OpenGL42;
        OPENGL_43 = capabilities.OpenGL43;
        OPENGL_44 = capabilities.OpenGL44;
        OPENGL_45 = capabilities.OpenGL45;

        //ContextCapabilities only knows versions up to 4.5...
        //copypasta'd from GLContext.getSupportedExtensions()
        StringTokenizer version_tokenizer = new StringTokenizer(glGetString(GL_VERSION), ". ");
        int majorVersion = Integer.parseInt(version_tokenizer.nextToken());
        int minorVersion = Integer.parseInt(version_tokenizer.nextToken());
        OPENGL_46 = (majorVersion == 4 && minorVersion >= 6) || majorVersion > 4;

        MAX_COMPUTE_WORK_GROUP_COUNT_X = glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_COUNT, 0);
        MAX_COMPUTE_WORK_GROUP_COUNT_Y = glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_COUNT, 1);
        MAX_COMPUTE_WORK_GROUP_COUNT_Z = glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_COUNT, 2);
        MAX_COMPUTE_WORK_GROUP_SIZE = new WorkGroupSize(
                glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_SIZE, 0),
                glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_SIZE, 1),
                glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_SIZE, 2));
        MAX_COMPUTE_WORK_GROUP_INVOCATIONS = glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS);
    }

    /**
     * Checks for OpenGL errors, printing a message if any occurred.
     * <p>
     * Note that this function is a no-op if not in debug mode.
     *
     * @param message the message to print alongside the OpenGL error code
     */
    public void checkGLError(String message) {
        if (FP2_DEBUG) {
            for (int error; (error = glGetError()) != GL_NO_ERROR; ) {
                Constants.FP2_LOG.error("########## GL ERROR ##########");
                Constants.FP2_LOG.error("@ {}", message);
                Constants.FP2_LOG.error("{}: {}", error, gluErrorString(error));
            }
        }
    }

    protected Stream<WorkGroupSize> allPossibleWorkGroupSizes() {
        return IntStream.rangeClosed(1, MAX_COMPUTE_WORK_GROUP_SIZE.x())
                .mapToObj(x -> IntStream.rangeClosed(1, MAX_COMPUTE_WORK_GROUP_SIZE.y())
                        .mapToObj(y -> IntStream.rangeClosed(1, MAX_COMPUTE_WORK_GROUP_SIZE.z())
                                .mapToObj(z -> new WorkGroupSize(x, y, z)))
                        .flatMap(Function.identity()))
                .flatMap(Function.identity());
    }

    protected Stream<WorkGroupSize> allPossibleWorkGroupSizesPow2() {
        return IntStream.rangeClosed(0, Integer.numberOfTrailingZeros(Integer.highestOneBit(MAX_COMPUTE_WORK_GROUP_SIZE.x())))
                .mapToObj(xShift -> IntStream.rangeClosed(0, Integer.numberOfTrailingZeros(Integer.highestOneBit(MAX_COMPUTE_WORK_GROUP_SIZE.y())))
                        .mapToObj(yShift -> IntStream.rangeClosed(0, Integer.numberOfTrailingZeros(Integer.highestOneBit(MAX_COMPUTE_WORK_GROUP_SIZE.z())))
                                .mapToObj(zShift -> new WorkGroupSize(1 << xShift, 1 << yShift, 1 << zShift)))
                        .flatMap(Function.identity()))
                .flatMap(Function.identity());
    }

    /**
     * Computes the optimal size of a compute shader work group, given some arbitrary upper and lower bounds.
     * <p>
     * If either {@code minSize} or {@code maxSize} are {@code null}, they will be treated as if there were no limits in that direction.
     * <p>
     * The bounds given here define the total size of the work group. If you need control over the individual axes, use {@link #getOptimalComputeWorkGroupSizeAxis(WorkGroupSize, WorkGroupSize)}.
     *
     * @param minSize the minimum allowed size of a single compute work group (inclusive)
     * @param maxSize the maximum allowed size of a single compute work group (inclusive)
     * @return the optimal size of a compute shader work group for the given bounds
     */
    public WorkGroupSize getOptimalComputeWorkGroupSizeTotal(Integer minSize, Integer maxSize) {
        //check arguments
        if (minSize != null) {
            positive(minSize, "minSize");
        }
        if (maxSize != null) {
            positive(maxSize, "maxSize");
            checkArg(minSize == null || maxSize >= minSize, "maxSize (%d) must not be less than than minSize (%d)!", maxSize, minSize);
        }

        int minSizeI = minSize != null ? minSize : 1;
        int maxSizeI = maxSize != null ? maxSize : MAX_COMPUTE_WORK_GROUP_INVOCATIONS;

        //brute-force search (this is stupidly inefficient, but i don't feel like optimizing this and it only has to run once anyway)
        return allPossibleWorkGroupSizes().parallel()
                //only allow work groups whose total size is in the given range
                .filter(workGroupSize -> workGroupSize.totalSize() >= minSizeI && workGroupSize.totalSize() <= maxSizeI)
                //sorting by reverse order gives us a stream of work groups starting with the largest total size.
                //  yes, this buffers the whole stream, but who cares? most likely it'll only be a few thousand elements
                .sorted(Comparator.reverseOrder())
                //the first one is the biggest one in range
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(PStrings.fastFormat(
                        "impossible to find a work group size in range [%d, %d] (GL_MAX_COMPUTE_WORK_GROUP_SIZE=%s, GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS=%d)",
                        minSize, maxSize, MAX_COMPUTE_WORK_GROUP_SIZE, MAX_COMPUTE_WORK_GROUP_INVOCATIONS)));
    }

    /**
     * Computes the optimal size of a compute shader work group, given some arbitrary upper and lower bounds.
     * <p>
     * If either {@code minSize} or {@code maxSize} are {@code null}, they will be treated as if there were no limits in that direction.
     * <p>
     * The bounds given here define the size of the work group per-axis. If you don't need control over the individual axes, use {@link #getOptimalComputeWorkGroupSizeTotal(Integer, Integer)}.
     *
     * @param minSize the minimum allowed size of a single compute work group (inclusive)
     * @param maxSize the maximum allowed size of a single compute work group (inclusive)
     * @return the optimal size of a compute shader work group for the given bounds
     */
    public WorkGroupSize getOptimalComputeWorkGroupSizeAxis(WorkGroupSize minSize, WorkGroupSize maxSize) {
        //check arguments
        if (maxSize != null) {
            checkArg(minSize == null || (maxSize.x() >= minSize.x() && maxSize.y() >= minSize.y() && maxSize.z() >= minSize.z()), "maxSize (%s) must not be less than than minSize (%s)!", maxSize, minSize);
        }

        WorkGroupSize minSizeI = minSize != null ? minSize : new WorkGroupSize(1, 1, 1);
        WorkGroupSize maxSizeI = maxSize != null ? maxSize : MAX_COMPUTE_WORK_GROUP_SIZE;

        //brute-force search (this is stupidly inefficient, but i don't feel like optimizing this and it only has to run once anyway)
        return allPossibleWorkGroupSizes().parallel()
                //only allow work groups whose total size is valid and whose component-wise sizes are in the given range
                .filter(workGroupSize -> workGroupSize.totalSize() < MAX_COMPUTE_WORK_GROUP_INVOCATIONS
                                         && workGroupSize.x() >= minSizeI.x() && workGroupSize.x() <= maxSizeI.x()
                                         && workGroupSize.y() >= minSizeI.y() && workGroupSize.y() <= maxSizeI.y()
                                         && workGroupSize.z() >= minSizeI.z() && workGroupSize.z() <= maxSizeI.z())
                //sorting by reverse order gives us a stream of work groups starting with the largest total size.
                //  yes, this buffers the whole stream, but who cares? most likely it'll only be a few thousand elements
                .sorted(Comparator.reverseOrder())
                //the first one is the biggest one in range
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(PStrings.fastFormat(
                        "impossible to find a work group size in range [%s, %s] (GL_MAX_COMPUTE_WORK_GROUP_SIZE=%s, GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS=d)",
                        minSize, maxSize, MAX_COMPUTE_WORK_GROUP_SIZE, MAX_COMPUTE_WORK_GROUP_INVOCATIONS)));
    }

    /**
     * Computes the optimal size of a compute shader work group, given some arbitrary upper and lower bounds. This method is similar to
     * {@link #getOptimalComputeWorkGroupSizeTotal(Integer, Integer)}, except that it is guaranteed to return a {@link WorkGroupSize} whose {@link WorkGroupSize#totalSize()} is a
     * power of 2.
     * <p>
     * If either {@code minSize} or {@code maxSize} are {@code null}, they will be treated as if there were no limits in that direction.
     *
     * @param minSize the minimum allowed size of a single compute work group (inclusive)
     * @param maxSize the maximum allowed size of a single compute work group (inclusive)
     * @return the optimal size of a compute shader work group for the given bounds
     */
    public WorkGroupSize getOptimalComputeWorkSizePow2(Integer minSize, Integer maxSize) {
        //check arguments
        if (minSize != null) {
            positive(minSize, "minSize");
        }
        if (maxSize != null) {
            positive(maxSize, "maxSize");
            checkArg(minSize == null || maxSize >= minSize, "maxSize (%d) must not be less than than minSize (%d)!", maxSize, minSize);
        }

        int minSizeI = minSize != null ? minSize : 1;
        int maxSizeI = maxSize != null ? maxSize : MAX_COMPUTE_WORK_GROUP_INVOCATIONS;

        //brute-force search (this is stupidly inefficient, but i don't feel like optimizing this and it only has to run once anyway)
        return allPossibleWorkGroupSizesPow2().parallel()
                //only allow work groups whose total size is in the given range
                .filter(workGroupSize -> workGroupSize.totalSize() <= MAX_COMPUTE_WORK_GROUP_INVOCATIONS
                                         && workGroupSize.totalSize() >= minSizeI && workGroupSize.totalSize() <= maxSizeI)
                //sorting by reverse order gives us a stream of work groups starting with the largest total size.
                //  yes, this buffers the whole stream, but who cares? most likely it'll only be a few thousand elements
                .sorted(Comparator.reverseOrder())
                //the first one is the biggest one in range
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(PStrings.fastFormat(
                        "impossible to find a work group size in range [%d, %d] (GL_MAX_COMPUTE_WORK_GROUP_SIZE=%s, GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS=%d)",
                        minSize, maxSize, MAX_COMPUTE_WORK_GROUP_SIZE, MAX_COMPUTE_WORK_GROUP_INVOCATIONS)));
    }
}
