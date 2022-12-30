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

package net.daporkchop.fp2.core.server.event;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.api.event.ReturningEvent;
import net.daporkchop.fp2.api.world.level.FBlockLevel;
import net.daporkchop.fp2.core.server.world.FBlockLevelHolder;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;

/**
 * Fired in order to retrieve the {@link FBlockLevelHolder} for accessing exact or rough block data in a given {@link IFarLevelServer}.
 * <p>
 * Note that this event is fired twice per level: once with a {@link T type parameter value} of {@link FBlockLevelHolder.Exact}, and once with
 * {@link FBlockLevelHolder.Rough}. Users wanting to inject their own exact/rough {@link FBlockLevelHolder} instances should only listen for events of the exact type
 * they're interested in - accidentally injecting a {@link FBlockLevelHolder.Rough rough level} as the {@link FBlockLevelHolder.Exact exact level} is a bug!
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public abstract class GetFBlockLevelHolderEvent<T extends FBlockLevelHolder> implements ReturningEvent<T> {
    /**
     * Creates a new {@link GetFBlockLevelHolderEvent} for getting a {@link IFarLevelServer level}'s {@link FBlockLevelHolder.Exact exact} {@link FBlockLevel}.
     *
     * @param level the {@link IFarLevelServer level}
     * @return a new instance of {@link GetFBlockLevelHolderEvent} with a generic parameter of {@link FBlockLevelHolder.Exact}
     */
    public static GetFBlockLevelHolderEvent<FBlockLevelHolder.Exact> createExact(@NonNull IFarLevelServer level) {
        return new GetFBlockLevelHolderEvent<FBlockLevelHolder.Exact>(level) {};
    }

    /**
     * Creates a new {@link GetFBlockLevelHolderEvent} for getting a {@link IFarLevelServer level}'s {@link FBlockLevelHolder.Rough rough} {@link FBlockLevel}.
     *
     * @param level the {@link IFarLevelServer level}
     * @return a new instance of {@link GetFBlockLevelHolderEvent} with a generic parameter of {@link FBlockLevelHolder.Rough}
     */
    public static GetFBlockLevelHolderEvent<FBlockLevelHolder.Rough> createRough(@NonNull IFarLevelServer level) {
        return new GetFBlockLevelHolderEvent<FBlockLevelHolder.Rough>(level) {};
    }

    @NonNull
    protected final IFarLevelServer level;
}
