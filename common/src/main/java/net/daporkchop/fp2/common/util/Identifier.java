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

package net.daporkchop.fp2.common.util;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.lib.common.reference.HandleableReference;
import net.daporkchop.lib.common.reference.PReferenceHandler;
import net.daporkchop.lib.common.reference.WeakReference;
import net.daporkchop.lib.common.reference.cache.Cached;
import net.daporkchop.lib.common.util.PorkUtil;

import java.lang.ref.ReferenceQueue;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.daporkchop.lib.common.math.PMath.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * A namespaced identifier used by Minecraft to identify basically everything.
 * <p>
 * Identifiers take the form of {@code [namespace:]<path>}, where {@code namespace} is optional and defaults to {@code minecraft}.
 *
 * @author DaPorkchop_
 */
@Getter
public final class Identifier implements Comparable<Identifier> {
    public static final String DEFAULT_NAMESPACE = "minecraft";

    private static final Cached<Matcher> STRICT_MATCHER = Cached.regex(Pattern.compile("^(?>" + Pattern.quote(DEFAULT_NAMESPACE) + ":|([a-zA-Z0-9_.-]*):)?([a-zA-Z0-9/_.-]*)$"));
    private static final Cached<Matcher> LENIENT_MATCHER = Cached.regex(Pattern.compile("^(?>" + Pattern.quote(DEFAULT_NAMESPACE) + ":|([^:]*):)?([^:]*)$"));

    private static final Map<String, Reference> VALUES = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER); //TODO: do i really want this to be case-insensitive?

    /**
     * Parses an {@link Identifier} from its text representation.
     *
     * @param namespace the identifier's namespace
     * @param path      the identifier's path
     * @return the {@link Identifier}
     * @throws IllegalArgumentException if the given string is not a valid identifier
     */
    public static Identifier from(@NonNull String namespace, @NonNull String path) {
        return from(namespace + ':' + path);
    }

    /**
     * Parses an {@link Identifier} from its text representation.
     *
     * @param identifier the identifier to parse
     * @return the {@link Identifier}
     * @throws IllegalArgumentException if the given string is not a valid identifier
     */
    public static Identifier from(@NonNull String identifier) {
        return fromString0(identifier, STRICT_MATCHER);
    }

    /**
     * Parses an {@link Identifier} from its text representation.
     * <p>
     * This method only applies extremely lenient validation to the identifier. Usage of this method is strongly discouraged.
     *
     * @param namespace the identifier's namespace
     * @param path      the identifier's path
     * @return the {@link Identifier}
     * @throws IllegalArgumentException if the given string is not a valid identifier
     */
    public static Identifier fromLenient(@NonNull String namespace, @NonNull String path) {
        return fromLenient(namespace + ':' + path);
    }

    /**
     * Parses an {@link Identifier} from its text representation.
     * <p>
     * This method only applies extremely lenient validation to the identifier. Usage of this method is strongly discouraged.
     *
     * @param identifier the identifier to parse
     * @return the {@link Identifier}
     * @throws IllegalArgumentException if the given string is not a valid identifier
     */
    public static Identifier fromLenient(@NonNull String identifier) {
        return fromString0(identifier, LENIENT_MATCHER);
    }

    private static Identifier fromString0(@NonNull String identifier, @NonNull Cached<Matcher> matcherRef) {
        Matcher matcher = matcherRef.get().reset(identifier);
        checkArg(matcher.find(), "Invalid identifier: \"%s\"", identifier);

        String namespace = PorkUtil.fallbackIfNull(matcher.group(1), DEFAULT_NAMESPACE);
        String path = matcher.group(2);

        return lookupOrGet(namespace, path);
    }

    private static Identifier lookupOrGet(@NonNull String namespace, @NonNull String path) {
        String fullName = namespace + ':' + path;

        { //try to return existing identifier from cache
            Reference ref = VALUES.get(fullName);
            Identifier id;
            if (ref != null && (id = ref.get()) != null) { //already cached, return it!
                return id;
            }
        }

        Identifier id = new Identifier(namespace, path, fullName);
        fullName = id.fullName; //this has been interned

        Reference ref = PReferenceHandler.<Identifier, Reference>createReference(id, Reference::new);
        Reference existingReference;

        while ((existingReference = VALUES.putIfAbsent(fullName, ref)) != null) {
            Identifier existingId = existingReference.get();
            if (existingId != null) { //we lost the race
                return existingId;
            } else if (VALUES.replace(fullName, existingReference, ref)) { //we won the race
                return id;
            }
        }

        return id;
    }

    private final String namespace;
    private final String path;
    @Getter(AccessLevel.NONE)
    private final String fullName;
    private final transient int hashCode;

    private Identifier(String namespace, String path, String fullName) {
        this.namespace = namespace.intern();
        this.path = path.intern();
        this.fullName = fullName.intern();

        //dig my epic hash distribution
        this.hashCode = mix32(fullName.chars().asLongStream().reduce((a, b) -> mix64(a + b)).getAsLong());
    }

    @Override
    public String toString() {
        return this.fullName;
    }

    @Override
    public int compareTo(Identifier o) {
        return this == o ? 0 : this.fullName.compareTo(o.fullName);
    }

    /**
     * @author DaPorkchop_
     */
    private static class Reference extends WeakReference<Identifier> implements HandleableReference {
        protected final String fullName;

        public Reference(@NonNull Identifier id, @NonNull ReferenceQueue<? super Identifier> queue) {
            super(id, queue);

            this.fullName = id.fullName;
        }

        @Override
        public void handle() {
            VALUES.remove(this.fullName, this);
        }
    }
}
