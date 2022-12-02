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

package net.daporkchop.fp2.api.world.level.query;

import lombok.Builder;
import lombok.Data;
import net.daporkchop.fp2.api.world.level.BlockLevelConstants;
import net.daporkchop.lib.common.annotation.param.NotNegative;

import java.util.Collections;
import java.util.List;

import static net.daporkchop.fp2.api.world.level.BlockLevelConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Describes which transitions are of interest to a type transition search query.
 *
 * @author DaPorkchop_
 */
@Data
@Builder
public final class TypeTransitionFilter {
    private static final TypeTransitionFilter OUTPUT_NOTHING_FILTER = new TypeTransitionFilter(blockTypeFlags(), blockTypeFlags(), -1, -1);
    private static final TypeTransitionFilter OUTPUT_EVERYTHING_FILTER = new TypeTransitionFilter(allBlockTypes(), allBlockTypes(), -1, -1);
    
    private static final List<TypeTransitionFilter> OUTPUT_NOTHING_FILTER_LIST = Collections.singletonList(OUTPUT_NOTHING_FILTER);
    private static final List<TypeTransitionFilter> OUTPUT_EVERYTHING_FILTER_LIST = Collections.singletonList(OUTPUT_EVERYTHING_FILTER);

    /**
     * @return a {@link TypeTransitionFilter} which matches nothing
     */
    public static TypeTransitionFilter outputNothingFilter() {
        return OUTPUT_NOTHING_FILTER;
    }

    /**
     * @return a {@link List} containing a single {@link TypeTransitionFilter} which {@link #outputNothingFilter() matches nothing}
     */
    public static List<TypeTransitionFilter> outputNothingFilterList() {
        return OUTPUT_NOTHING_FILTER_LIST;
    }

    /**
     * @return a {@link TypeTransitionFilter} which matches everything
     */
    public static TypeTransitionFilter outputEverythingFilter() {
        return OUTPUT_EVERYTHING_FILTER;
    }

    /**
     * @return a {@link List} containing a single {@link TypeTransitionFilter} which {@link #outputEverythingFilter() matches everything}
     */
    public static List<TypeTransitionFilter> outputEverythingFilterList() {
        return OUTPUT_EVERYTHING_FILTER_LIST;
    }

    /**
     * A bitfield indicating which block types are allowed as the block type being transitioned from. Transitions from block types not included in this bitfield will
     * be ignored.
     * <p>
     * The bitfield must be non-empty (i.e. at least one bit must be set). It may be {@link BlockLevelConstants#allBlockTypes() full}, in which case transitions from
     * any block type will be included.
     */
    private final int fromTypes;

    /**
     * A bitfield indicating which block types are allowed as the block type being transitioned to. Transitions to block types not included in this bitfield will
     * be ignored.
     * <p>
     * The bitfield must be non-empty (i.e. at least one bit must be set). It may be {@link BlockLevelConstants#allBlockTypes() full}, in which case transitions from
     * any block type will be included.
     */
    private final int toTypes;

    /**
     * Indicates the maximum number of hits matching this filter to include in the output. Once this number of transitions matching this filter have been output, any
     * subsequent hits matching this filter will not be included in the output.
     * <p>
     * Special values:<br>
     * <ul>
     *     <li>if {@code < 0}, the filter will be allowed to write an unlimited number of output values without being disabled.</li>
     *     <li>if {@code == 0}, transitions matching this filter will never be included in the output <i>(unless they also match another filter)</i>. This option is
     *     intended to be used with {@link #abortAfterHitCount}, in order to permit filters which serve only to abort the query once hit some number of times without
     *     affecting the output.</li>
     * </ul>
     */
    private final int disableAfterHitCount;

    /**
     * Indicates the maximum number of hits matching this filter to allow. Once this filter has been hit this number of times, the query will be aborted and no
     * further values will be written to the output.
     * <p>
     * Note that transitions which match this filter but are not written to the output due to the value of {@link #disableAfterHitCount} being exceeded still count
     * towards the total number of hits.
     * <p>
     * Special values:<br>
     * <ul>
     *     <li>if {@code < 0}, the filter will be allowed to be hit an unlimited number of times without aborting the query.</li>
     *     <li>if {@code == 0}, the query will be aborted immediately.</li>
     * </ul>
     */
    private final int abortAfterHitCount;

    private TypeTransitionFilter(int fromTypes, int toTypes, int disableAfterHitCount, int abortAfterHitCount) {
        checkArg(isValidBlockTypeSet(fromTypes), "invalid bitfield in fromTypes: %d", fromTypes);
        checkArg(isValidBlockTypeSet(toTypes), "invalid bitfield in toTypes: %d", toTypes);

        this.fromTypes = fromTypes;
        this.toTypes = toTypes;
        this.disableAfterHitCount = disableAfterHitCount;
        this.abortAfterHitCount = abortAfterHitCount;
    }

    /**
     * Checks whether a transition from the given type to the given type matches this filter.
     *
     * @param fromType the type being transitioned from
     * @param toType   the type being transitioned to
     * @return whether a transition from the given type to the given type matches this filter
     */
    public boolean transitionMatches(int fromType, int toType) {
        return isBlockTypeEnabled(this.fromTypes, fromType)
               && isBlockTypeEnabled(this.toTypes, toType);
    }

    /**
     * Checks whether this filter should be disabled for the remainder of the current query.
     *
     * @param hitCount the number of times that this filter has been hit so far
     * @return whether this filter should be disabled
     */
    public boolean shouldDisable(@NotNegative int hitCount) {
        return this.disableAfterHitCount == 0
               || (this.disableAfterHitCount > 0 && this.disableAfterHitCount < hitCount);
    }

    /**
     * Checks whether the current query should be aborted.
     *
     * @param hitCount the number of times that this filter has been hit so far
     * @return whether the current query should be aborted
     */
    public boolean shouldAbort(@NotNegative int hitCount) {
        return this.disableAfterHitCount == 0
               || (this.disableAfterHitCount > 0 && this.disableAfterHitCount < hitCount);
    }
}
