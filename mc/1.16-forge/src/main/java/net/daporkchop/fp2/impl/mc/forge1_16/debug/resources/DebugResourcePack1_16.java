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

package net.daporkchop.fp2.impl.mc.forge1_16.debug.resources;

import com.google.common.collect.ImmutableSet;
import lombok.SneakyThrows;
import net.daporkchop.fp2.core.debug.resources.DebugResources;
import net.minecraft.resources.IResourcePack;
import net.minecraft.resources.ResourcePackFileNotFoundException;
import net.minecraft.resources.ResourcePackType;
import net.minecraft.resources.data.IMetadataSectionSerializer;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * @author DaPorkchop_
 */
public class DebugResourcePack1_16 implements IResourcePack {
    @Override
    public InputStream getRootResource(String path) throws IOException {
        return DebugResources.findStream(Paths.get(path))
                .orElseThrow(() -> new ResourcePackFileNotFoundException(Paths.get(path).toFile(), path));
    }

    @Override
    public InputStream getResource(ResourcePackType type, ResourceLocation location) throws IOException {
        return this.getRootResource(type.getDirectory() + '/' + location.getNamespace() + '/' + location.getPath());
    }

    @Override
    public boolean hasResource(ResourcePackType type, ResourceLocation location) {
        return DebugResources.findPath(Paths.get(type.getDirectory() + '/' + location.getNamespace() + '/' + location.getPath())).isPresent();
    }

    @Override
    @SneakyThrows(IOException.class)
    public Collection<ResourceLocation> getResources(ResourcePackType type, String namespace, String category, int maxDepth, Predicate<String> filter) {
        return DebugResources.list(Paths.get(type.getDirectory() + '/' + namespace + '/' + category), maxDepth).stream()
                .filter(path -> filter.test(path.getFileName().toString()))
                .map(path -> new ResourceLocation(namespace, path.toString().substring((type.getDirectory() + '/' + namespace + '/').length())))
                .collect(Collectors.toList());
    }

    @Override
    public Set<String> getNamespaces(ResourcePackType p_195759_1_) {
        return ImmutableSet.of(MODID);
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(IMetadataSectionSerializer<T> p_195760_1_) throws IOException {
        return null;
    }

    @Override
    public String getName() {
        return "FarPlaneTwo debug resources";
    }

    @Override
    public void close() {
        //no-op
    }
}
