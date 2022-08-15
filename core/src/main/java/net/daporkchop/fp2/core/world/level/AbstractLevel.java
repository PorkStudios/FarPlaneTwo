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
 */

package net.daporkchop.fp2.core.world.level;

import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.api.event.FEventBus;
import net.daporkchop.fp2.api.event.generic.load.FLoadedEvent;
import net.daporkchop.fp2.api.event.generic.load.FLoadingEvent;
import net.daporkchop.fp2.api.event.generic.load.FUnloadedEvent;
import net.daporkchop.fp2.api.event.generic.load.FUnloadingEvent;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.api.world.level.FLevel;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.event.EventBus;
import net.daporkchop.fp2.core.util.threading.workergroup.WorkerManager;
import net.daporkchop.fp2.core.world.AbstractWorld;
import net.daporkchop.lib.common.function.exception.ERunnable;
import net.daporkchop.lib.reflection.type.PTypes;

import java.util.concurrent.Callable;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Base implementation of {@link FLevel}.
 *
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractLevel<F extends FP2Core,
        IMPL_WORLD, WORLD extends AbstractWorld<F, IMPL_WORLD, WORLD, IMPL_LEVEL, LEVEL>,
        IMPL_LEVEL, LEVEL extends AbstractLevel<F, IMPL_WORLD, WORLD, IMPL_LEVEL, LEVEL>> implements FLevel {
    private final F fp2;
    private final IMPL_LEVEL implLevel;

    private final WORLD world;
    private final Identifier id;

    private final WorkerManager workerManager;

    private final FEventBus eventBus = new EventBus();

    public AbstractLevel(@NonNull F fp2, IMPL_LEVEL implLevel, @NonNull WORLD world, @NonNull Identifier id) {
        this.fp2 = fp2;
        this.implLevel = implLevel;
        this.world = world;
        this.id = id;

        this.workerManager = this.createWorkerManager();

        //fire FLoadingEvent<LEVEL> on world event bus
        this.world.eventBus().fireTyped((FLoadingEvent<?>) () -> this, PTypes.parameterized(FLoadingEvent.class, null, this.world.levelClass()));
    }

    protected final <T> T getForInit(@NonNull Callable<T> action) {
        try {
            return action.call();
        } catch (Exception e) {
            try (AbstractLevel<F, IMPL_WORLD, WORLD, IMPL_LEVEL, LEVEL> _this = this) {
                throw new RuntimeException("exception while initializing level", e);
            }
        }
    }

    protected final void runForInit(@NonNull ERunnable action) {
        try {
            action.runThrowing();
        } catch (Exception e) {
            try (AbstractLevel<F, IMPL_WORLD, WORLD, IMPL_LEVEL, LEVEL> _this = this) {
                throw new RuntimeException("exception while initializing level", e);
            }
        }
    }

    public final LEVEL init() {
        return this.getForInit(() -> {
            this.doInit();

            //fire FLoadedEvent<LEVEL>
            this.eventBus().fireTyped((FLoadedEvent<?>) () -> this, PTypes.parameterized(FLoadedEvent.class, null, this.world.levelClass()));

            return uncheckedCast(this);
        });
    }

    protected void doInit() throws Exception {
        //no-op
    }

    protected abstract WorkerManager createWorkerManager();

    @Override
    @SneakyThrows(Exception.class)
    public final void close() {
        try (AutoCloseable closeable = this::doClose) {
            //fire FUnloadingEvent<LEVEL>
            this.eventBus().fireTyped((FUnloadingEvent<?>) () -> this, PTypes.parameterized(FUnloadingEvent.class, null, this.world.levelClass()));
        } finally {
            //fire FUnloadedEvent<LEVEL>
            this.eventBus().fireTyped((FUnloadedEvent<?>) () -> this, PTypes.parameterized(FUnloadedEvent.class, null, this.world.levelClass()));
        }
    }

    protected void doClose() throws Exception {
        //no-op
    }
}
