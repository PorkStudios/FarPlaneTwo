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

package net.daporkchop.fp2.common;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.UncheckedExecutionException;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.daporkchop.lib.common.util.PorkUtil;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class GlobalProperties {
    protected static final LoadingCache<CacheKey, GlobalProperties> CACHE = CacheBuilder.newBuilder()
            .weakKeys().weakValues()
            .build(new CacheLoader<CacheKey, GlobalProperties>() {
                @Override
                public GlobalProperties load(@NonNull CacheKey key) throws IOException {
                    Properties properties = new Properties();
                    try {
                        //load file and append to list
                        try (InputStream in = key.clazz.getResourceAsStream(key.name + ".properties")) {
                            properties.load(in);
                        }
                    } catch (IOException e) {
                        throw new IOException("unable to load properties for " + key, e);
                    }

                    return new GlobalProperties(ImmutableMap.copyOf(PorkUtil.<Map<String, String>>uncheckedCast(properties)));
                }
            });

    @NonNull
    protected final Map<String, String> properties;

    protected final LoadingCache<String, Class<?>> classesCache = CacheBuilder.newBuilder()
            .weakValues()
            .build(new CacheLoader<String, Class<?>>() {
                @Override
                public Class<?> load(@NonNull String key) throws Exception {
                    return Class.forName(GlobalProperties.this.get(key), false, GlobalProperties.class.getClassLoader());
                }
            });

    protected final LoadingCache<String, Object> instancesCache = CacheBuilder.newBuilder()
            .weakValues()
            .build(CacheLoader.from(this::getInstance));

    /**
     * Gets the properties for the given class with the given name.
     *
     * @param clazz the class
     * @param name  the name of the properties file, relative to the class
     * @return the properties
     */
    public static GlobalProperties find(@NonNull Class<?> clazz, @NonNull String name) {
        return CACHE.getUnchecked(new CacheKey(clazz, name));
    }

    /**
     * Gets the property with the given key.
     *
     * @param key the property key
     * @return the property value
     * @throws NoSuchElementException if no property with the given key exists
     */
    public String get(@NonNull String key) {
        String value = this.properties.get(key);
        if (value == null) {
            throw new NoSuchElementException(key);
        }
        return value;
    }

    /**
     * Gets the class named by the given key.
     *
     * @param key the property key
     * @param <T> the class' expected supertype
     * @return the class described by the property value
     * @throws NoSuchElementException if no property with the given key exists
     * @throws ClassNotFoundException if the class with the given name could not be found
     */
    @SneakyThrows(Throwable.class)
    public <T> Class<? extends T> getClass(@NonNull String key) {
        try {
            return uncheckedCast(this.classesCache.get(key));
        } catch (ExecutionException | UncheckedExecutionException | ExecutionError e) {
            throw e.getCause();
        }
    }

    /**
     * Gets a new instance of the class named by the given key.
     * <p>
     * This method will always create a new instance of the class.
     *
     * @param key the property key
     * @param <T> the class' expected supertype
     * @return a new instance of the class described by the property value
     * @throws NoSuchElementException    if no property with the given key exists
     * @throws ClassNotFoundException    if the class with the given name could not be found
     * @throws IllegalAccessException    if the class' default constructor isn't accessible
     * @throws InstantiationException    if the class cannot be instantiated
     * @throws InvocationTargetException if an exception occurred in the target class' constructor
     * @throws NoSuchMethodException     if the class does not declare a no-args constructor
     * @see #getInstanceCached(String)
     */
    @SneakyThrows({ IllegalAccessException.class, InstantiationException.class, InvocationTargetException.class, NoSuchMethodException.class })
    public <T> T getInstance(@NonNull String key) {
        Constructor<? extends T> constructor = this.<T>getClass(key).getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    /**
     * Gets an instance of the class named by the given key.
     * <p>
     * This method will cache the instance, so subsequent invocations <strong>may</strong> return the same instance. As such, this method should only be used if you're sure that
     * the implementation will be thread-safe.
     *
     * @param key the property key
     * @param <T> the class' expected supertype
     * @return a new instance of the class described by the property value
     * @throws NoSuchElementException    if no property with the given key exists
     * @throws ClassNotFoundException    if the class with the given name could not be found
     * @throws IllegalAccessException    if the class' default constructor isn't accessible
     * @throws InstantiationException    if the class cannot be instantiated
     * @throws InvocationTargetException if an exception occurred in the target class' constructor
     * @throws NoSuchMethodException     if the class does not declare a no-args constructor
     * @see #getInstance(String)
     */
    @SneakyThrows(Throwable.class)
    public <T> T getInstanceCached(@NonNull String key) {
        try {
            return uncheckedCast(this.instancesCache.get(key));
        } catch (ExecutionException | UncheckedExecutionException | ExecutionError e) {
            throw e.getCause();
        }
    }

    /**
     * @author DaPorkchop_
     */
    @Data
    private static class CacheKey {
        private final Class clazz;
        private final String name;
    }
}
