/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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

package net.daporkchop.fp2.core.mode.api.ctx;

import lombok.NonNull;
import net.daporkchop.fp2.api.world.FBlockWorld;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarRenderMode;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.mode.api.server.IFarTileProvider;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * @author DaPorkchop_
 */
public interface IFarWorldServer extends IFarWorld {
    /**
     * Gets the {@link IFarTileProvider} used by the given {@link IFarRenderMode} in this world.
     *
     * @param mode the {@link IFarRenderMode}
     * @return the {@link IFarTileProvider} used by the given {@link IFarRenderMode} in this world
     */
    <POS extends IFarPos, T extends IFarTile> IFarTileProvider<POS, T> fp2_IFarWorldServer_tileProviderFor(@NonNull IFarRenderMode<POS, T> mode);

    /**
     * Runs the given action on every {@link IFarTileProvider}.
     *
     * @param action the action
     */
    void fp2_IFarWorldServer_forEachTileProvider(@NonNull Consumer<IFarTileProvider<?, ?>> action);

    /**
     * @return the world's root directory
     */
    Path fp2_IFarWorldServer_worldDirectory();

    /**
     * @return the vanilla terrain generator instance
     */
    Object fp2_IFarWorldServer_getVanillaGenerator();

    /**
     * @return a {@link FBlockWorld} for accessing this world's data
     */
    FBlockWorld fp2_IFarWorldServer_fblockWorld();

    /**
     * @return the sea level
     */
    int fp2_IFarWorldServer_seaLevel();
}
