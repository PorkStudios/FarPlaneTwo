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

package net.daporkchop.fp2.core.server.world;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.storage.FStorage;
import net.daporkchop.fp2.api.storage.external.FStorageCategory;
import net.daporkchop.fp2.api.world.FWorldServer;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.server.world.level.AbstractLevelServer;
import net.daporkchop.fp2.core.storage.rocks.RocksStorage;
import net.daporkchop.fp2.core.world.AbstractWorld;

import java.nio.file.Path;

/**
 * Base implementation of {@link FWorldServer}.
 *
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractWorldServer<F extends FP2Core,
        IMPL_WORLD, WORLD extends AbstractWorldServer<F, IMPL_WORLD, WORLD, IMPL_LEVEL, LEVEL>,
        IMPL_LEVEL, LEVEL extends AbstractLevelServer<F, IMPL_WORLD, WORLD, IMPL_LEVEL, LEVEL>> extends AbstractWorld<F, IMPL_WORLD, WORLD, IMPL_LEVEL, LEVEL> implements FWorldServer {
    private final FStorage storage;

    public AbstractWorldServer(@NonNull F fp2, IMPL_WORLD implWorld, @NonNull Path path) {
        super(fp2, implWorld);

        this.storage = this.getForInit(() -> RocksStorage.open(path.resolve("fp2")));
    }

    @Override
    protected void doClose() throws Exception {
        //noinspection Convert2MethodRef
        try (AutoCloseable closeSuper = () -> super.doClose();
             FStorage storage = this.storage) {
            //no-op
        }
    }

    @Override
    public FStorageCategory storageCategory() {
        return this.storage;
    }
}
