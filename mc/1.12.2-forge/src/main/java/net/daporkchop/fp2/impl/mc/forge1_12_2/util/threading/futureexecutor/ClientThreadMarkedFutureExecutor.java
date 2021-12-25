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

package net.daporkchop.fp2.impl.mc.forge1_12_2.util.threading.futureexecutor;

import lombok.NonNull;
import net.daporkchop.fp2.core.util.threading.futureexecutor.AbstractMarkedFutureExecutor;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author DaPorkchop_
 */
@SideOnly(Side.CLIENT)
public class ClientThreadMarkedFutureExecutor extends AbstractMarkedFutureExecutor {
    /**
     * Gets the {@link ClientThreadMarkedFutureExecutor} for the given {@link Minecraft} instance.
     *
     * @param mc the {@link Minecraft} instance
     * @return the corresponding {@link ClientThreadMarkedFutureExecutor}
     */
    public static ClientThreadMarkedFutureExecutor getFor(@NonNull Minecraft mc) {
        return ((Holder) mc).fp2_ClientThreadMarkedFutureExecutor$Holder_get();
    }

    /**
     * @deprecated internal API, do not touch!
     */
    @Deprecated
    public ClientThreadMarkedFutureExecutor(@NonNull Minecraft mc) {
        super(mc.thread);
        this.start();
    }

    /**
     * @author DaPorkchop_
     * @deprecated internal API, do not touch!
     */
    @SideOnly(Side.CLIENT)
    @Deprecated
    public interface Holder {
        ClientThreadMarkedFutureExecutor fp2_ClientThreadMarkedFutureExecutor$Holder_get();
    }
}
