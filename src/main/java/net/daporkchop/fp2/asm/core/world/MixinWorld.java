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

package net.daporkchop.fp2.asm.core.world;

import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.fp2.core.mode.api.ctx.IFarWorld;
import net.daporkchop.fp2.core.server.event.GetCoordinateLimitsEvent;
import net.daporkchop.fp2.impl.mc.forge1_12_2.world.registry.GameRegistry1_12_2;
import net.daporkchop.lib.common.util.PorkUtil;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Mixin(World.class)
public abstract class MixinWorld implements IFarWorld {
    @Unique
    protected IntAxisAlignedBB fp2_coordLimits;

    @Unique
    protected boolean isInitialized() {
        return this.fp2_coordLimits != null;
    }

    @Override
    public Object fp2_IFarWorld_implWorld() {
        return this;
    }

    @Override
    public IntAxisAlignedBB fp2_IFarWorld_coordLimits() {
        checkState(this.isInitialized(), "not initialized!");
        return this.fp2_coordLimits;
    }

    @Override
    public void fp2_IFarWorld_init() {
        checkState(!this.isInitialized(), "already initialized!");

        this.fp2_coordLimits = fp2().eventBus().fireAndGetFirst(new GetCoordinateLimitsEvent(uncheckedCast(this))).get();
    }

    @Override
    public int fp2_IFarWorld_dimensionId() {
        return PorkUtil.<World>uncheckedCast(this).provider.getDimension();
    }

    @Shadow
    public abstract long getTotalWorldTime();

    @Override
    public long fp2_IFarWorld_timestamp() {
        return this.getTotalWorldTime();
    }

    @Override
    public FGameRegistry fp2_IFarWorld_registry() {
        return GameRegistry1_12_2.get();
    }

    @Shadow
    public abstract int getSeaLevel();
}
