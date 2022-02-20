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

package net.daporkchop.fp2.resources.translate;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.resources.FResources;
import net.daporkchop.lib.common.function.io.IOSupplier;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class AbstractTranslatingFResources implements FResources {
    @NonNull
    private final FResources next;

    @Override
    public Optional<IOSupplier<InputStream>> getResource(@NonNull Path path) {
        return this.getResource(path, this.next);
    }

    protected abstract Optional<IOSupplier<InputStream>> getResource(@NonNull Path path, @NonNull FResources next);

    @Override
    public IOSupplier<Stream<Path>> listResources() {
        return this.listResources(this.next);
    }

    protected abstract IOSupplier<Stream<Path>> listResources(@NonNull FResources next);

    @Override
    public IOSupplier<Stream<Path>> listResources(@NonNull Path path, int maxDepth) {
        return this.listResources(path, maxDepth, this.next);
    }

    protected IOSupplier<Stream<Path>> listResources(@NonNull Path path, int maxDepth, @NonNull FResources next) {
        return FResources.super.listResources(path, maxDepth);
    }
}
