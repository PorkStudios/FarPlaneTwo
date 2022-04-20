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

package net.daporkchop.fp2.core.world;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.event.FEventBus;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.api.world.FWorld;
import net.daporkchop.fp2.api.world.level.FLevel;
import net.daporkchop.fp2.core.event.EventBus;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Base implementation of {@link FWorld}.
 *
 * @author DaPorkchop_
 */
public abstract class AbstractWorld implements FWorld {
    protected final Map<Identifier, FLevel> loadedLevels = new ConcurrentHashMap<>();

    @Getter
    protected final FEventBus eventBus = new EventBus();

    @Override
    public FLevel loadLevel(@NonNull Identifier idIn) {
        return this.loadedLevels.compute(idIn, (id, level) -> {
            checkState(level == null, "level %s is already loaded!", id);
            return this.createLevel(id);
        });
    }

    protected abstract FLevel createLevel(@NonNull Identifier id);

    @Override
    public void unloadLevel(@NonNull Identifier idIn) throws NoSuchElementException {
        this.loadedLevels.compute(idIn, (id, level) -> {
            if (level == null) {
                throw new NoSuchElementException("level isn't loaded: " + id);
            }

            level.close();
            return null; //return null to remove the level from the map
        });
    }
}
