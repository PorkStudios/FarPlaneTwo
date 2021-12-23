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
import lombok.SneakyThrows;
import net.daporkchop.fp2.api.event.Constrain;
import net.daporkchop.fp2.api.event.FEventBus;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.api.event.ReturningEvent;
import net.daporkchop.lib.common.function.throwing.EFunction;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.reference.Reference;
import net.daporkchop.lib.common.reference.ReferenceStrength;
import net.daporkchop.lib.common.util.PorkUtil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
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
            .build(new CacheLoader<Class<?>, List<HandlerMethod>>() {
                @Override
                public List<HandlerMethod> load(Class<?> clazz) throws Exception {
                    return this.getAllSuperclasses(clazz)
                            .map(Class::getDeclaredMethods)
                            .flatMap(Stream::of)
                            .filter(method -> method.isAnnotationPresent(FEventHandler.class))
                            .map((EFunction<Method, HandlerMethod>) method -> {
                                String methodAsString = toStringGeneric(method);

                                method.setAccessible(true);
                                FEventHandler handler = method.getAnnotation(FEventHandler.class);
                                checkArg(method.getParameterCount() == 1, "event handler must have exactly one parameter: %s", methodAsString);

                                String name = handler.name().isEmpty() ? org.objectweb.asm.Type.getMethodDescriptor(method).intern() : handler.name();
                                Constrain constrain = handler.constrain();
                                Type parameterType = method.getGenericParameterTypes()[0];
                                MethodHandle handle = MethodHandles.publicLookup().unreflect(method);
                                ReturnHandling returnHandling = ReturnHandling.VOID;
                                boolean instance = (method.getModifiers() & Modifier.STATIC) == 0;

                                boolean returning = ReturningEvent.class.isAssignableFrom(method.getParameterTypes()[0]);
                                if (returning) {
                                    Type t = parameterType instanceof ParameterizedType
                                            ? ((ParameterizedType) parameterType).getActualTypeArguments()[0]
                                            : Object.class;

                                    if (method.getReturnType() == void.class) { //void
                                        //no-op
                                    } else if (constrain.monitor()) { //monitors may not have a return value
                                        throw new IllegalStateException("monitor handler may not have a return value: " + methodAsString);
                                    } else if (t.equals(method.getGenericReturnType())) { //T
                                        returnHandling = ReturnHandling.T;
                                    } else if (new ParameterizedTypeImpl(new Type[]{ t }, Optional.class, null).equals(method.getGenericReturnType())) { //Optional<T>
                                        returnHandling = ReturnHandling.OPTIONAL_T;
                                    } else {
                                        throw new IllegalArgumentException(PStrings.fastFormat("event handler must return void, %2$s or Optional<%2$s>: %1$s", methodAsString, parameterType));
                                    }
                                } else {
                                    checkArg(method.getReturnType() == void.class, "event handler must return void: %s", methodAsString);
                                }

                                return new HandlerMethod(name, constrain, parameterType, handle, returnHandling, instance);
                            })
                            .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
                }

                protected Stream<Class<?>> getAllSuperclasses(@NonNull Class<?> clazz) {
                    return Stream.concat(
                            Stream.of(clazz),
                            Stream.concat(Stream.of(clazz.getSuperclass()), Stream.of(clazz.getInterfaces()))
                                    .filter(Objects::nonNull)
                                    .flatMap(this::getAllSuperclasses))
                            .distinct();
                }
            });

    protected static Type resolveType(Type type, Map<String, Type> parameters) {
        if (type == null || type instanceof Class) {
            return type;
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type[] actualTypeArguments = Stream.of(parameterizedType.getActualTypeArguments()).map(argument -> resolveType(argument, parameters)).toArray(Type[]::new);
            Type rawType = resolveType(parameterizedType.getRawType(), parameters);
            Type ownerType = resolveType(parameterizedType.getOwnerType(), parameters);

            return new ParameterizedTypeImpl(actualTypeArguments, rawType, ownerType);
        } else if (type instanceof TypeVariable) {
            TypeVariable typeVariable = (TypeVariable) type;
            Type value = parameters.get(typeVariable.getName());
            checkArg(value != null, "unknown type variable: %s (parameters: %s)", typeVariable, parameters);
            return value;
        } else if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            Type[] upperBounds = Stream.of(wildcardType.getUpperBounds()).map(argument -> resolveType(argument, parameters)).toArray(Type[]::new);
            Type[] lowerBounds = Stream.of(wildcardType.getLowerBounds()).map(argument -> resolveType(argument, parameters)).toArray(Type[]::new);

            return new WildcardTypeImpl(upperBounds, lowerBounds);
        } else {
            throw new IllegalArgumentException("don't know how to handle type: " + type);
        }
    }

    protected static String toStringGeneric(@NonNull Method method) {
        return Stream.of(method.getGenericParameterTypes()).map(Objects::toString).collect(Collectors.joining(", ",
                method.getGenericReturnType() + " " + method.getDeclaringClass().getTypeName() + '#' + method.getName() + '(', ")"));
    }

    protected final Map<Type, Set<Class<?>>> signatureToEventClasses = new HashMap<>();
    protected final Map<Type, HandlerList> signatureToHandlers = new HashMap<>();

    protected final Map<Class<?>, HandlerList> eventClassesToHandlers = new WeakHashMap<>();

    @Override
    public void register(@NonNull Object listener) {
        this.registerMember(listener, ReferenceStrength.STRONG);
    }

    @Override
    public void registerWeak(@NonNull Object listener) {
        this.registerMember(listener, ReferenceStrength.WEAK);
    }

    protected synchronized void registerMember(@NonNull Object listener, @NonNull ReferenceStrength strength) {
        Class<?> listenerClass = listener.getClass();
        Reference<Object> reference = strength.createReference(listener, ref -> this.cleanup(listenerClass, ref));

        LISTENER_METHOD_CACHE.getUnchecked(listener.getClass()).forEach(method -> {
            method.handlerMember(reference).ifPresent(handler -> this.addHandler(handler, method.signature));
        });
    }

    @Override
    public synchronized void registerStatic(@NonNull Class<?> listener) {
        LISTENER_METHOD_CACHE.getUnchecked(listener).forEach(method -> {
            method.handlerStatic(listener).ifPresent(handler -> this.addHandler(handler, method.signature));
        });
    }

    protected void addHandler(@NonNull Handler handler, @NonNull Type signature) {
        this.signatureToHandlers.computeIfAbsent(signature, HandlerList::new).add(handler);
        this.signatureToEventClasses.getOrDefault(signature, Collections.emptySet()).forEach(eventClass -> {
            this.eventClassesToHandlers.get(eventClass).add(handler);
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
    public void unregisterStatic(@NonNull Class<?> listener) {
        this.cleanup(listener, listener);
    }

    @Override
    public <T> T fire(@NonNull T event) {
        HandlerList list = this.eventClassesToHandlers.get(event.getClass());
        if (list == null) {
            list = this.createListForUnknownEventType(event);
        }

        for (Handler handler : list.handlers) {
            handler.fire(event);
        }
        return event;
    }

    @Override
    public <R> Optional<R> fireAndGetFirst(@NonNull ReturningEvent<R> event) {
        HandlerList list = this.eventClassesToHandlers.get(event.getClass());
        if (list == null) {
            list = this.createListForUnknownEventType(event);
        }

        Optional<R> out = Optional.empty();
        for (Handler handler : list.handlers) {
            if (!out.isPresent() || handler.monitor) {
                out = uncheckedCast(handler.fire(event));
            }
        }
        return out;
    }

    @Override
    public <R> List<R> fireAndGetAll(@NonNull ReturningEvent<R> event) {
        HandlerList list = this.eventClassesToHandlers.get(event.getClass());
        if (list == null) {
            list = this.createListForUnknownEventType(event);
        }

        List<R> out = new ArrayList<>();
        for (Handler handler : list.handlers) {
            PorkUtil.<Optional<R>>uncheckedCast(handler.fire(event)).ifPresent(out::add);
        }
        return out;
    }

    protected synchronized HandlerList createListForUnknownEventType(@NonNull Object event) {
        return this.eventClassesToHandlers.computeIfAbsent(event.getClass(), eventClass -> {
            HandlerList list = new HandlerList(eventClass);
            EVENT_TYPES_CACHE.getUnchecked(eventClass).forEach(type -> {
                list.add(this.signatureToHandlers.computeIfAbsent(type, HandlerList::new).handlers);

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
        protected final String name;
        @NonNull
        protected final Constrain constrain;

        @NonNull
        protected final Type signature;
        @NonNull
        protected final MethodHandle handle;
        @NonNull
        protected final ReturnHandling returnHandling;
        protected final boolean member;

        public Optional<Handler> handlerMember(@NonNull Reference<Object> reference) {
            return this.member
                    ? Optional.of(new Handler.Member(this, reference))
                    : Optional.empty();
        }

        public Optional<Handler> handlerStatic(@NonNull Class<?> clazz) {
            return this.member
                    ? Optional.empty()
                    : Optional.of(new Handler.Static(this, clazz));
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    protected static class HandlerList {
        @NonNull
        protected final Type eventType;

        protected Handler[] handlers = new Handler[0];

        public void fire(@NonNull Object event) {
            for (Handler handler : this.handlers) {
                handler.fire(event);
            }
        }

        public boolean isEmpty() {
            return this.handlers.length == 0;
        }

        public synchronized void add(@NonNull Handler... handlers) {
            Handler[] nextHandlers = Arrays.copyOf(this.handlers, this.handlers.length + handlers.length);
            System.arraycopy(handlers, 0, nextHandlers, this.handlers.length, handlers.length);
            this.setHandlers(nextHandlers);
        }

        public synchronized void cleanAndRemove(@NonNull Object referenceOrListener) {
            Handler[] nextHandlers = new Handler[this.handlers.length];
            int i = 0;
            for (Handler handler : this.handlers) {
                if (!handler.shouldRemove(referenceOrListener)) {
                    nextHandlers[i++] = handler;
                }
            }
            this.setHandlers(Arrays.copyOf(nextHandlers, i));
        }

        private void setHandlers(@NonNull Handler[] handlers) {
            if (handlers.length <= 1) { //no sorting is needed
                this.handlers = handlers;
                return;
            }

            //index handlers by their name, for improved lookup times
            Map<String, List<Handler>> handlersByName = Stream.of(handlers).collect(Collectors.groupingBy(handler -> handler.method.name));

            //prepare dependencies graph
            Map<Handler, Set<Handler>> dependenciesByHandler = new IdentityHashMap<>();
            for (Handler handler : handlers) {
                dependenciesByHandler.put(handler, Collections.newSetFromMap(new IdentityHashMap<>()));
            }

            //build dependencies graph
            List<Handler> notMonitors = Stream.of(handlers).filter(handler -> !handler.method.constrain.monitor()).collect(Collectors.toList());
            for (Handler handler : handlers) {
                Constrain constrain = handler.method.constrain;
                Set<Handler> dependencies = dependenciesByHandler.get(handler);

                if (constrain.monitor()) { //handler is a monitor, make it depend on everything which isn't
                    dependencies.addAll(notMonitors);
                }
                for (String before : constrain.before()) {
                    handlersByName.getOrDefault(before, Collections.emptyList()).forEach(other -> dependenciesByHandler.get(other).add(handler));
                }
                for (String after : constrain.after()) {
                    dependencies.addAll(handlersByName.getOrDefault(after, Collections.emptyList()));
                }
            }

            //sort the handlers using a topological sort
            List<Handler> sorted = new ArrayList<>(handlers.length);
            Set<Handler> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            Set<Handler> cycleGuard = new LinkedHashSet<>();
            for (Handler handler : handlers) {
                this.toposortHandlers(handler, sorted, dependenciesByHandler, visited, cycleGuard);
            }
            checkState(sorted.size() == handlers.length, "sorted handler list only contains %d/%d handlers!", sorted.size(), handlers.length);
            this.handlers = sorted.toArray(new Handler[0]);
        }

        private void toposortHandlers(Handler handler, List<Handler> sorted, Map<Handler, Set<Handler>> dependenciesByHandler, Set<Handler> visited, Set<Handler> cycleGuard) {
            if (!cycleGuard.add(handler)) {
                throw this.cycleDetected(handler, cycleGuard);
            }

            if (visited.add(handler)) { //this is the first time we've visited this handler
                dependenciesByHandler.get(handler).forEach(dependency -> this.toposortHandlers(dependency, sorted, dependenciesByHandler, visited, cycleGuard));
                sorted.add(handler);
            }

            cycleGuard.remove(handler);
        }

        private IllegalStateException cycleDetected(Handler handler, Set<Handler> cycleGuard) {
            StringBuilder builder = new StringBuilder();
            builder.append("cycle detected when attempting to sort handlers for ").append(this.eventType.getTypeName()).append(": ");
            for (Iterator<Handler> itr = cycleGuard.iterator(); itr.hasNext(); ) {
                if (itr.next() == handler) {
                    builder.append(handler);
                    while (itr.hasNext()) {
                        builder.append(" -> ").append(itr.next());
                    }
                    builder.append(" -> ").append(handler);
                }
            }
            return new IllegalStateException(builder.toString());
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected static abstract class Handler {
        protected final HandlerMethod method;
        protected final MethodHandle handle;
        protected final ReturnHandling returnHandling;
        protected final boolean monitor;

        public Handler(@NonNull HandlerMethod method) {
            this.method = method;
            this.handle = method.handle;
            this.returnHandling = method.returnHandling;
            this.monitor = method.constrain.monitor();
        }

        public abstract Optional<?> fire(@NonNull Object event);

        public abstract boolean shouldRemove(@NonNull Object referenceOrListener);

        @Override
        public String toString() {
            return this.method.name;
        }

        /**
         * @author DaPorkchop_
         */
        protected static class Member extends Handler {
            protected final Reference<Object> reference;

            protected Member(@NonNull HandlerMethod method, @NonNull Reference<Object> reference) {
                super(method);
                this.reference = reference;
            }

            @Override
            @SneakyThrows
            public Optional<?> fire(@NonNull Object event) {
                Object instance = this.reference.get();
                return instance != null
                        ? this.returnHandling.wrapReturnValue(this.handle.invoke(instance, event))
                        : Optional.empty();
            }

            @Override
            public boolean shouldRemove(@NonNull Object referenceOrListener) {
                if (this.reference == referenceOrListener) { //referenceOrListener is a reference, and is the same
                    return true;
                } else {
                    Object instance = this.reference.get();
                    return instance == null || instance == referenceOrListener;
                }
            }
        }

        /**
         * @author DaPorkchop_
         */
        protected static class Static extends Handler {
            protected final Class<?> clazz;

            protected Static(@NonNull HandlerMethod method, @NonNull Class<?> clazz) {
                super(method);
                this.clazz = clazz;
            }

            @Override
            @SneakyThrows
            public Optional<?> fire(@NonNull Object event) {
                return this.returnHandling.wrapReturnValue(this.handle.invoke(event));
            }

            @Override
            public boolean shouldRemove(@NonNull Object referenceOrListener) {
                return this.clazz == referenceOrListener;
            }
        }
    }

    /**
     * @author DaPorkchop_
     */
    private enum ReturnHandling {
        VOID {
            @Override
            public Optional<?> wrapReturnValue(Object returnValue) {
                return Optional.empty();
            }
        },
        T {
            @Override
            public Optional<?> wrapReturnValue(Object returnValue) {
                return Optional.of(returnValue);
            }
        },
        OPTIONAL_T {
            @Override
            public Optional<?> wrapReturnValue(Object returnValue) {
                return uncheckedCast(returnValue);
            }
        };

        public abstract Optional<?> wrapReturnValue(Object returnValue);
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    private static class ParameterizedTypeImpl implements ParameterizedType {
        @NonNull
        private final Type[] actualTypeArguments;
        @NonNull
        private final Type rawType;
        private final Type ownerType;

        @Override
        public Type[] getActualTypeArguments() {
            return this.actualTypeArguments;
        }

        @Override
        public Type getRawType() {
            return this.rawType;
        }

        @Override
        public Type getOwnerType() {
            return this.ownerType;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(this.actualTypeArguments)
                   ^ Objects.hashCode(this.ownerType)
                   ^ Objects.hashCode(this.rawType);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof ParameterizedType) {
                ParameterizedType other = (ParameterizedType) obj;
                return Objects.equals(this.ownerType, other.getOwnerType())
                       && Objects.equals(this.rawType, other.getRawType())
                       && Arrays.equals(this.actualTypeArguments, other.getActualTypeArguments());
            } else {
                return false;
            }
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    private static class WildcardTypeImpl implements WildcardType {
        @NonNull
        private final Type[] upperBounds;
        @NonNull
        private final Type[] lowerBounds;

        @Override
        public Type[] getUpperBounds() {
            return this.upperBounds;
        }

        @Override
        public Type[] getLowerBounds() {
            return this.lowerBounds;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(this.upperBounds) ^ Arrays.hashCode(this.lowerBounds);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof WildcardType) {
                WildcardType other = (WildcardType) obj;
                return Arrays.equals(this.upperBounds, other.getUpperBounds()) && Arrays.equals(this.lowerBounds, other.getLowerBounds());
            } else {
                return false;
            }
        }
    }
}
