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

package net.daporkchop.fp2.core;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.daporkchop.fp2.api.FP2;
import net.daporkchop.fp2.api.event.FEventBus;
import net.daporkchop.fp2.common.util.ResourceProvider;
import net.daporkchop.fp2.core.event.EventBus;
import net.daporkchop.lib.common.util.PorkUtil;
import net.daporkchop.lib.logging.Logger;

import java.lang.reflect.Method;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class FP2Core implements FP2 {
    public static final String MODID = FP2.MODID;

    /**
     * @return the current FP2 instance
     */
    public static FP2Core fp2() {
        return (FP2Core) FP2.fp2();
    }

    private final FEventBus eventBus = new EventBus();

    @Setter(AccessLevel.PROTECTED)
    private Logger log;

    @Setter(AccessLevel.PROTECTED)
    private Logger chat;

    @SneakyThrows
    protected FP2Core() {
        //initialize global fp2 instance
        Method method = PorkUtil.classForName(FP2.class.getCanonicalName() + "Holder").getDeclaredMethod("init", FP2.class);
        method.setAccessible(true);
        method.invoke(null, this);

        //register self as an event listener
        this.eventBus.register(this);
    }

    /**
     * @return a {@link ResourceProvider} which can load game resources
     */
    public abstract ResourceProvider resourceProvider();

    /**
     * @return whether or not the active game distribution contains a client
     */
    public abstract boolean hasClient();

    /**
     * @return whether or not the active game distribution contains a server
     */
    public abstract boolean hasServer();
}
