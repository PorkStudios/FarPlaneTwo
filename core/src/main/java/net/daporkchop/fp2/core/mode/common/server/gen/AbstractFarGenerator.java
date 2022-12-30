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

package net.daporkchop.fp2.core.mode.common.server.gen;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.world.registry.FExtendedBiomeRegistryData;
import net.daporkchop.fp2.api.world.registry.FExtendedStateRegistryData;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarGenerator;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;

/**
 * Base class containing common code shared among all generators.
 *
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractFarGenerator<POS extends IFarPos, T extends IFarTile> implements IFarGenerator<POS, T> {
    private final IFarLevelServer level;
    private final IFarTileProvider<POS, T> provider;

    private final FGameRegistry registry;
    private final FExtendedBiomeRegistryData extendedBiomeRegistryData;
    private final FExtendedStateRegistryData extendedStateRegistryData;

    private final int seaLevel;

    public AbstractFarGenerator(@NonNull IFarLevelServer level, @NonNull IFarTileProvider<POS, T> provider) {
        this.level = level;
        this.provider = provider;

        this.registry = level.registry();
        this.extendedBiomeRegistryData = this.registry.extendedBiomeRegistryData();
        this.extendedStateRegistryData = this.registry.extendedStateRegistryData();

        this.seaLevel = level.seaLevel();
    }
}
