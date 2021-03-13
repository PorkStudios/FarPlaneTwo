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

package net.daporkchop.fp2.mode.api.ctx;

import lombok.NonNull;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.function.Consumer;

/**
 * Provides access to {@link IFarServerContext} instances in a {@link WorldServer}.
 *
 * @author DaPorkchop_
 */
public interface IFarWorldServer {
    /**
     * Gets the {@link IFarServerContext} used by the given {@link IFarServerContext} in this world.
     *
     * @param mode the {@link IFarRenderMode}
     * @return the {@link IFarServerContext} used by the given {@link IFarServerContext} in this world
     */
    <POS extends IFarPos, T extends IFarTile> IFarServerContext<POS, T> contextFor(@NonNull IFarRenderMode<POS, T> mode);

    /**
     * Runs the given action on every {@link IFarServerContext}.
     *
     * @param action the action
     */
    void forEachContext(@NonNull Consumer<IFarServerContext<?, ?>> action);

    /**
     * Called when the world is being loaded.
     */
    void fp2_init();

    /**
     * Called when the world is being unloaded.
     */
    void close();
}
