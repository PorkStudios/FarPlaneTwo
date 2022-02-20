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

package net.daporkchop.fp2.resources;

import lombok.NonNull;
import net.daporkchop.fp2.resources.simple.DirectoryFResources;
import net.daporkchop.fp2.resources.simple.EmptyFResources;
import net.daporkchop.fp2.resources.translate.MinecraftPackTranslatingFResources0To1;
import net.daporkchop.fp2.resources.translate.MinecraftPackTranslatingFResources1To2;
import net.daporkchop.fp2.resources.translate.MinecraftPackTranslatingFResources2To3;
import net.daporkchop.fp2.resources.translate.MinecraftPackTranslatingFResources3To4;
import net.daporkchop.fp2.resources.translate.MinecraftPackTranslatingFResources4To5;
import net.daporkchop.fp2.resources.translate.MinecraftPackTranslatingFResources5To6;
import net.daporkchop.lib.common.function.io.IOSupplier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A resource pack containing debug resources.
 *
 * @author DaPorkchop_
 */
public interface FResources {
    static FResources findForDebug(@NonNull String targetFormat) {
        Path path = Paths.get(".").toAbsolutePath();

        //find git repo root directory
        while (path != null && !Files.isDirectory(path.resolve(".git")) && !Files.isRegularFile(path.resolve("LICENSE"))) {
            path = path.getParent();
        }

        return translateResources(path != null ? new DirectoryFResources(path.resolve("core").resolve("resources")) : new EmptyFResources(), targetFormat);
    }

    static FResources translateResources(@NonNull FResources resources, @NonNull String targetFormat) {
        switch (targetFormat) {
            case "minecraft_pack_format_v1":
                return new MinecraftPackTranslatingFResources0To1(resources);
            case "minecraft_pack_format_v2":
                return new MinecraftPackTranslatingFResources1To2(translateResources(resources, "minecraft_pack_format_v1"));
            case "minecraft_pack_format_v3":
                return new MinecraftPackTranslatingFResources2To3(translateResources(resources, "minecraft_pack_format_v2"));
            case "minecraft_pack_format_v4":
                return new MinecraftPackTranslatingFResources3To4(translateResources(resources, "minecraft_pack_format_v3"));
            case "minecraft_pack_format_v5":
                return new MinecraftPackTranslatingFResources4To5(translateResources(resources, "minecraft_pack_format_v4"));
            case "minecraft_pack_format_v6":
                return new MinecraftPackTranslatingFResources5To6(translateResources(resources, "minecraft_pack_format_v5"));
            default:
                throw new IllegalArgumentException("unknown target format: " + targetFormat);
        }
    }

    /**
     * Gets an {@link IOSupplier} which will provide {@link InputStream}s for reading the resource with the given path.
     *
     * @param path the resource's {@link Path}
     * @return an {@link Optional} containing an {@link IOSupplier}, or an empty {@link Optional} if no resource exists with the given path
     */
    Optional<IOSupplier<InputStream>> getResource(@NonNull Path path);

    /**
     * Gets a {@link Stream} containing the paths of all resources in this pack.
     * <p>
     * Note that the returned {@link Stream} <strong>must</strong> be {@link Stream#close() closed} once no longer needed.
     *
     * @return a {@link Stream} containing the paths of all resources in this pack
     */
    IOSupplier<Stream<Path>> listResources();

    /**
     * Gets a {@link Stream} containing the paths of all resources in this pack under the given path.
     * <p>
     * Note that the returned {@link Stream} <strong>must</strong> be {@link Stream#close() closed} once no longer needed.
     *
     * @param path     the {@link Path} to serve as the root of the search
     * @param maxDepth the maximum recursion depth for the search
     * @return a {@link Stream} containing the paths of all resources in this pack under the given path
     */
    default IOSupplier<Stream<Path>> listResources(@NonNull Path path, int maxDepth) {
        return () -> this.listResources().getThrowing()
                .filter(p -> p.startsWith(path) && p.getNameCount() <= maxDepth);
    }
}
