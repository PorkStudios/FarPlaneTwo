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

package net.daporkchop.fp2.core.engine.client.index;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.render.GlobalRenderer;
import net.daporkchop.fp2.core.client.render.state.CameraStateUniforms;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.engine.client.bake.storage.BakeStorage;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLExtensionSet;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.UniformBuffer;

import java.util.ServiceLoader;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public abstract class RenderIndexType {
    /**
     * Returns an {@link Iterable} each of the supported {@link RenderIndexType}s.
     *
     * @param fp2 the current {@link FP2Core FP2} instance
     * @return an {@link Iterable} each of the supported {@link RenderIndexType}s
     */
    public static Iterable<? extends RenderIndexType> getTypes(@NonNull FP2Core fp2) {
        return ServiceLoader.load(RenderIndexType.class, fp2.getClass().getClassLoader());
    }

    /**
     * The set of {@link GLExtension OpenGL extensions} which are required by this render index.
     */
    private final @NonNull GLExtensionSet requiredExtensions;

    /**
     * Checks if this render index implementation is enabled with the current configuration.
     *
     * @param config the current config
     * @return {@code true} if this render index implementation is enabled
     */
    public boolean enabled(@NonNull FP2Config config) {
        return true;
    }

    /**
     * @return {@code true} if this render index can only render tile data from a {@link BakeStorage} with {@link BakeStorage#absoluteIndices} set to {@code true}
     */
    public boolean absoluteIndices() {
        return false;
    }

    /**
     * Creates a new {@link RenderIndex} instance using this implementation.
     *
     * @param gl                        the OpenGL context
     * @param bakeStorage               the baked tile data storage
     * @param alloc                     an allocator for off-heap memory
     * @param globalRenderer            the {@link GlobalRenderer} instance
     * @param cameraStateUniformsBuffer the uniform buffer containing camera state uniforms
     * @return a new {@link RenderIndex} instance using this implementation
     * @throws UnsupportedOperationException if this render index implementation isn't supported
     */
    public abstract <VertexType extends AttributeStruct> RenderIndex<VertexType> createRenderIndex(OpenGL gl, BakeStorage<VertexType> bakeStorage, DirectMemoryAllocator alloc, GlobalRenderer globalRenderer, UniformBuffer<CameraStateUniforms> cameraStateUniformsBuffer);
}
