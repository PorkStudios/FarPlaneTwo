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

package net.daporkchop.fp2.core.event;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.api.event.FEventBus;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.lib.common.reference.Reference;
import net.daporkchop.lib.common.reference.ReferenceStrength;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Standard implementation of {@link FEventBus}.
 *
 * @author DaPorkchop_
 */
public class EventBus implements FEventBus {
    protected static final LoadingCache<Class<?>, List<Type>> EVENT_TYPES_CACHE = CacheBuilder.newBuilder()
            .weakKeys().weakValues()
            .build(new CacheLoader<Class<?>, List<Type>>() {
                @Override
                public List<Type> load(Class<?> key) throws Exception {
                    ImmutableSet.Builder<Type> builder = ImmutableSet.builder();
                    this.findTypesRecursive(builder, key);
                    return ImmutableList.copyOf(builder.build());
                }

                protected <T> void findTypesRecursive(ImmutableSet.Builder<Type> builder, Type type) {
                    builder.add(type);

                    Class<T> rawType;
                    Type[] typeArguments;
                    if (type == Object.class) {
                        return;
                    } else if (type instanceof Class) {
                        rawType = uncheckedCast(type);
                        typeArguments = new Type[0];
                    } else if (type instanceof ParameterizedType) {
                        ParameterizedType parameterizedType = (ParameterizedType) type;
                        rawType = uncheckedCast(parameterizedType.getRawType());
                        typeArguments = parameterizedType.getActualTypeArguments();
                    } else {
                        throw new IllegalArgumentException("don't know how to handle type: " + type);
                    }

                    TypeVariable<Class<T>>[] typeVariables = rawType.getTypeParameters();
                    checkArg(typeVariables.length == typeArguments.length, "cannot resolve type parameters for %s (length mismatch)", type);

                    Map<String, Type> parameters = IntStream.range(0, typeVariables.length).boxed().collect(Collectors.toMap(
                            i -> typeVariables[i].getName(),
                            i -> typeArguments[i]));

                    if (!rawType.isInterface()) {
                        this.findTypesRecursive(builder, resolveType(rawType.getGenericSuperclass(), parameters));
                    }
                    for (Type interfaz : rawType.getGenericInterfaces()) {
                        this.findTypesRecursive(builder, resolveType(interfaz, parameters));
                    }
                }
            });

    protected static final LoadingCache<Class<?>, List<HandlerMethod>> LISTENER_METHOD_CACHE = CacheBuilder.newBuilder()
            .weakKeys().weakValues()
            .build(CacheLoader.from(clazz -> {
                ImmutableList.Builder<HandlerMethod> builder = ImmutableList.builder();
                for (Method method : clazz.getMethods()) {
                    if ((method.getModifiers() & Modifier.STATIC) == 0 //method isn't static
                        && method.isAnnotationPresent(FEventHandler.class)) { //method is annotated as @FEventHandler
                        checkArg(method.getReturnType() == void.class, "method annotated as %s must return void: %s", FEventHandler.class.getTypeName(), method);
                        checkArg(method.getParameterCount() == 1, "method annotated as %s must have exactly one parameter: %s", FEventHandler.class.getTypeName(), method);

                        try {
                            method.setAccessible(true);
                            builder.add(new HandlerMethod(method.getGenericParameterTypes()[0], MethodHandles.publicLookup().unreflect(method)));
                        } catch (IllegalAccessException | IllegalArgumentException e) {
                            PUnsafe.throwException(e);
                        }
                    }
                }
                return builder.build();
            }));

    protected static Type resolveType(Type type, Map<String, Type> parameters) {
        if (type == null || type instanceof Class) {
            return type;
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type[] actualTypeArguments = Stream.of(parameterizedType.getActualTypeArguments()).map(argument -> resolveType(argument, parameters)).toArray(Type[]::new);
            Type rawType = resolveType(parameterizedType.getRawType(), parameters);
            Type ownerType = resolveType(parameterizedType.getOwnerType(), parameters);

            return new ParameterizedType() {
                @Override
                public Type[] getActualTypeArguments() {
                    return actualTypeArguments;
                }

                @Override
                public Type getRawType() {
                    return rawType;
                }

                @Override
                public Type getOwnerType() {
                    return ownerType;
                }

                @Override
                public int hashCode() {
                    return Arrays.hashCode(actualTypeArguments)
                           ^ Objects.hashCode(ownerType)
                           ^ Objects.hashCode(rawType);
                }

                @Override
                public boolean equals(Object obj) {
                    if (obj == this) {
                        return true;
                    } else if (obj instanceof ParameterizedType) {
                        ParameterizedType other = (ParameterizedType) obj;
                        return Objects.equals(ownerType, other.getOwnerType())
                               && Objects.equals(rawType, other.getRawType())
                               && Arrays.equals(actualTypeArguments, other.getActualTypeArguments());
                    } else {
                        return false;
                    }
                }
            };
        } else if (type instanceof TypeVariable) {
            TypeVariable typeVariable = (TypeVariable) type;
            Type value = parameters.get(typeVariable.getName());
            checkArg(value != null, "unknown type variable: %s (parameters: %s)", typeVariable, parameters);
            return value;
        } else if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            Type[] upperBounds = Stream.of(wildcardType.getUpperBounds()).map(argument -> resolveType(argument, parameters)).toArray(Type[]::new);
            Type[] lowerBounds = Stream.of(wildcardType.getLowerBounds()).map(argument -> resolveType(argument, parameters)).toArray(Type[]::new);

            return new WildcardType() {
                @Override
                public Type[] getUpperBounds() {
                    return upperBounds;
                }

                @Override
                public Type[] getLowerBounds() {
                    return lowerBounds;
                }

                @Override
                public int hashCode() {
                    return Arrays.hashCode(upperBounds) ^ Arrays.hashCode(lowerBounds);
                }

                @Override
                public boolean equals(Object obj) {
                    if (obj == this) {
                        return true;
                    } else if (obj instanceof WildcardType) {
                        WildcardType other = (WildcardType) obj;
                        return Arrays.equals(upperBounds, other.getUpperBounds()) && Arrays.equals(lowerBounds, other.getLowerBounds());
                    } else {
                        return false;
                    }
                }
            };
        } else {
            throw new IllegalArgumentException("don't know how to handle type: " + type);
        }
    }

    protected final Map<Type, Set<Class<?>>> signatureToEventClasses = new WeakHashMap<>();
    protected final Map<Type, HandlerList> signatureToHandlers = new WeakHashMap<>();

    protected final Map<Class<?>, HandlerList> eventClassesToHandlers = new WeakHashMap<>();

    @Override
    public void register(@NonNull Object listener) {
        this.registerReference(listener, ReferenceStrength.STRONG);
    }

    @Override
    public void registerWeak(@NonNull Object listener) {
        this.registerReference(listener, ReferenceStrength.WEAK);
    }

    protected synchronized void registerReference(@NonNull Object listener, @NonNull ReferenceStrength strength) {
        Class<?> listenerClass = listener.getClass();
        Reference<Object> reference = strength.createReference(listener, ref -> this.cleanup(listenerClass, ref));

        LISTENER_METHOD_CACHE.getUnchecked(listener.getClass()).forEach(method -> {
            Handler handler = method.handler(reference);

            this.signatureToHandlers.computeIfAbsent(method.signature, _unused -> new HandlerList()).add(handler);
            this.signatureToEventClasses.getOrDefault(method.signature, Collections.emptySet()).forEach(eventClass -> {
                this.eventClassesToHandlers.get(eventClass).add(handler);
            });
        });
    }

    protected synchronized void cleanup(@NonNull Class<?> listenerClass, @NonNull Object referenceOrListener) {
        LISTENER_METHOD_CACHE.getUnchecked(listenerClass).forEach(method -> {
            this.signatureToHandlers.computeIfPresent(method.signature, (signature, list) -> {
                list.cleanAndRemove(referenceOrListener);
                return list.isEmpty() ? null : list;
            });

            this.signatureToEventClasses.getOrDefault(method.signature, Collections.emptySet()).forEach(eventClass -> {
                this.eventClassesToHandlers.get(eventClass).cleanAndRemove(referenceOrListener);
            });
        });
    }

    @Override
    public void unregister(@NonNull Object listener) {
        this.cleanup(listener.getClass(), listener);
    }

    @Override
    public <T> T fire(@NonNull T event) {
        HandlerList handlers = this.eventClassesToHandlers.get(event.getClass());
        if (handlers == null) {
            handlers = this.createListForUnknownEventType(event);
        }

        handlers.fire(event);
        return event;
    }

    protected synchronized HandlerList createListForUnknownEventType(@NonNull Object event) {
        return this.eventClassesToHandlers.computeIfAbsent(event.getClass(), eventClass -> {
            HandlerList list = new HandlerList();
            EVENT_TYPES_CACHE.getUnchecked(eventClass).forEach(type -> {
                list.addAll(this.signatureToHandlers.computeIfAbsent(type, _unused -> new HandlerList()));

                this.signatureToEventClasses.computeIfAbsent(type, _unused -> Collections.newSetFromMap(new WeakHashMap<>())).add(eventClass);
            });
            return list;
        });
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    protected static class HandlerMethod {
        @NonNull
        protected final Type signature;
        @NonNull
        protected final MethodHandle handle;

        public Handler handler(@NonNull Reference<Object> reference) {
            return new Handler(reference, this.handle);
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected static class HandlerList extends CopyOnWriteArrayList<Handler> {
        public void fire(@NonNull Object event) {
            this.forEach(handler -> {
                Object instance = handler.reference.get();
                if (instance != null) {
                    try {
                        handler.handle.invoke(instance, event);
                    } catch (Throwable t) {
                        PUnsafe.throwException(t);
                    }
                }
            });
        }

        public void cleanAndRemove(@NonNull Object referenceOrListener) {
            this.removeIf(handler -> {
                if (handler.reference == referenceOrListener) { //referenceOrListener is a reference, and is the same
                    return true;
                } else {
                    Object instance = handler.reference.get();
                    return instance == null || instance == referenceOrListener;
                }
            });
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    protected static class Handler {
        @NonNull
        protected final Reference<Object> reference;
        @NonNull
        protected final MethodHandle handle;
    }
}
