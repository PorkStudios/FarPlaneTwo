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
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.daporkchop.lib.common.util.PorkUtil;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Container class for global properties which are used to configure the behavior of every FP2 module.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class GlobalProperties {
    protected final Map<String, String> PROPERTIES;

    protected final LoadingCache<String, Class<?>> CLASSES_CACHE = CacheBuilder.newBuilder()
            .weakValues()
            .build(new CacheLoader<String, Class<?>>() {
                @Override
                public Class<?> load(@NonNull String key) throws Exception {
                    return Class.forName(get(key), false, GlobalProperties.class.getClassLoader());
                }
            });

    protected final LoadingCache<String, Object> INSTANCES_CACHE = CacheBuilder.newBuilder()
            .weakValues()
            .build(CacheLoader.from(GlobalProperties::getInstance));

    static {
        List<Properties> propertiesList = new ArrayList<>();

        try {
            //iterate over every fp2.properties file (from each module)
            for (Enumeration<URL> urls = GlobalProperties.class.getClassLoader().getResources("fp2.properties"); urls.hasMoreElements(); ) {
                //load file and append to list
                try (InputStream in = urls.nextElement().openStream()) {
                    Properties properties = new Properties();
                    properties.load(in);
                    propertiesList.add(properties);
                }
            }
        } catch (IOException e) {
            throw new AssertionError("unable to load global properties!", e);
        }

        //sort properties files by their priority
        propertiesList.sort(Comparator.comparingDouble(properties -> Double.parseDouble(properties.getProperty("priority", "0"))));

        //merge properties in order
        Map<String, String> merged = new HashMap<>();
        for (Properties properties : propertiesList) {
            for (Map.Entry<String, String> entry : PorkUtil.<Map<String, String>>uncheckedCast(properties).entrySet()) {
                merged.put(entry.getKey().intern(), entry.getValue().intern());
            }
        }
        merged.remove("priority");

        PROPERTIES = ImmutableMap.copyOf(merged);
    }

    /**
     * Gets the property with the given key.
     *
     * @param key the property key
     * @return the property value
     * @throws NoSuchElementException if no property with the given key exists
     */
    public String get(@NonNull String key) {
        String value = PROPERTIES.get(key);
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
            return uncheckedCast(CLASSES_CACHE.get(key));
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
        Constructor<? extends T> constructor = GlobalProperties.<T>getClass(key).getDeclaredConstructor();
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
            return uncheckedCast(INSTANCES_CACHE.get(key));
        } catch (ExecutionException | UncheckedExecutionException | ExecutionError e) {
            throw e.getCause();
        }
    }
}
