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

package net.daporkchop.fp2.api.event;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restricts the order in which {@link FEventHandler}s can be executed when multiple ones are listening for the same event.
 *
 * @author DaPorkchop_
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Constrain {
    /**
     * Adds a dependency on handler(s) with the given name. If any other handler with the given name is found, it will be executed before this handler.
     *
     * @return the name(s) of the handler(s) which should run before this handler
     */
    String[] before() default {};

    /**
     * Adds a reverse dependency on handler(s) with the given name. If any other handler with the given name is found, it will be executed after this handler.
     *
     * @return the name(s) of the handler(s) which should run after this handler
     */
    String[] after() default {};

    /**
     * Marks this handler as a monitor. Monitors are always notified last.
     * <p>
     * If {@code true}, adds a dependency on all handler(s) for which {@link #monitor()} is {@code false}.
     * <p>
     * This is intended for handlers which want to consume the output of an event without modifying it. It should not be used as a lazy solution to make a "normal" handler run later than
     * another one; that should be done using {@link #after()}.
     *
     * @return whether or not this handler is a monitor
     */
    boolean monitor() default false;
}
