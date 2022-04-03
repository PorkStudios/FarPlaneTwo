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

package net.daporkchop.fp2.impl.mc.forge1_16.world;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.common.util.Identifier;
import net.daporkchop.fp2.core.mode.api.ctx.IFarWorld;
import net.daporkchop.fp2.impl.mc.forge1_16.FP2Forge1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.world.registry.GameRegistry1_16;
import net.minecraft.world.World;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractFarWorld1_16<W extends World> implements IFarWorld {
    protected final FP2Forge1_16 fp2;
    protected final W world;

    protected final GameRegistry1_16 registry;

    public AbstractFarWorld1_16(@NonNull FP2Forge1_16 fp2, @NonNull W world) {
        this.fp2 = fp2;
        this.world = world;

        this.registry = new GameRegistry1_16(world);
    }

    @Override
    public Object fp2_IFarWorld_implWorld() {
        return this.world;
    }

    @Override
    public Identifier fp2_IFarWorld_dimensionId() {
        return Identifier.from(this.world.dimension().location().toString());
    }

    @Override
    public long fp2_IFarWorld_timestamp() {
        return this.world.getGameTime();
    }

    @Override
    public GameRegistry1_16 fp2_IFarWorld_registry() {
        return this.registry;
    }
}
