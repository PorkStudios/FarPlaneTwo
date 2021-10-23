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

package net.daporkchop.fp2.client.gl.shader;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.common.util.PorkUtil;
import net.daporkchop.lib.primitive.map.open.ObjObjOpenHashMap;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Custom preprocessor implementation for GLSL source files.
 *
 * @author DaPorkchop_
 */
public class Preprocessor {
    public static final char DIRECTIVE_PREFIX = '#';

    public static final String QUOTED_DIRECTIVE_PREFIX = Pattern.quote(String.valueOf(DIRECTIVE_PREFIX));

    //protected static final String EOL_PATTERN = "\\s*(?://.*)?$";
    protected static final String EOL_PATTERN = "\\s*$";

    protected static final String MACRO_PATTERN = "[A-Za-z][A-Za-z0-9_]+";
    protected static final Pattern MACRO_PATTERN_COMPILED = Pattern.compile(MACRO_PATTERN);

    protected static final String MACRO_WORD_PATTERN = "\\b" + MACRO_PATTERN + "\\b";
    protected static final Pattern MACRO_WORD_PATTERN_COMPILED = Pattern.compile(MACRO_WORD_PATTERN);

    protected static final String DIRECTIVE_NAME_PATTERN = "(?<=^" + QUOTED_DIRECTIVE_PREFIX + ")[a-z]+(?=\\s|$)";
    protected static final Pattern DIRECTIVE_NAME_PATTERN_COMPILED = Pattern.compile(DIRECTIVE_NAME_PATTERN);

    protected static final String DEFINE_PATTERN = '^' + QUOTED_DIRECTIVE_PREFIX + "define\\s+(" + MACRO_PATTERN + ")(?:\\s+(.+))?";
    protected static final Pattern DEFINE_PATTERN_COMPILED = Pattern.compile(DEFINE_PATTERN);

    protected static final String UNDEF_PATTERN = '^' + QUOTED_DIRECTIVE_PREFIX + "undef\\s+(" + MACRO_PATTERN + ')' + EOL_PATTERN;
    protected static final Pattern UNDEF_PATTERN_COMPILED = Pattern.compile(UNDEF_PATTERN);

    protected static final String INCLUDE_PATTERN = '^' + QUOTED_DIRECTIVE_PREFIX + "include\\s+(?:<\"(.*?)\">|\"(.*?)\")" + EOL_PATTERN;
    protected static final Pattern INCLUDE_PATTERN_COMPILED = Pattern.compile(INCLUDE_PATTERN);
    protected static final int INCLUDE_PATTERN_GROUP_ABSOLUTE = 1;
    protected static final int INCLUDE_PATTERN_GROUP_RELATIVE = 2;

    protected static final String ERROR_PATTERN = '^' + QUOTED_DIRECTIVE_PREFIX + "error(\\s+.+)?$";
    protected static final Pattern ERROR_PATTERN_COMPILED = Pattern.compile(ERROR_PATTERN);

    protected static final String ANY_BRANCH_PATTERN = '^' + QUOTED_DIRECTIVE_PREFIX + "(?:((el)?if(?:(def)|(ndef))?\\s)|(else)|(endif))";
    protected static final Pattern ANY_CONDITION_PATTERN_COMPILED = Pattern.compile(ANY_BRANCH_PATTERN);
    protected static final int ANY_BRANCH_PATTERN_GROUP_CONDITION = 1;
    protected static final int ANY_BRANCH_PATTERN_GROUP_EL = 2;
    protected static final int ANY_BRANCH_PATTERN_GROUP_DEF = 3;
    protected static final int ANY_BRANCH_PATTERN_GROUP_NDEF = 4;
    protected static final int ANY_BRANCH_PATTERN_GROUP_ELSE = 5;
    protected static final int ANY_BRANCH_PATTERN_GROUP_ENDIF = 6;

    protected static final String IF_PATTERN = '^' + QUOTED_DIRECTIVE_PREFIX + "(?:el)?if\\s+(.+)" + EOL_PATTERN;
    protected static final Pattern IF_PATTERN_COMPILED = Pattern.compile(IF_PATTERN);

    protected static final String IFDEF_PATTERN = '^' + QUOTED_DIRECTIVE_PREFIX + "(?:el)?ifdef\\s+(" + MACRO_PATTERN + ')' + EOL_PATTERN;
    protected static final Pattern IFDEF_PATTERN_COMPILED = Pattern.compile(IFDEF_PATTERN);

    protected static final String IFNDEF_PATTERN = '^' + QUOTED_DIRECTIVE_PREFIX + "(?:el)?ifndef\\s+(" + MACRO_PATTERN + ')' + EOL_PATTERN;
    protected static final Pattern IFNDEF_PATTERN_COMPILED = Pattern.compile(IFNDEF_PATTERN);

    protected static final String ELSE_PATTERN = '^' + QUOTED_DIRECTIVE_PREFIX + "else" + EOL_PATTERN;
    protected static final Pattern ELSE_PATTERN_COMPILED = Pattern.compile(ELSE_PATTERN);

    protected static final String ENDIF_PATTERN = '^' + QUOTED_DIRECTIVE_PREFIX + "endif" + EOL_PATTERN;
    protected static final Pattern ENDIF_PATTERN_COMPILED = Pattern.compile(ENDIF_PATTERN);

    protected static final String EXPR_NUMBER_PATTERN = "-?\\d+";
    protected static final Pattern EXPR_NUMBER_PATTERN_COMPILED = Pattern.compile(EXPR_NUMBER_PATTERN);

    protected static final String EXPR_DEFINED_PATTERN = "defined\\((" + MACRO_PATTERN + ")\\)";
    protected static final Pattern EXPR_DEFINED_PATTERN_COMPILED = Pattern.compile(EXPR_DEFINED_PATTERN);

    protected static final String EXPR_TRIVIAL_PATTERN = "(?:" + MACRO_PATTERN + '|' + EXPR_NUMBER_PATTERN + '|' + EXPR_DEFINED_PATTERN + ')';
    protected static final String EXPR_SINGLE_PATTERN = "!?(?:" + MACRO_PATTERN + '|' + EXPR_NUMBER_PATTERN + '|' + EXPR_DEFINED_PATTERN + ')';

    protected static final String EXPR_NEGATION_PATTERN = "!(" + EXPR_TRIVIAL_PATTERN + ')';
    protected static final Pattern EXPR_NEGATION_PATTERN_COMPILED = Pattern.compile(EXPR_NEGATION_PATTERN);

    protected static final String[] EXPR_OPERATOR_PATTERNS_PRIORITIZED = {
            "[*/%]",
            "[+-]",
            "<<|>>>?",
            "[<>]=?",
            "[!=]=",
            "&",
            "\\^",
            "\\|",
            "&&",
            "\\|\\|",
    };
    protected static final String[] EXPR_OPERATION_PATTERNS_PRIORITIZED = Stream.of(EXPR_OPERATOR_PATTERNS_PRIORITIZED)
            .map(pat -> '(' + EXPR_SINGLE_PATTERN + ")\\s*(" + pat + ")\\s*(" + EXPR_SINGLE_PATTERN + ')')
            .toArray(String[]::new);
    protected static final Pattern[] EXPR_OPERATION_PATTERNS_COMPILED_PRIORITIZED = Stream.of(EXPR_OPERATION_PATTERNS_PRIORITIZED)
            .map(Pattern::compile)
            .toArray(Pattern[]::new);
    protected static final int EXPR_OPERATION_PATTERN_GROUP_A = 1;
    protected static final int EXPR_OPERATION_PATTERN_GROUP_B = 4;
    protected static final int EXPR_OPERATION_PATTERN_GROUP_OP = 3;

    protected static final String EXPR_PARENS_PATTERN = "\\(([^(]*?)\\)";
    protected static final Pattern EXPR_PARENS_PATTERN_COMPILED = Pattern.compile(EXPR_PARENS_PATTERN);

    /**
     * Loads and preprocesses a shader source file.
     * <p>
     * This will recursively load other included source files as needed.
     *
     * @param root   the location of the root source file
     * @param macros the initial set of preprocessor macros to use
     * @param loader a function to use for loading source files
     * @return the preprocessed source code
     */
    public static SourceLine[] loadAndPreprocess(@NonNull ResourceLocation root, @NonNull Map<String, Object> macros, @NonNull Function<ResourceLocation, SourceLine[]> loader) {
        return new Preprocessor(macros, loader)
                .appendLines(root)
                .preprocess()
                .lines();
    }

    protected final Map<String, Object> macros;
    protected final Function<ResourceLocation, SourceLine[]> loader;

    protected Node head;
    protected Node tail;
    protected int size;

    public Preprocessor(@NonNull Map<String, Object> macros, @NonNull Function<ResourceLocation, SourceLine[]> loader) {
        this.macros = new ObjObjOpenHashMap<>(macros);
        this.loader = loader;
    }

    //
    // EXTERNAL API
    //

    /**
     * Loads the source lines from the given {@link ResourceLocation} and appends them to the preprocessing buffer.
     *
     * @param location the location of the source file to load
     */
    public Preprocessor appendLines(@NonNull ResourceLocation location) {
        return this.appendLines(this.loader.apply(location));
    }

    /**
     * Appends the given source lines to the preprocessing buffer.
     *
     * @param lines the source lines
     */
    public Preprocessor appendLines(@NonNull SourceLine... lines) {
        Stream.of(lines).map(Node::new).forEach(this::insertAtBack);
        return this;
    }

    /**
     * Preprocesses this shader's source code.
     */
    public Preprocessor preprocess() {
        this.preprocessImpl();
        return this;
    }

    /**
     * @return all of the current source lines
     */
    public SourceLine[] lines() {
        SourceLine[] lines = new SourceLine[this.size];
        int idx = 0;
        for (Node node = this.head; node != null; node = node.next, idx++) {
            lines[idx] = node.line;
        }
        return lines;
    }

    //
    // INTERNAL: PREPROCESSOR IMPLEMENTATION
    //

    protected void preprocessImpl() {
        for (Node node = this.head; node != null; ) {
            if (!this.isDirectiveCandidate(node)) { //node isn't a potential preprocessor directive, skip it
                this.substituteMacros(node);

                node = node.next;
                continue;
            }

            node = this.processDirective(node, this.extractDirectiveName(node));
        }
    }

    protected void substituteMacros(@NonNull Node node) {
        String text = node.line.text();
        for (Matcher matcher = MACRO_WORD_PATTERN_COMPILED.matcher(text); matcher.find(); matcher.reset(text)) {
            StringBuffer buffer = new StringBuffer();
            do {
                String key = matcher.group();
                matcher.appendReplacement(buffer, this.macros.getOrDefault(key, key).toString());
            } while (matcher.find());
            matcher.appendTail(buffer);

            String newText = buffer.toString();
            if (text.equals(newText)) { //nothing changed
                break;
            }
            text = newText;
        }

        node.line = node.line.withText(text);
    }

    protected boolean isDirectiveCandidate(@NonNull Node node) {
        String text = node.line.text();
        return !text.isEmpty() && text.charAt(0) == DIRECTIVE_PREFIX;
    }

    protected String extractDirectiveName(@NonNull Node node) {
        return this.matcherFor(DIRECTIVE_NAME_PATTERN_COMPILED, node.line).group();
    }

    protected Node processDirective(@NonNull Node node, @NonNull String directiveName) {
        switch (directiveName) {
            case "define":
                return this.processDefine(node);
            case "undef":
                return this.processUndef(node);
            case "include":
                return this.processInclude(node);
            case "error":
                return this.processError(node);
            case "if":
            case "ifdef":
            case "ifndef":
                return this.processIf(node);
            case "elif":
            case "elifdef":
            case "elifndef":
            case "else":
            case "endif":
                throw new IllegalArgumentException(node.line.toString("dangling " + directiveName));
            case "version": //we don't want to process these directives ourselves, so we ignore them to let GLSL process them
                return node.next;
            default:
                throw new IllegalArgumentException(node.line.toString("invalid preprocessor directive"));
        }
    }

    protected Node processDefine(@NonNull Node node) {
        //TODO: support multiline macros and macros with parameters?
        Matcher matcher = this.matcherFor(DEFINE_PATTERN_COMPILED, node.line);
        String key = matcher.group(1);
        String value = matcher.group(2);

        if (this.macros.putIfAbsent(key, value) != null) {
            throw new IllegalArgumentException(node.line.toString("attempted to redefine macro " + key));
        }

        return this.removeAndGetNext(node);
    }

    protected Node processUndef(@NonNull Node node) {
        Matcher matcher = this.matcherFor(UNDEF_PATTERN_COMPILED, node.line);
        String key = matcher.group(1);

        this.macros.remove(key);

        return this.removeAndGetNext(node);
    }

    protected Node processInclude(@NonNull Node node) {
        Matcher matcher = this.matcherFor(INCLUDE_PATTERN_COMPILED, node.line);
        String absolutePath = matcher.group(INCLUDE_PATTERN_GROUP_ABSOLUTE);
        String relativePath = matcher.group(INCLUDE_PATTERN_GROUP_RELATIVE);

        //compute source file location
        ResourceLocation location;
        if (absolutePath != null) {
            location = new ResourceLocation(absolutePath);
        } else if (relativePath != null) {
            location = null; //TODO
        } else {
            throw new IllegalStateException(node.line.toString());
        }

        //load source lines and append them after the current node
        SourceLine[] lines = this.loader.apply(location);
        Stream.of(lines).map(Node::new).reduce(node, this::insertAfter);

        //delete starting node
        return this.removeAndGetNext(node);
    }

    protected Node processError(@NonNull Node node) {
        throw new IllegalStateException(node.line.toString(this.matcherFor(ERROR_PATTERN_COMPILED, node.line).group(1) + '\n', false));
    }

    protected Node processIf(@NonNull Node node) {
        class BranchTree {
            final Node[] nodes;
            final Node tailNode;

            final int selectedBranch;

            public BranchTree(@NonNull Node node, boolean evaluate) {
                List<Node> nodes = new ArrayList<>();
                Node tailNode;
                int selectedBranch = -1;

                Matcher matcher = ANY_CONDITION_PATTERN_COMPILED.matcher("");

                while (true) {
                    if (Preprocessor.this.isDirectiveCandidate(node)  //this node could be a condition directive
                        && matcher.reset(node.line.text()).find()) { //this node is a branch-related directive
                        boolean condition = matcher.group(ANY_BRANCH_PATTERN_GROUP_CONDITION) != null;
                        boolean el = matcher.group(ANY_BRANCH_PATTERN_GROUP_EL) != null;
                        boolean def = matcher.group(ANY_BRANCH_PATTERN_GROUP_DEF) != null;
                        boolean ndef = matcher.group(ANY_BRANCH_PATTERN_GROUP_NDEF) != null;
                        boolean _else = matcher.group(ANY_BRANCH_PATTERN_GROUP_ELSE) != null;
                        boolean endif = matcher.group(ANY_BRANCH_PATTERN_GROUP_ENDIF) != null;

                        if (nodes.isEmpty()) { //this is the first node processed so far
                            if (!condition) {
                                throw new IllegalStateException(node.line.toString("first directive in branch expression isn't a condition"));
                            } else if (el) {
                                throw new IllegalStateException(node.line.toString("first directive in branch expression is an else"));
                            }
                        }

                        if (condition) { //currently at a condition (if/ifdef/elif/elifdef)
                            if (!nodes.isEmpty() && !el) { //this is the beginning of a new subtree
                                //build the subtree recursively so that we can skip to the end of it
                                node = new BranchTree(node, false).tailNode;
                            } else {
                                nodes.add(node);

                                if (evaluate //we were requested to evaluate branch expressions
                                    && selectedBranch < 0 //no branch has been selected yet
                                    && (def
                                        ? Preprocessor.this.evaluateIfdef(node) : ndef
                                        ? Preprocessor.this.evaluateIfndef(node)
                                        : Preprocessor.this.evaluateIf(node))) { //the condition is true
                                    selectedBranch = nodes.size() - 1;
                                }
                            }
                        } else if (_else) {
                            Preprocessor.this.matcherFor(ELSE_PATTERN_COMPILED, node.line);
                            nodes.add(node);

                            if (evaluate //we were requested to evaluate branch expressions
                                && selectedBranch < 0) { //no branch has been selected yet
                                selectedBranch = nodes.size() - 1;
                            }
                        } else if (endif) {
                            Preprocessor.this.matcherFor(ENDIF_PATTERN_COMPILED, node.line);
                            nodes.add(node);

                            tailNode = node;
                            break;
                        }
                    } else if (nodes.isEmpty()) {
                        throw new IllegalStateException(node.line.toString("first directive in branch expression isn't a condition"));
                    }

                    node = node.next;
                    if (node == null) {
                        throw new IllegalStateException(nodes.get(nodes.size() - 1).line.toString("EOF reached before block could be closed"));
                    }
                }

                checkState(nodes.size() >= 2, "must have at least 2 nodes!");

                this.nodes = nodes.toArray(new Node[0]);
                this.tailNode = tailNode;
                this.selectedBranch = selectedBranch;
            }

            public Node remove() {
                if (this.selectedBranch < 0) { //no conditions were true, remove every node and return the last one
                    return Preprocessor.this.removeRangeInclusiveAndGetNext(this.nodes[0], this.tailNode);
                } else { //one of the conditions matched, remove everything before and after and return the first line inside the matching block
                    //remove all blocks after the matched one
                    Preprocessor.this.removeRangeInclusiveAndGetNext(this.nodes[this.selectedBranch + 1], this.tailNode);

                    //remove all blocks up to the matched one
                    return Preprocessor.this.removeRangeInclusiveAndGetNext(this.nodes[0], this.nodes[this.selectedBranch]);
                }
            }
        }

        return new BranchTree(node, true).remove();
    }

    protected boolean evaluateIfdef(@NonNull Node node) {
        Matcher matcher = this.matcherFor(IFDEF_PATTERN_COMPILED, node.line);
        String key = matcher.group(1);
        return this.macros.containsKey(key);
    }

    protected boolean evaluateIfndef(@NonNull Node node) {
        Matcher matcher = this.matcherFor(IFNDEF_PATTERN_COMPILED, node.line);
        String key = matcher.group(1);
        return !this.macros.containsKey(key);
    }

    protected boolean evaluateIf(@NonNull Node node) {
        Matcher matcher = this.matcherFor(IF_PATTERN_COMPILED, node.line);
        return this.evaluateExpr(matcher.group(1)) != 0L;
    }

    protected long evaluateExpr(@NonNull String expr) {
        expr = expr.trim();
        Matcher matcher;

        for (String prevExpr = null; ; checkArg(!expr.equals(prevExpr), "failed to simplify expression: "), prevExpr = expr) {
            //if expression is a simple expression, evaluate that part first
            if (MACRO_PATTERN_COMPILED.matcher(expr).matches()) { //value is a simple macro
                return this.evaluateMacro(expr);
            } else if (EXPR_NUMBER_PATTERN_COMPILED.matcher(expr).matches()) { //value is a number
                return Long.parseLong(expr);
            } else if ((matcher = EXPR_NEGATION_PATTERN_COMPILED.matcher(expr)).matches()) { //!expression
                return this.evaluateExpr(matcher.group(1)) == 0L ? 1L : 0L;
            }

            //defined(MACRO)
            for (matcher = EXPR_DEFINED_PATTERN_COMPILED.matcher(expr); matcher.find(); matcher.reset(expr)) {
                StringBuffer buffer = new StringBuffer();
                do {
                    matcher.appendReplacement(buffer, this.macros.containsKey(matcher.group(1)) ? "1" : "0");
                } while (matcher.find());
                matcher.appendTail(buffer);
                expr = buffer.toString().trim();
            }

            //evaluate parentheses
            for (matcher = EXPR_PARENS_PATTERN_COMPILED.matcher(expr); matcher.find(); matcher.reset(expr)) {
                StringBuffer buffer = new StringBuffer();
                do {
                    matcher.appendReplacement(buffer, String.valueOf(this.evaluateExpr(matcher.group(1))));
                } while (matcher.find());
                matcher.appendTail(buffer);
                expr = buffer.toString().trim();
            }

            //expression <operator> expression
            for (Pattern pattern : EXPR_OPERATION_PATTERNS_COMPILED_PRIORITIZED) {
                for (matcher = pattern.matcher(expr); matcher.find(); matcher.reset(expr)) {
                    StringBuffer buffer = new StringBuffer();
                    do {
                        matcher.appendReplacement(buffer, String.valueOf(
                                this.evalOperation(
                                        this.evaluateExpr(matcher.group(EXPR_OPERATION_PATTERN_GROUP_A)),
                                        this.evaluateExpr(matcher.group(EXPR_OPERATION_PATTERN_GROUP_B)),
                                        matcher.group(EXPR_OPERATION_PATTERN_GROUP_OP))));
                    } while (matcher.find());
                    matcher.appendTail(buffer);
                    expr = buffer.toString().trim();
                }
            }
        }
    }

    protected long evalOperation(long a, long b, @NonNull String operation) {
        switch (operation) {
            case "+":
                return Math.addExact(a, b);
            case "-":
                return Math.subtractExact(a, b);
            case "*":
                return Math.multiplyExact(a, b);
            case "/":
                return a / b;
            case "%":
                return a % b;
            case "&":
                return a & b;
            case "|":
                return a | b;
            case "^":
                return a ^ b;
            case "==":
                return a == b ? 1L : 0L;
            case "!=":
                return a != b ? 1L : 0L;
            case "<":
                return a < b ? 1L : 0L;
            case ">":
                return a > b ? 1L : 0L;
            case "<=":
                return a <= b ? 1L : 0L;
            case ">=":
                return a >= b ? 1L : 0L;
            case "&&":
                return a != 0L && b != 0L ? 1L : 0L;
            case "||":
                return (a | b) != 0L ? 1L : 0L;
            case "<<":
                return a << b;
            case ">>":
                return a >> b;
            case ">>>":
                return a >>> b;
            default: //impossible
                throw new IllegalArgumentException("invalid operation: " + operation);
        }
    }

    protected long evaluateMacro(@NonNull String key) {
        Object value = this.macros.get(key);
        if (value == null) {
            value = this.macros.containsKey(key);
        }

        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof Boolean) {
            return (Boolean) value ? 1L : 0L;
        } else if (value instanceof String) {
            return this.evaluateExpr((String) value);
        } else {
            throw new IllegalArgumentException("invalid type for " + key + ": " + PorkUtil.className(value));
        }
    }

    //
    // INTERNAL: UTILITY METHODS
    //

    protected Matcher matcherFor(@NonNull Pattern pattern, @NonNull SourceLine line) {
        return this.matcherFor(pattern, line, "syntax error");
    }

    protected Matcher matcherFor(@NonNull Pattern pattern, @NonNull SourceLine line, @NonNull String errorMsg) {
        Matcher matcher = pattern.matcher(line.text());
        if (!matcher.find()) {
            throw new IllegalArgumentException(line.toString(errorMsg));
        }
        return matcher;
    }

    protected Matcher matcherFor(@NonNull Matcher matcher, @NonNull SourceLine line) {
        return this.matcherFor(matcher, line, "syntax error");
    }

    protected Matcher matcherFor(@NonNull Matcher matcher, @NonNull SourceLine line, @NonNull String errorMsg) {
        if (!matcher.reset(line.text()).find()) {
            throw new IllegalArgumentException(line.toString(errorMsg));
        }
        return matcher;
    }

    //
    // INTERNAL: LINKED LIST API
    //

    protected void connect(@NonNull Node first, @NonNull Node second) {
        assert first.next == null : "first node already connected!";
        assert second.prev == null : "second node already connected!";

        first.next = second;
        second.prev = first;
    }

    protected Node insertAtBack(@NonNull Node node) {
        if (this.size++ == 0) { //list is empty
            this.head = this.tail = node.beginUsing();
        } else {
            this.connect(this.tail, node.beginUsing());
            this.tail = node;
        }
        return node;
    }

    protected Node insertAfter(@NonNull Node prev, @NonNull Node node) {
        Node next = prev.next;

        if (next == null) { //existing node is the tail
            return this.insertAtBack(node);
        } else {
            this.size++;
            next.prev = prev.next = node.beginUsing();
            node.prev = prev;
            node.next = next;
        }
        return node;
    }

    protected void remove(@NonNull Node node) {
        node.stopUsing();
        this.size--;

        Node prev = node.prev;
        Node next = node.next;

        if (prev == null) { //this node is at the head
            this.head = next;
        } else {
            prev.next = next;
        }

        if (next == null) { //this node is at the tail
            this.tail = prev;
        } else {
            next.prev = prev;
        }
    }

    protected Node removeAndGetPrev(@NonNull Node node) {
        this.remove(node);
        return node.prev;
    }

    protected Node removeAndGetNext(@NonNull Node node) {
        this.remove(node);
        return node.next;
    }

    protected Node removeRangeInclusiveAndGetNext(@NonNull Node firstNode, @NonNull Node lastNode) {
        for (Node node = firstNode; node != lastNode; node = this.removeAndGetNext(node)) {
        }
        return this.removeAndGetNext(lastNode);
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    protected static class Node {
        @NonNull
        protected SourceLine line;

        protected Node prev;
        protected Node next;

        protected boolean used = false;

        protected Node beginUsing() {
            assert !this.used : "node is already being used?!?";
            this.used = true;

            return this;
        }

        protected Node stopUsing() {
            assert this.used : "node isn't being used?!?";
            this.used = false;

            return this;
        }
    }
}
