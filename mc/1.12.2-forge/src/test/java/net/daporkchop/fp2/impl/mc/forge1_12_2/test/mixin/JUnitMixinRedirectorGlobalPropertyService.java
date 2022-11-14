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

package net.daporkchop.fp2.impl.mc.forge1_12_2.test.mixin;

import lombok.Data;
import lombok.NonNull;
import net.daporkchop.lib.common.util.PorkUtil;
import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
public final class JUnitMixinRedirectorGlobalPropertyService implements IGlobalPropertyService {
    private static final Map<Object, Object> MAP = new ConcurrentHashMap<>();

    @Override
    public IPropertyKey resolveKey(String name) {
        return new Key(name.intern());
    }

    @Override
    public <T> T getProperty(IPropertyKey key) {
        return uncheckedCast(MAP.get(key));
    }

    @Override
    public void setProperty(IPropertyKey key, Object value) {
        MAP.put(key, value);
    }

    @Override
    public <T> T getProperty(IPropertyKey key, T defaultValue) {
        return uncheckedCast(MAP.getOrDefault(key, defaultValue));
    }

    @Override
    public String getPropertyString(IPropertyKey key, String defaultValue) {
        return PorkUtil.fallbackIfNull(MAP.get(key), defaultValue).toString();
    }

    /**
     * @author DaPorkchop_
     */
    @Data
    private static final class Key implements IPropertyKey {
        @NonNull
        private final String key;
    }
}
