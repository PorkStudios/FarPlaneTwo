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

package net.daporkchop.fp2.core.util;

import lombok.NonNull;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.*;
import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * A registry of locale keys to localized {@link String}s.
 *
 * @author DaPorkchop_
 */
public interface I18n {
    /**
     * Checks whether or not this registry contains the given locale key.
     *
     * @param key the locale key
     * @return whether or not this registry contains the given locale key
     */
    boolean hasKey(@NonNull String key);

    /**
     * Retrieves the localized {@link String} for the given locale key and formats it with the given formatter arguments.
     * <p>
     * If the given locale key is not known, the key is returned.
     *
     * @param key the locale key
     * @return the formatted {@link String}, or the locale key
     */
    String format(@NonNull String key);

    /**
     * Retrieves the localized {@link String} for the given locale key and formats it with the given formatter arguments.
     * <p>
     * If the given locale key is not known, the key is returned.
     *
     * @param key  the locale key
     * @param args the formatter arguments
     * @return the formatted {@link String}, or the locale key
     */
    String format(@NonNull String key, @NonNull Object... args);

    /**
     * @return the current {@link Locale}
     */
    Locale javaLocale();

    /**
     * @return a {@link NumberFormat} for numbers which can be used in the current locale
     */
    default NumberFormat numberFormat() {
        NumberFormat numberFormat = NumberFormat.getInstance(this.javaLocale());
        numberFormat.setMaximumFractionDigits(2);
        return numberFormat;
    }

    /**
     * @return a {@link NumberFormat} for percentages which can be used in the current locale
     */
    default NumberFormat percentFormat() {
        NumberFormat numberFormat = NumberFormat.getPercentInstance(this.javaLocale());
        numberFormat.setMaximumFractionDigits(2);
        return numberFormat;
    }

    /**
     * Formats a byte count as a human-readable number in the current locale.
     *
     * @param bytes the byte count
     * @return the formatted count
     */
    default String formatByteCount(long bytes) {
        for (long log1024 = 6L; ; log1024--) {
            long fac = 1L << (log1024 * 10L);
            if (log1024 == 0L || abs(bytes) >= fac) {
                return this.format(MODID + ".util.numberFormat.bytes." + log1024, this.numberFormat().format(bytes / (double) fac));
            }
        }
    }

    /**
     * Formats a duration as a human-readable number in the current locale.
     *
     * @param nanos the duration (in nanoseconds)
     * @return the formatted count
     */
    default String formatDuration(long nanos) {
        TimeUnit[] units = TimeUnit.values();
        for (int i = units.length - 1; ; i--) { //iterate backwards
            long fac = units[i].toNanos(1L);
            if (fac == 1L || abs(nanos) >= fac) {
                return this.format(MODID + ".util.numberFormat.time." + units[i].name().toLowerCase(Locale.ROOT), this.numberFormat().format(nanos / (double) fac));
            }
        }
    }
}
