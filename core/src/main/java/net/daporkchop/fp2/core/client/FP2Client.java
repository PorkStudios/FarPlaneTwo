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

package net.daporkchop.fp2.core.client;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.gui.GuiContext;
import net.daporkchop.fp2.core.client.gui.GuiScreen;
import net.daporkchop.fp2.core.client.key.KeyCategory;
import net.daporkchop.fp2.core.client.player.IFarPlayerClient;
import net.daporkchop.fp2.core.client.shader.ShaderMacros;
import net.daporkchop.lib.logging.Logger;

import java.util.Optional;
import java.util.function.Function;

/**
 * @author DaPorkchop_
 */
@Getter
@Setter(AccessLevel.PROTECTED)
public abstract class FP2Client {
    private final ShaderMacros.Mutable globalShaderMacros = new ShaderMacros.Mutable();

    private Logger chat;

    /**
     * @return the {@link FP2Core} instance which this {@link FP2Client} is used for
     */
    public abstract FP2Core fp2();

    /**
     * Opens a new {@link GuiScreen}.
     *
     * @param factory a factory for creating a new {@link GuiScreen}
     * @param <T>     the {@link GuiScreen}
     * @return the created {@link GuiScreen}
     * @throws UnsupportedOperationException if the active game distribution does not contain a client
     */
    public abstract <T extends GuiScreen> T openScreen(@NonNull Function<GuiContext, T> factory);

    /**
     * Creates a new {@link KeyCategory}.
     *
     * @param localeKey the locale key of the category name
     * @return the created {@link KeyCategory}
     */
    public abstract KeyCategory createKeyCategory(@NonNull String localeKey);

    /**
     * @return the current {@link IFarPlayerClient}, or an empty {@link Optional} if the client is not connected
     */
    public abstract Optional<? extends IFarPlayerClient> currentPlayer();

    //TODO: somehow move Reversed-Z stuff out of FP2Client, and preferably into :gl
    @Deprecated
    public abstract void enableReverseZ();

    @Deprecated
    public abstract void disableReverseZ();

    @Deprecated
    public abstract boolean isReverseZ();

    @Deprecated
    public abstract int vanillaRenderDistanceChunks();
}
