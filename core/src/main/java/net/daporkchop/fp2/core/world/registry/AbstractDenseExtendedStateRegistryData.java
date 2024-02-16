/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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

package net.daporkchop.fp2.core.world.registry;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.world.level.BlockLevelConstants;
import net.daporkchop.fp2.api.world.registry.FExtendedStateRegistryData;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;

import java.util.IntSummaryStatistics;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Base implementation of {@link FExtendedStateRegistryData} for registries whose state IDs are allocated in an integer range without gaps.
 *
 * @author DaPorkchop_
 */
public abstract class AbstractDenseExtendedStateRegistryData<STATE> implements FExtendedStateRegistryData {
    @Getter
    private final FGameRegistry registry;

    private final byte[] types;
    private final byte[] lightAttrs;

    public AbstractDenseExtendedStateRegistryData(@NonNull FGameRegistry registry) {
        this.registry = registry;

        IntSummaryStatistics statistics = registry.states().summaryStatistics();
        Preconditions.checkArgument(statistics.getCount() == 0L || statistics.getMin() == 0, "registry contains invalid negative id %s", statistics.getMin());
        int length = Math.max(statistics.getMax() + 1, 0);
        byte[] types = new byte[length];
        byte[] lightAttrs = new byte[length];
        for (int id = 0; id < length; id++) {
            STATE state = uncheckedCast(registry.id2state(id));

            int type = this.type(id, state);
            int lightOpacity = this.lightOpacity(id, state);
            int lightEmission = this.lightEmission(id, state);
            Preconditions.checkState(BlockLevelConstants.isValidBlockType(type), "for %s (id %s): invalid block type %s", state, id, type);
            Preconditions.checkState(BlockLevelConstants.isValidLight(lightOpacity), "for %s (id %s): invalid light opacity %s", state, id, type);
            Preconditions.checkState(BlockLevelConstants.isValidLight(lightEmission), "for %s (id %s): invalid light emission %s", state, id, type);

            types[id] = (byte) type;
            lightAttrs[id] = BlockLevelConstants.packLightAttrs(lightOpacity, lightEmission);
        }
        this.types = types;
        this.lightAttrs = lightAttrs;
    }

    protected abstract int type(int id, STATE state);

    protected abstract int lightOpacity(int id, STATE state);

    protected abstract int lightEmission(int id, STATE state);

    @Override
    public final int type(int state) throws IndexOutOfBoundsException {
        return this.types[state];
    }

    @Override
    public final byte packedLightAttrs(int state) throws IndexOutOfBoundsException {
        return this.lightAttrs[state];
    }
}
