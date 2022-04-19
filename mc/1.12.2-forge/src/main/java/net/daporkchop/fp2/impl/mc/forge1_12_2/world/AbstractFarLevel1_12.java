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

package net.daporkchop.fp2.impl.mc.forge1_12_2.world;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.common.util.Identifier;
import net.daporkchop.fp2.core.mode.api.ctx.IFarLevel;
import net.daporkchop.fp2.impl.mc.forge1_12_2.FP2Forge1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.world.registry.GameRegistry1_12_2;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public abstract class AbstractFarLevel1_12<W extends World> implements IFarLevel {
    @NonNull
    protected final FP2Forge1_12_2 fp2;
    @NonNull
    protected final W world;

    @Override
    public Object implWorld() {
        return this.world;
    }

    @Override
    public Identifier dimensionId() {
        int dimensionId = this.world.provider.getDimension();
        DimensionType dimensionType = this.world.provider.getDimensionType();

        //sanity check because i'm not entirely sure what kind of crazy shit mods do with dimension types, and i want to be sure not to screw anything up
        checkState(dimensionId == dimensionType.getId(), "dimension #%d has invalid ID: '%s' is expected to have ID %d", dimensionId, dimensionType.getName(), dimensionType.getId());

        return Identifier.fromLenient("minecraft", dimensionType.getName());
    }

    @Override
    public long timestamp() {
        return this.world.getTotalWorldTime();
    }

    @Override
    public GameRegistry1_12_2 registry() {
        return GameRegistry1_12_2.get();
    }
}
