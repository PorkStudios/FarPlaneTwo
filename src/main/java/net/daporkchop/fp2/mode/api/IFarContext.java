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

package net.daporkchop.fp2.mode.api;

import lombok.NonNull;
import net.daporkchop.fp2.mode.api.client.IFarRenderer;
import net.daporkchop.fp2.mode.api.piece.IFarPiece;
import net.daporkchop.fp2.mode.api.server.IFarPlayerTracker;
import net.daporkchop.fp2.mode.api.server.IFarWorld;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author DaPorkchop_
 */
public interface IFarContext<POS extends IFarPos, P extends IFarPiece, W extends IFarWorld<POS, P>, T extends IFarPlayerTracker<POS>, R extends IFarRenderer<POS, P>> {
    /**
     * Initializes this context.
     *
     * @param mode the new {@link IFarRenderMode} to use
     */
    void init(@NonNull IFarRenderMode<POS, P> mode);

    /**
     * @return whether or not this context has been initialized
     */
    boolean isInitialized();

    /**
     * @return the current {@link IFarRenderMode}
     * @throws IllegalStateException if this context has not yet been initialized
     */
    IFarRenderMode<POS, P> mode();

    /**
     * @return the current {@link IFarWorld}
     * @throws IllegalStateException if this context has not yet been initialized
     */
    W world();

    /**
     * @return the current {@link IFarRenderer}, or {@code null} if this context has not yet been initialized
     */
    @SideOnly(Side.CLIENT)
    R renderer();
}
