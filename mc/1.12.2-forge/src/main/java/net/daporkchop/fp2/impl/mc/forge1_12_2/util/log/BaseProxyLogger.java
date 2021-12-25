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

package net.daporkchop.fp2.impl.mc.forge1_12_2.util.log;

import lombok.NonNull;
import net.daporkchop.lib.logging.LogLevel;
import net.daporkchop.lib.logging.Logger;
import net.daporkchop.lib.logging.format.FormatParser;
import net.daporkchop.lib.logging.format.MessageFormatter;

import java.util.EnumSet;
import java.util.Set;

/**
 * @author DaPorkchop_
 */
public abstract class BaseProxyLogger implements Logger {
    @Override
    public Logger log(@NonNull LogLevel level, @NonNull String message) {
        this.log(level, null, message);
        return this;
    }

    protected abstract void log(@NonNull LogLevel level, String channel, @NonNull String message);

    @Override
    public FormatParser getFormatParser() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Logger setFormatParser(@NonNull FormatParser parser) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MessageFormatter getMessageFormatter() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Logger setMessageFormatter(@NonNull MessageFormatter formatter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<LogLevel> getLogLevels() {
        return EnumSet.allOf(LogLevel.class);
    }

    @Override
    public Logger setLogLevels(@NonNull Set<LogLevel> levels) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Logger channel(@NonNull String name) {
        return new BaseProxyLogger() {
            @Override
            public Logger log(@NonNull LogLevel level, @NonNull String message) {
                BaseProxyLogger.this.log(level, name, message);
                return this;
            }

            @Override
            protected void log(@NonNull LogLevel level, String channel, @NonNull String message) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
