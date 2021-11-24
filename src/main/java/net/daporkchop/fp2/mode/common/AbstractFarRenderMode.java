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
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.api.IFarDirectPosAccess;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.mode.api.ctx.IFarClientContext;
import net.daporkchop.fp2.mode.api.ctx.IFarServerContext;
import net.daporkchop.fp2.mode.api.ctx.IFarWorldClient;
import net.daporkchop.fp2.mode.api.ctx.IFarWorldServer;
import net.daporkchop.fp2.mode.api.player.IFarPlayerServer;
import net.daporkchop.fp2.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.core.util.SimpleRecycler;
import net.daporkchop.fp2.core.event.AbstractRegisterEvent;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.reference.ReferenceStrength;
import net.daporkchop.lib.common.reference.cache.Cached;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Arrays;
import java.util.Objects;

/**
 * Base implementation of {@link IFarRenderMode}.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class AbstractFarRenderMode<POS extends IFarPos, T extends IFarTile> implements IFarRenderMode<POS, T> {
    protected final IFarGeneratorExact.Factory<POS, T>[] exactGeneratorFactories = this.exactGeneratorFactoryEvent().fire().collectValues();
    protected final IFarGeneratorRough.Factory<POS, T>[] roughGeneratorFactories = this.roughGeneratorFactoryEvent().fire().collectValues();

    protected final Cached<SimpleRecycler<T>> recyclerRef = Cached.threadLocal(() -> new SimpleRecycler.OfReusablePersistent<>(this::newTile), ReferenceStrength.SOFT);

    @Getter(lazy = true)
    private final String name = REGISTRY.getName(this);
    @Getter
    protected final int storageVersion;

    protected abstract AbstractRegisterEvent<IFarGeneratorExact.Factory<POS, T>> exactGeneratorFactoryEvent();

    protected abstract AbstractRegisterEvent<IFarGeneratorRough.Factory<POS, T>> roughGeneratorFactoryEvent();

    protected abstract T newTile();

    @Override
    public IFarGeneratorExact<POS, T> exactGenerator(@NonNull WorldServer world) {
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
    public IFarGeneratorRough<POS, T> roughGenerator(@NonNull WorldServer world) {
        return Arrays.stream(this.roughGeneratorFactories)
                .map(f -> f.forWorld(world))
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    @Override
    public abstract IFarTileProvider<POS, T> tileProvider(@NonNull WorldServer world);

    @Override
    public abstract IFarServerContext<POS, T> serverContext(@NonNull IFarPlayerServer player, @NonNull IFarWorldServer world, @NonNull FP2Config config);

    @SideOnly(Side.CLIENT)
    @Override
    public abstract IFarClientContext<POS, T> clientContext(@NonNull IFarWorldClient world, @NonNull FP2Config config);

    @Override
    public SimpleRecycler<T> tileRecycler() {
        return this.recyclerRef.get();
    }

    @Override
    public abstract IFarDirectPosAccess<POS> directPosAccess();

    @Override
    public abstract POS readPos(@NonNull ByteBuf buf);

    @Override
    public abstract POS[] posArray(int length);

    @Override
    public abstract T[] tileArray(int length);
}
