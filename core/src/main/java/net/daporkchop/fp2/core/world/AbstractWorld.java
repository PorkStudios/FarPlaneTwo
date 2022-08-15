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

package net.daporkchop.fp2.core.world;

import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.api.event.FEventBus;
import net.daporkchop.fp2.api.event.generic.load.FLoadedEvent;
import net.daporkchop.fp2.api.event.generic.load.FLoadingEvent;
import net.daporkchop.fp2.api.event.generic.load.FUnloadedEvent;
import net.daporkchop.fp2.api.event.generic.load.FUnloadingEvent;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.api.world.FWorld;
import net.daporkchop.fp2.api.world.level.FLevel;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.event.EventBus;
import net.daporkchop.fp2.core.world.level.AbstractLevel;
import net.daporkchop.lib.common.function.exception.ERunnable;
import net.daporkchop.lib.common.util.GenericMatcher;
import net.daporkchop.lib.reflection.type.PTypes;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Base implementation of {@link FWorld}.
 * <p>
 * {@link FLevel}s belonging to this world are always a subtype of {@link AbstractLevel}. They are loaded by {@link #loadLevel(Identifier, Object)}, and are unloaded
 * by {@link #unloadLevel(Identifier)}.
 *
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractWorld<F extends FP2Core,
        IMPL_WORLD, WORLD extends AbstractWorld<F, IMPL_WORLD, WORLD, IMPL_LEVEL, LEVEL>,
        IMPL_LEVEL, LEVEL extends AbstractLevel<F, IMPL_WORLD, WORLD, IMPL_LEVEL, LEVEL>> implements FWorld {
    @NonNull
    private final F fp2;
    private final IMPL_WORLD implWorld;

    @Getter(lazy = true)
    private final Class<WORLD> worldClass = GenericMatcher.uncheckedFind(this.getClass(), AbstractWorld.class, "WORLD");
    @Getter(lazy = true)
    private final Class<LEVEL> levelClass = GenericMatcher.uncheckedFind(this.getClass(), AbstractWorld.class, "LEVEL");

    private final FEventBus eventBus = new EventBus();

    private final Map<Identifier, LEVEL> loadedLevels = new ConcurrentHashMap<>();

    public AbstractWorld(@NonNull F fp2, IMPL_WORLD implWorld) {
        this.fp2 = fp2;
        this.implWorld = implWorld;

        //fire FLoadingEvent<WORLD> on global event bus
        this.fp2().eventBus().fireTyped((FLoadingEvent<?>) () -> this, PTypes.parameterized(FLoadingEvent.class, null, this.worldClass()));
    }

    protected final <T> T getForInit(@NonNull Callable<T> action) {
        try {
            return action.call();
        } catch (Exception e) {
            try (AbstractWorld<F, IMPL_WORLD, WORLD, IMPL_LEVEL, LEVEL> _this = this) {
                throw new RuntimeException("exception while initializing world", e);
            }
        }
    }

    protected final void runForInit(@NonNull ERunnable action) {
        try {
            action.runThrowing();
        } catch (Exception e) {
            try (AbstractWorld<F, IMPL_WORLD, WORLD, IMPL_LEVEL, LEVEL> _this = this) {
                throw new RuntimeException("exception while initializing world", e);
            }
        }
    }

    public WORLD init() {
        return this.getForInit(() -> {
            this.doInit();

            //fire FLoadedEvent<WORLD>
            this.eventBus().fireTyped((FLoadedEvent<?>) () -> this, PTypes.parameterized(FLoadedEvent.class, null, this.worldClass()));

            return uncheckedCast(this);
        });
    }

    protected void doInit() throws Exception {
        //no-op
    }

    @Override
    @SneakyThrows(Exception.class)
    public final void close() {
        try (AutoCloseable closeable = this::doClose) {
            //fire FUnloadingEvent<WORLD>
            this.eventBus().fireTyped((FUnloadingEvent<?>) () -> this, PTypes.parameterized(FUnloadingEvent.class, null, this.worldClass()));
        } finally {
            //fire FUnloadedEvent<WORLD>
            this.eventBus().fireTyped((FUnloadedEvent<?>) () -> this, PTypes.parameterized(FUnloadedEvent.class, null, this.worldClass()));
        }
    }

    protected void doClose() throws Exception {
        if (!this.loadedLevels.isEmpty()) { //some levels are still loaded! this isn't allowed, but we'll try to unload them anyway
            IllegalStateException exception = new IllegalStateException("closed a world while it still had level(s) loaded!");
            for (Identifier id : this.loadedLevels.keySet()) { //unload each level
                try {
                    this.unloadLevel(id);
                } catch (Exception e) { //unloading the level failed, add the cause as a suppressed exception
                    exception.addSuppressed(e);
                }
            }
            throw exception;
        }
    }

    public LEVEL loadLevel(@NonNull Identifier _id, IMPL_LEVEL implLevel) {
        return this.loadedLevels.compute(_id, (id, level) -> {
            checkState(level == null, "level %s is already loaded!", id);

            return this.createLevel(id, implLevel).init();
        });
    }

    /**
     * Creates a new instance of {@link LEVEL} with the given {@link Identifier} and {@link IMPL_LEVEL implementation-specific level data}.
     * <p>
     * The method must only invoke the {@link LEVEL}'s constructor, but not {@link LEVEL#init() initialize} the {@link LEVEL}.
     *
     * @param id        the level's {@link Identifier}
     * @param implLevel the level's {@link IMPL_LEVEL implementation-specific level data}
     * @return a new, uninitialized {@link LEVEL}
     */
    protected abstract LEVEL createLevel(@NonNull Identifier id, IMPL_LEVEL implLevel);

    public void unloadLevel(@NonNull Identifier _id) {
        class State implements BiFunction<Identifier, LEVEL, LEVEL> {
            Exception e;

            @Override
            public LEVEL apply(Identifier id, LEVEL level) {
                try {
                    checkState(level != null, "level %s isn't loaded!", id);

                    //actually close the level
                    level.close();
                } catch (Exception e) {
                    this.e = e; //save to rethrow later once the entry has been removed from the map
                }
                return null; //remove entry from map
            }
        }

        State state = new State();
        this.loadedLevels.compute(_id, state);
        if (state.e != null) {
            PUnsafe.throwException(state.e);
        }
    }
}
