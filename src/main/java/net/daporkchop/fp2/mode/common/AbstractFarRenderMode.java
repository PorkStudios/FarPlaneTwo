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

package net.daporkchop.fp2.mode.common;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.piece.IFarPiece;
import net.daporkchop.fp2.mode.api.server.IFarWorld;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.util.SimpleRecycler;
import net.daporkchop.fp2.util.event.AbstractOrderedRegistryEvent;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.ref.Ref;
import net.daporkchop.lib.common.ref.ThreadRef;
import net.minecraft.world.WorldServer;

import java.util.Arrays;
import java.util.Objects;

/**
 * Base implementation of {@link IFarRenderMode}.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class AbstractFarRenderMode<POS extends IFarPos, P extends IFarPiece> implements IFarRenderMode<POS, P> {
    protected final IFarGeneratorExact.Factory<POS, P>[] exactGeneratorFactories = this.exactGeneratorFactoryEvent().fire().collectValues();
    protected final IFarGeneratorRough.Factory<POS, P>[] roughGeneratorFactories = this.roughGeneratorFactoryEvent().fire().collectValues();

    protected final Ref<SimpleRecycler<P>> recyclerRef = ThreadRef.soft(() -> new SimpleRecycler.OfReusablePersistent<>(this::newTile));

    @Getter(lazy = true)
    private final String name = REGISTRY.getName(this);
    @Getter
    protected final int storageVersion;

    protected abstract AbstractOrderedRegistryEvent<IFarGeneratorExact.Factory<POS, P>> exactGeneratorFactoryEvent();

    protected abstract AbstractOrderedRegistryEvent<IFarGeneratorRough.Factory<POS, P>> roughGeneratorFactoryEvent();

    protected abstract P newTile();

    @Override
    public IFarGeneratorExact<POS, P> exactGenerator(@NonNull WorldServer world) {
        return Arrays.stream(this.exactGeneratorFactories)
                .map(f -> f.forWorld(world))
                .filter(Objects::nonNull)
                .findFirst().orElseThrow(() -> new IllegalStateException(PStrings.fastFormat(
                        "No exact generator could be found for world %d (type: %s), mode:%s",
                        world.provider.getDimension(),
                        world.getWorldType(),
                        this.name()
                )));
    }

    @Override
    public IFarGeneratorRough<POS, P> roughGenerator(@NonNull WorldServer world) {
        return Arrays.stream(this.roughGeneratorFactories)
                .map(f -> f.forWorld(world))
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    @Override
    public abstract IFarWorld<POS, P> world(@NonNull WorldServer world);

    @Override
    public SimpleRecycler<P> tileRecycler() {
        return this.recyclerRef.get();
    }

    @Override
    public abstract POS readPos(@NonNull ByteBuf buf);

    @Override
    public abstract POS[] posArray(int length);

    @Override
    public abstract P[] tileArray(int length);
}
