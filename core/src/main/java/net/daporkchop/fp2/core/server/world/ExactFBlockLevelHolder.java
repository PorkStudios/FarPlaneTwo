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

package net.daporkchop.fp2.core.server.world;

import lombok.NonNull;
import net.daporkchop.fp2.api.world.level.FBlockLevel;
import net.daporkchop.fp2.api.world.level.FBlockLevelDataAvailability;
import net.daporkchop.fp2.common.util.capability.CloseableResource;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;

/**
 * A container which provides instances of {@link FBlockLevel} for accessing rough block data in a {@link IFarLevelServer}.
 *
 * @author DaPorkchop_
 */
public interface ExactFBlockLevelHolder extends FBlockLevelDataAvailability, CloseableResource {
    /**
     * Gets an {@link FBlockLevel} instance for given requirement of whether or not generation should be allowed.
     *
     * @param requirement whether or not generation should be allowed
     * @return an {@link FBlockLevel} instance matching the given requirement
     */
    FBlockLevel worldFor(@NonNull AllowGenerationRequirement requirement);

    /**
     * {@inheritDoc}
     * <p>
     * When this holder is closed, all the {@link FBlockLevel} instances created by it will produce undefined behavior, even if not yet closed.
     */
    @Override
    void close();

    /**
     * Controls whether or not generation should be allowed when requesting an {@link FBlockLevel} instance.
     *
     * @author DaPorkchop_
     */
    enum AllowGenerationRequirement {
        ALLOWED,
        NOT_ALLOWED,
        DONT_CARE;
    }
}
