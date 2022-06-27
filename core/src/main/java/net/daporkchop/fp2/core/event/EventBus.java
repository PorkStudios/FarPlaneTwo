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

package net.daporkchop.fp2.core.event;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
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
import net.daporkchop.lib.reflection.type.PTypes;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Standard implementation of {@link FEventBus}.
 *
 * @author DaPorkchop_
 */
//TODO: this currently does not properly wait for handlers to be removed from all executing lists when unregistering them
public class EventBus implements FEventBus {
    protected static final LoadingCache<Type, List<Type>> INHERITED_TYPES_CACHE = CacheBuilder.newBuilder()
            .weakKeys().weakValues()
            .build(new CacheLoader<Type, List<Type>>() {
                @Override
                @SuppressWarnings("UnstableApiUsage")
                public List<Type> load(Type key) throws Exception {
                    return PTypes.allRawSupertypes(PTypes.raw(key))
                            .map(rawSupertype -> PTypes.inheritedGenericSupertype(key, rawSupertype))
                            .collect(ImmutableList.toImmutableList());
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
                                    Type returningParameterType = INHERITED_TYPES_CACHE.getUnchecked(parameterType).stream()
                                            .filter(type -> type == ReturningEvent.class
                                                            || (type instanceof ParameterizedType && ((ParameterizedType) type).getRawType() == ReturningEvent.class))
                                            .findAny().get();
                                    Type t = returningParameterType instanceof ParameterizedType
                                            ? ((ParameterizedType) returningParameterType).getActualTypeArguments()[0]
                                            : Object.class;

                                    if (method.getReturnType() == void.class) { //void
                                        //no-op
                                    } else if (constrain.monitor()) { //monitors may not have a return value
                                        throw new IllegalStateException("monitor handler may not have a return value: " + methodAsString);
                                    } else if (t.equals(method.getGenericReturnType())) { //T
                                        returnHandling = ReturnHandling.T;
                                    } else if (PTypes.equals(PTypes.parameterized(Optional.class, null, t), method.getGenericReturnType())) { //Optional<T>
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

    protected static String toStringGeneric(@NonNull Method method) {
        return Stream.of(method.getGenericParameterTypes()).map(Objects::toString).collect(Collectors.joining(", ",
                method.getGenericReturnType() + " " + method.getDeclaringClass().getTypeName() + '#' + method.getName() + '(', ")"));
    }

    protected final TypeInheritanceMap<Boolean> presentEventClasses = new WeakTypeInheritanceMap<>();
    protected final TypeInheritanceMap<HandlerList> signatureToHandlers = new ConcurrentTypeInheritanceMap<>(); //needs to be concurrent in order to be read when a DynamicallyTypedEvent is fired

    protected final Map<Class<?>, HandlerList> eventClassesToHandlers = CacheBuilder.newBuilder().weakKeys().<Class<?>, HandlerList>build().asMap();

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

        this.presentEventClasses.subMap(signature)
                .forEach((eventClass, flag) -> this.eventClassesToHandlers.get(eventClass).add(handler));
    }

    @Override
    public void unregister(@NonNull Object listener) {
        this.cleanup(listener.getClass(), listener);
    }

    @Override
    public void unregisterStatic(@NonNull Class<?> listener) {
        this.cleanup(listener, listener);
    }

    protected synchronized void cleanup(@NonNull Class<?> listenerClass, @NonNull Object referenceOrListener) {
        LISTENER_METHOD_CACHE.getUnchecked(listenerClass).forEach(method -> {
            this.signatureToHandlers.computeIfPresent(method.signature, (signature, list) -> {
                list.cleanAndRemove(referenceOrListener);
                return list.isEmpty() ? null : list;
            });

            this.presentEventClasses.subMap(method.signature)
                    .forEach((eventClass, flag) -> this.eventClassesToHandlers.get(eventClass).cleanAndRemove(referenceOrListener));
        });
    }

    protected HandlerList getHandlerListForEvent(@NonNull Object event) {
        Class<?> eventClass = event.getClass();

        HandlerList list = this.eventClassesToHandlers.get(eventClass);
        return list == null
                ? this.createListForUnknownEventType(event) //create a new HandlerList for the given event type
                : list;
    }

    protected synchronized HandlerList createListForUnknownEventType(@NonNull Object event) {
        return this.eventClassesToHandlers.computeIfAbsent(event.getClass(), eventClass -> {
            HandlerList list = new HandlerList(eventClass);
            list.add(this.signatureToHandlers.superMap(eventClass).values().stream()
                    .flatMap(signatureSpecificList -> Stream.of(signatureSpecificList.handlers))
                    .distinct().toArray(Handler[]::new));

            this.presentEventClasses.put(eventClass, Boolean.TRUE);
            return list;
        });
    }

    protected HandlerList getHandlerListForEvent(@NonNull Object event, @NonNull Type forcedType) {
        if (!PTypes.raw(forcedType).isAssignableFrom(event.getClass())) {
            throw new IllegalArgumentException("raw type of " + PTypes.toString(forcedType) + " is not assignable from event " + event.getClass());
        }

        //find all the handlers whose signatures are subtypes of the event's real type, pack them into a new HandlerList and return that
        //TODO: this will make it difficult to implement waiting for handlers to be removed from all executing lists when unregistering them

        HandlerList tempList = new HandlerList(forcedType);
        tempList.add(this.signatureToHandlers.superMap(forcedType).values().stream()
                .flatMap(list -> Stream.of(list.handlers))
                .distinct().toArray(Handler[]::new));
        return tempList;
    }

    @Override
    public <T> T fire(@NonNull T event) {
        HandlerList list = this.getHandlerListForEvent(event);

        for (Handler handler : list.handlers) {
            handler.fire(event);
        }
        return event;
    }

    @Override
    public <T> T fireTyped(@NonNull T event, @NonNull Type forcedType) {
        HandlerList list = this.getHandlerListForEvent(event, forcedType);

        for (Handler handler : list.handlers) {
            handler.fire(event);
        }
        return event;
    }

    @Override
    public <R> Optional<R> fireAndGetFirst(@NonNull ReturningEvent<R> event) {
        HandlerList list = this.getHandlerListForEvent(event);

        Optional<R> out = Optional.empty();
        for (Handler handler : list.handlers) {
            if (!out.isPresent() || handler.monitor) {
                out = uncheckedCast(handler.fire(event));
            }
        }
        return out;
    }

    @Override
    public <R> Optional<R> fireTypedAndGetFirst(@NonNull ReturningEvent<R> event, @NonNull Type forcedType) {
        HandlerList list = this.getHandlerListForEvent(event, forcedType);

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
        HandlerList list = this.getHandlerListForEvent(event);

        List<R> out = new ArrayList<>();
        for (Handler handler : list.handlers) {
            PorkUtil.<Optional<R>>uncheckedCast(handler.fire(event)).ifPresent(out::add);
        }
        return out;
    }

    @Override
    public <R> List<R> fireTypedAndGetAll(@NonNull ReturningEvent<R> event, @NonNull Type forcedType) {
        HandlerList list = this.getHandlerListForEvent(event, forcedType);

        List<R> out = new ArrayList<>();
        for (Handler handler : list.handlers) {
            PorkUtil.<Optional<R>>uncheckedCast(handler.fire(event)).ifPresent(out::add);
        }
        return out;
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

    private interface TypeInheritanceMap<V> extends Map<Type, V> {
        /**
         * Gets a view of the portion of this map whose keys are a subtype of the given type.
         *
         * @param type the type
         * @return a view of the portion of this map whose keys are a subtype of the given type
         */
        default TypeInheritanceMap<V> subMap(@NonNull Type type) {
            return new DefaultTypeInheritanceChildMap<>(this, key -> PTypes.isSubtype(type, key));
        }

        /**
         * Gets a view of the portion of this map whose keys are a supertype of the given type.
         *
         * @param type the type
         * @return a view of the portion of this map whose keys are a supertype of the given type
         */
        default TypeInheritanceMap<V> superMap(@NonNull Type type) {
            return new DefaultTypeInheritanceChildMap<>(this, key -> PTypes.isSubtype(key, type));
        }
    }

    private static class DefaultTypeInheritanceMap<V> extends HashMap<Type, V> implements TypeInheritanceMap<V> {
    }

    private static class WeakTypeInheritanceMap<V> extends WeakHashMap<Type, V> implements TypeInheritanceMap<V> {
    }

    private static class ConcurrentTypeInheritanceMap<V> extends ConcurrentHashMap<Type, V> implements TypeInheritanceMap<V> {
    }

    @RequiredArgsConstructor
    private static class DefaultTypeInheritanceChildMap<V> extends AbstractMap<Type, V> implements TypeInheritanceMap<V> {
        @NonNull
        private final TypeInheritanceMap<V> delegate;
        @NonNull
        private final Predicate<Type> filter;

        @Getter(lazy = true)
        private final Set<Entry<Type, V>> entrySet = new AbstractSet<Entry<Type, V>>() {
            @Override
            public Iterator<Entry<Type, V>> iterator() {
                Predicate<Type> filter = DefaultTypeInheritanceChildMap.this.filter;

                return new BaseFilteredIterator<Entry<Type, V>>(DefaultTypeInheritanceChildMap.this.delegate.entrySet().iterator()) {
                    @Override
                    protected boolean test(@NonNull Entry<Type, V> entry) {
                        return filter.test(entry.getKey());
                    }
                };
            }

            @Override
            public Spliterator<Entry<Type, V>> spliterator() {
                Predicate<Type> filter = DefaultTypeInheritanceChildMap.this.filter;

                return new FilteredSpliterator<>(
                        DefaultTypeInheritanceChildMap.this.delegate.entrySet().spliterator(),
                        entry -> filter.test(entry.getKey()));
            }

            @Override
            public Stream<Entry<Type, V>> stream() {
                Predicate<Type> filter = DefaultTypeInheritanceChildMap.this.filter;

                return DefaultTypeInheritanceChildMap.this.delegate.entrySet().stream()
                        .filter(entry -> filter.test(entry.getKey()));
            }

            @Override
            public int size() {
                return DefaultTypeInheritanceChildMap.this.size();
            }
        };

        @Getter(lazy = true)
        private final Set<Type> keySet = new AbstractSet<Type>() {
            @Override
            public Iterator<Type> iterator() {
                Predicate<Type> filter = DefaultTypeInheritanceChildMap.this.filter;

                return new BaseFilteredIterator<Type>(DefaultTypeInheritanceChildMap.this.delegate.keySet().iterator()) {
                    @Override
                    protected boolean test(@NonNull Type key) {
                        return filter.test(key);
                    }
                };
            }

            @Override
            public Spliterator<Type> spliterator() {
                return new FilteredSpliterator<>(
                        DefaultTypeInheritanceChildMap.this.delegate.keySet().spliterator(),
                        DefaultTypeInheritanceChildMap.this.filter);
            }

            @Override
            public Stream<Type> stream() {
                return DefaultTypeInheritanceChildMap.this.delegate.keySet().stream()
                        .filter(DefaultTypeInheritanceChildMap.this.filter);
            }

            @Override
            public int size() {
                return DefaultTypeInheritanceChildMap.this.size();
            }
        };

        @Getter(lazy = true)
        private final Collection<V> values = new AbstractCollection<V>() {
            @Override
            public Iterator<V> iterator() {
                return new BaseMappingIterator<Map.Entry<Type, V>, V>(DefaultTypeInheritanceChildMap.this.entrySet().iterator()) {
                    @Override
                    protected V map(Entry<Type, V> in) {
                        return in.getValue();
                    }
                };
            }

            @Override
            public Spliterator<V> spliterator() {
                return new MappingSpliterator<Map.Entry<Type, V>, V>(DefaultTypeInheritanceChildMap.this.entrySet().spliterator(), Map.Entry::getValue);
            }

            @Override
            public Stream<V> stream() {
                //noinspection SimplifyStreamApiCallChains
                return DefaultTypeInheritanceChildMap.this.entrySet().stream().map(Map.Entry::getValue);
            }

            @Override
            public int size() {
                return DefaultTypeInheritanceChildMap.this.size();
            }
        };

        @Override
        public int size() {
            return toIntExact(this.delegate.keySet().stream().filter(this.filter).count());
        }

        @Override
        public boolean isEmpty() {
            return this.delegate.keySet().stream().noneMatch(this.filter);
        }

        @Override
        public boolean containsKey(Object key) {
            return key instanceof Type && this.filter.test((Type) key) && this.delegate.containsKey(key);
        }

        @Override
        public V get(Object key) {
            return key instanceof Type && this.filter.test((Type) key) ? this.delegate.get(key) : null;
        }

        @Override
        public V put(Type key, V value) {
            checkArg(this.filter.test(key), "key %s out of range", key);
            return this.delegate.put(key, value);
        }

        @Override
        public V remove(Object key) {
            return key instanceof Type && this.filter.test((Type) key) ? this.delegate.remove(key) : null;
        }

        @Override
        public void clear() {
            this.delegate.keySet().removeIf(this.filter);
        }

        @RequiredArgsConstructor
        private abstract static class BaseFilteredIterator<E> implements Iterator<E> {
            @NonNull
            private final Iterator<E> delegate;

            private E next;
            private boolean valid;

            protected abstract boolean test(@NonNull E value);

            @Override
            public boolean hasNext() {
                while (this.next == null && this.delegate.hasNext()) {
                    E next = this.delegate.next();
                    this.valid = false;

                    if (this.test(next)) {
                        this.next = next;
                        break;
                    }
                }
                return this.next != null;
            }

            @Override
            public E next() {
                if (!this.hasNext()) {
                    throw new NoSuchElementException();
                }

                E next = this.next;
                this.next = null;
                this.valid = true;
                return next;
            }

            @Override
            public void remove() {
                checkState(this.valid);

                this.valid = false;
                this.delegate.remove();
            }

            @Override
            public void forEachRemaining(Consumer<? super E> action) {
                this.valid = false;

                if (this.next != null) {
                    E next = this.next;
                    this.next = null;
                    action.accept(next);
                }

                this.delegate.forEachRemaining(value -> {
                    if (this.test(value)) {
                        action.accept(value);
                    }
                });
            }
        }

        @RequiredArgsConstructor
        private abstract static class BaseMappingIterator<I, O> implements Iterator<O> {
            @NonNull
            private final Iterator<I> delegate;

            protected abstract O map(I in);

            @Override
            public boolean hasNext() {
                return this.delegate.hasNext();
            }

            @Override
            public O next() {
                return this.map(this.delegate.next());
            }

            @Override
            public void remove() {
                this.delegate.remove();
            }

            @Override
            public void forEachRemaining(Consumer<? super O> action) {
                this.delegate.forEachRemaining(in -> action.accept(this.map(in)));
            }
        }

        @RequiredArgsConstructor
        private static final class FilteredSpliterator<E> implements Spliterator<E> {
            @NonNull
            private final Spliterator<E> delegate;
            @NonNull
            private final Predicate<E> filter;

            @Override
            public boolean tryAdvance(Consumer<? super E> action) {
                class State implements Consumer<E> {
                    boolean found;

                    @Override
                    public void accept(E e) {
                        if (FilteredSpliterator.this.filter.test(e)) {
                            this.found = true;
                            action.accept(e);
                        }
                    }
                }

                State state = new State();
                while (this.delegate.tryAdvance(state) && !state.found) {
                }
                return state.found;
            }

            @Override
            public Spliterator<E> trySplit() {
                Spliterator<E> split = this.delegate.trySplit();
                return split != null ? new FilteredSpliterator<>(split, this.filter) : null;
            }

            @Override
            public long estimateSize() {
                return Long.MAX_VALUE;
            }

            @Override
            public int characteristics() {
                return this.delegate.characteristics() & ~(SIZED | SUBSIZED);
            }

            @Override
            public void forEachRemaining(Consumer<? super E> action) {
                this.delegate.forEachRemaining(value -> {
                    if (this.filter.test(value)) {
                        action.accept(value);
                    }
                });
            }

            @Override
            public Comparator<? super E> getComparator() {
                return this.delegate.getComparator();
            }
        }

        @RequiredArgsConstructor
        private static final class MappingSpliterator<I, O> implements Spliterator<O> {
            @NonNull
            private final Spliterator<I> delegate;
            @NonNull
            private final Function<I, O> mappingFunction;

            @Override
            public boolean tryAdvance(Consumer<? super O> action) {
                return this.delegate.tryAdvance(in -> action.accept(this.mappingFunction.apply(in)));
            }

            @Override
            public Spliterator<O> trySplit() {
                Spliterator<I> split = this.delegate.trySplit();
                return split != null ? new MappingSpliterator<>(split, this.mappingFunction) : null;
            }

            @Override
            public long estimateSize() {
                return this.delegate.estimateSize();
            }

            @Override
            public long getExactSizeIfKnown() {
                return this.delegate.getExactSizeIfKnown();
            }

            @Override
            public int characteristics() {
                return this.delegate.characteristics() & ~(SORTED | DISTINCT | NONNULL);
            }

            @Override
            public void forEachRemaining(Consumer<? super O> action) {
                this.delegate.forEachRemaining(in -> action.accept(this.mappingFunction.apply(in)));
            }
        }
    }
}
