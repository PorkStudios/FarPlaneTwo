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

import net.daporkchop.fp2.api.event.Constrain;
import net.daporkchop.fp2.api.event.FEventBus;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.api.event.ReturningEvent;
import net.daporkchop.fp2.core.event.EventBus;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Optional;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class TestEventBus {
    @Test
    public void testFire() {
        class Events {
            private int object = 0;
            private int baseString = 0;

            private int eventBaseWildcard = 0;

            @FEventHandler
            private void handleObject(Object event) {
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
        }
        Events events = new Events();

        FEventBus eventBus = new EventBus();
        eventBus.register(events);
        checkState(events.object == 0 && events.baseString == 0);
        eventBus.fire(new Object());
        checkState(events.object == 1 && events.baseString == 0);
        eventBus.fire(new Base<String>() {});
        checkState(events.object == 2 && events.baseString == 1);

        checkState(events.eventBaseWildcard == 0);
        eventBus.fire(new Event<Base<?>>() {});
        checkState(events.eventBaseWildcard == 1);

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

        eventBus.fire("!!! this should not be printed !!!");
        eventBus.registerWeak(listener);
        eventBus.fire("hello world");

        listener = null;
        do {
            System.gc();
        } while (listenerReference.get() != null);

        eventBus.fire("!!! this should not be printed !!!");
    }

    @FEventHandler
    public void handleReturningAsVoid(ReturningEvent<Map<String, Optional<? extends CharSequence>>> event) {
    }

    @FEventHandler
    public Map<String, Optional<? extends CharSequence>> handleReturningAsT(ReturningEvent<Map<String, Optional<? extends CharSequence>>> event) {
        return null;
    }

    @FEventHandler
    public Optional<Map<String, Optional<? extends CharSequence>>> handleReturningAsOptionalT(ReturningEvent<Map<String, Optional<? extends CharSequence>>> event) {
        return Optional.empty();
    }

    @Test
    public void testOrdering() {
        final int NOT_MONITOR_COUNT = 3;

        class Events {
            int idx = 0;

            @FEventHandler(name = "first")
            public void first(String s) {
                checkState(this.idx++ == 0, this.idx);
            }

            @FEventHandler(name = "second",
                    constrain = @Constrain(after = "first", before = "third"))
            public void second(String s) {
                checkState(this.idx++ == 1, this.idx);
            }

            @FEventHandler(name = "third")
            public void third(String s) {
                checkState(this.idx++ == 2, this.idx);
            }

            @FEventHandler(name = "monitor_third", constrain = @Constrain(monitor = true))
            public void monitor_third(String s) {
                checkState(this.idx++ == NOT_MONITOR_COUNT + 2, this.idx);
            }

            @FEventHandler(name = "monitor_second",
                    constrain = @Constrain(after = "monitor_first", before = "monitor_third", monitor = true))
            public void monitor_second(String s) {
                checkState(this.idx++ == NOT_MONITOR_COUNT + 1, this.idx);
            }

            @FEventHandler(name = "monitor_first", constrain = @Constrain(monitor = true))
            public void monitor_first(String s) {
                checkState(this.idx++ == NOT_MONITOR_COUNT + 0, this.idx);
            }
        }

        FEventBus eventBus = new EventBus();
        eventBus.register(new Events());
        eventBus.fire("");
    }

    @Test
    public void testReturning() {
        class Events {
            @FEventHandler(name = "first")
            public void first(ReturningEvent<String> event) {
                //no-op
            }

            @FEventHandler(name = "second", constrain = @Constrain(after = "first"))
            public Optional<String> second(ReturningEvent<String> event) {
                return Optional.of("second");
            }

            @FEventHandler(name = "third", constrain = @Constrain(after = "second"))
            public String third(ReturningEvent<String> event) {
                return "third";
            }

            @FEventHandler(constrain = @Constrain(monitor = true))
            public void monitor(ReturningEvent<String> event) {
                System.out.println("monitor");
            }
        }

        ReturningEvent<String> event = new ReturningEvent<String>() {};

        FEventBus eventBus = new EventBus();

        System.out.println(eventBus.fireAndGetFirst(event));
        System.out.println(eventBus.fireAndGetAll(event));

        eventBus.register(new Events());

        System.out.println(eventBus.fireAndGetFirst(event));
        System.out.println(eventBus.fireAndGetAll(event));
    }

    private interface Base<T> {
    }

    private interface Event<T> {
    }
}
