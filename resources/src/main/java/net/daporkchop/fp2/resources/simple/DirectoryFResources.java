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

package net.daporkchop.fp2.resources.simple;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.resources.FResources;
import net.daporkchop.lib.common.function.io.IOSupplier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Implementation of {@link FResources} which reads from a directory.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class DirectoryFResources implements FResources {
    @NonNull
    protected final Path directory;

    @Override
    public Optional<IOSupplier<InputStream>> getResource(@NonNull Path path) {
        return Files.exists(this.directory.resolve(path))
                ? Optional.of(() -> Files.newInputStream(this.directory.resolve(path)))
                : Optional.empty();
    }

    @Override
    public IOSupplier<Stream<Path>> listResources() {
        return () -> {
            if (!Files.exists(this.directory)) { //prevent FileNotFoundException if the path doesn't exist
                return Stream.empty();
            }

            return Files.walk(this.directory)
                    .filter(Files::isRegularFile) //don't return directories as resources
                    .map(p -> p.relativize(this.directory)); //make paths relative to resource root
        };
    }

    @Override
    public IOSupplier<Stream<Path>> listResources(@NonNull Path path, int maxDepth) {
        return () -> {
            if (!Files.exists(this.directory.resolve(path))) { //prevent FileNotFoundException if the path doesn't exist
                return Stream.empty();
            }

            return Files.walk(this.directory.resolve(path), maxDepth)
                    .filter(Files::isRegularFile) //don't return directories as resources
                    .map(p -> p.relativize(this.directory)); //make paths relative to resource root
        };
    }
}
