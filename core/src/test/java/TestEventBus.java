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

import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.core.FP2Core;
import org.junit.Test;

import java.lang.ref.WeakReference;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class TestEventBus {
    private int object = 0;
    private int baseString = 0;

    private int eventBaseWildcard = 0;

    @Test
    public void test() {
        FP2Core.fp2().eventBus().register(this);
        checkState(this.object == 0 && this.baseString == 0);
        FP2Core.fp2().eventBus().fire(new Object());
        checkState(this.object == 1 && this.baseString == 0);
        FP2Core.fp2().eventBus().fire(new Base<String>() {});
        checkState(this.object == 2 && this.baseString == 1);

        checkState(this.eventBaseWildcard == 0);
        FP2Core.fp2().eventBus().fire(new Event<Base<?>>() {});
        checkState(this.eventBaseWildcard == 1);

        Object listener = new Object() {
            @FEventHandler
            public void handleString(String event) {
                if ("!!! this should not be printed !!!".equals(event)) {
                    throw new IllegalStateException();
                }

                System.out.println("handled String event: " + event);
            }
        };
        WeakReference<Object> listenerReference = new WeakReference<>(listener);

        FP2Core.fp2().eventBus().fire("!!! this should not be printed !!!");
        FP2Core.fp2().eventBus().registerWeak(listener);
        FP2Core.fp2().eventBus().fire("hello world");

        listener = null;
        do {
            System.gc();
        } while (listenerReference.get() != null);

        FP2Core.fp2().eventBus().fire("!!! this should not be printed !!!");
    }

    @FEventHandler
    public void handleObject(Object event) {
        this.object++;
    }

    @FEventHandler
    public void handleBaseString(Base<String> event) {
        this.baseString++;
    }

    @FEventHandler
    public void handleEventBaseWildcard(Event<Base<?>> event) {
        this.eventBaseWildcard++;
    }

    private interface Base<T> {
    }

    private interface Event<T> {
    }
}
