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

package net.daporkchop.fp2.core.util.threading.scheduler;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.daporkchop.fp2.core.util.threading.BlockingSupport;
import net.daporkchop.fp2.core.util.threading.workergroup.WorkerGroup;
import net.daporkchop.fp2.core.util.threading.workergroup.WorkerGroupBuilder;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.reference.cache.Cached;
import net.daporkchop.lib.primitive.map.open.ObjObjOpenHashMap;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.Math.*;
import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Implementation of {@link Scheduler} whose {@link CompletableFuture}s are shared for all occurrences of the same parameter value.
 * <p>
 * This implementation supports all features defined by {@link Scheduler}. However, recursive tasks can cause deadlocks if the dependency chain has a loop, and
 * large volumes of recursive tasks can quickly run the system out of memory or cause the worker threads' stacks to overflow. It is therefore recommended to use
 * {@link ApproximatelyPrioritizedSharedFutureScheduler} where possible.
 *
 * @author DaPorkchop_
 */
public class SharedFutureScheduler<P, V> implements Scheduler<P, V>, Runnable {
    protected static final long TASK_DEPENDENCIES_OFFSET = PUnsafe.pork_getOffset(SharedFutureScheduler.Task.class, "dependencies");

    protected static final boolean DEBUG_PRINTS_ENABLED = Boolean.parseBoolean(System.getProperty("fp2.SharedFutureScheduler.debugPrintsEnabled", "true"));

    protected final Map<P, Task> tasks = new ConcurrentHashMap<>();
    protected final BlockingQueue<Task> queue = this.createTaskQueue();

    protected final Cached<Deque<Task>> recursionStack = Cached.threadLocal(this.recursionStackFactory());

    protected final WorkFunction<P, V> function;

    protected final WorkerGroup group;
    protected volatile boolean running = true;

    public SharedFutureScheduler(@NonNull Function<? super SharedFutureScheduler<P, V>, WorkFunction<P, V>> functionFactory, @NonNull WorkerGroupBuilder builder) {
        this.function = functionFactory.apply(this);

        this.group = builder.build(this);
    }

    /**
     * Checks whether or not a worker is allowed to recurse into execution of a task with the given destination parameter from the given source parameter.
     *
     * @param from the parameter of the task currently being executed by the worker
     * @param to   the parameter to recurse to
     * @return whether or not the recursion is allowed
     */
    protected boolean canRecurse(@NonNull P from, @NonNull P to) {
        return true;
    }

    /**
     * Checks whether or not a worker is allowed to recurse into execution of a batch task with the given destination parameters from the given source parameter.
     *
     * @param from the parameter of the task currently being executed by the worker
     * @param tos  the parameters of the batch to recurse to
     * @return whether or not the recursion is allowed
     */
    protected boolean canRecurseToBatch(@NonNull P from, @NonNull List<P> tos) {
        return tos.stream().allMatch(to -> this.canRecurse(from, to));
    }

    /**
     * Checks whether or not all of the parameters in the given list are allowed to be executed in the same batch.
     *
     * @param parameters the list of parameters in the batch. It is safe to assume that there are no duplicate elements in the list.
     * @return whether or not all of the parameters in the given list are allowed to be executed in the same batch
     */
    protected boolean canExecuteInBatch(@NonNull List<P> parameters) {
        checkArg(!parameters.isEmpty(), "a batch must consist of at least one parameter!");
        return true;
    }

    protected Supplier<Deque<Task>> recursionStackFactory() {
        return ArrayDeque::new;
    }

    protected BlockingQueue<Task> createTaskQueue() {
        return new LinkedBlockingQueue<>();
    }

    protected Task createTask(@NonNull P param) {
        return new Task(param);
    }

    protected void enqueue(@NonNull Task task) {
        checkState(SharedFutureScheduler.this.queue.add(task));
    }

    protected void unqueue(@NonNull Task task) {
        //do nothing - the task will be cancelled, and the worker threads will remove it from the queue once we get to it.
        //  we don't want to actually remove it from the queue, since LinkedBlockingQueue#remove(Object) is O(n).
    }

    @Override
    public CompletableFuture<V> schedule(@NonNull P param) {
        return this.retainTask(param);
    }

    @Override
    public void close() {
        //notify workers that we're shutting down
        this.running = false;

        //wait until all the workers have exited
        this.group.close();
    }

    protected Task retainTask(@NonNull P _param) {
        class State implements BiFunction<P, Task, Task> {
            Task task;

            @Override
            public Task apply(@NonNull P param, Task task) {
                if (task == null) { //task doesn't exist, create new one
                    if (DEBUG_PRINTS_ENABLED) {
                        fp2().log().info("retain: creating new task at %s, was previously null", param);
                    }

                    task = SharedFutureScheduler.this.createTask(param);

                    //add task to execution queue
                    SharedFutureScheduler.this.enqueue(task);
                } else if (task.refCnt < 0) { //task is currently being executed, we want to replace it to force it to be re-enqueued later
                    if (DEBUG_PRINTS_ENABLED) {
                        fp2().log().info("retain: creating new task at %s, was previously %s", param, task);
                    }

                    Task previousTask = task;
                    task = SharedFutureScheduler.this.createTask(param);

                    //remember the previous task instance for later
                    task.previous = previousTask;
                } else { //retain existing task
                    task.refCnt = incrementExact(task.refCnt);

                    if (DEBUG_PRINTS_ENABLED) {
                        fp2().log().info("retain: retained existing task at %s, reference count is now %d", param, task.refCnt);
                    }
                }

                this.task = task;
                return task;
            }
        }

        State state = new State();
        this.tasks.compute(_param, state);
        return state.task;
    }

    protected boolean releaseTask(@NonNull Task expectedTask) {
        class State implements BiFunction<P, Task, Task> {
            Task task;
            boolean released;

            @Override
            public Task apply(@NonNull P param, Task task) {
                if (task != expectedTask) { //tasks don't match, do nothing
                    if (DEBUG_PRINTS_ENABLED) {
                        fp2().log().info("release: failed to release task at %s, %s != %s", param, task, expectedTask);
                    }

                    this.released = false;
                    return task;
                } else {
                    this.released = true;
                }

                if (task.refCnt > 0) { //the task isn't being actively executed
                    if (--task.refCnt != 0) { //reference count is non-zero, the task is still live
                        if (DEBUG_PRINTS_ENABLED) {
                            fp2().log().info("release: partially released task at %s, reference count is now %d", param, task.refCnt);
                        }
                        return task;
                    } else { //the reference count reached zero! cancel the task, unqueue it and remove it from the map
                        if (DEBUG_PRINTS_ENABLED) {
                            fp2().log().info("release: totally released task at %s, replacing with %d", param, task.previous);
                        }

                        SharedFutureScheduler.this.unqueue(task);
                        task.cancel0();

                        //save the task so we can cancel its dependents later (without holding a lock on the map entry)
                        this.task = task;

                        //restore the previous task, which will generally be null but could be non-null if this is a re-scheduled task which was cancelled again
                        //  before the original task was finished
                        return task.previous;
                    }
                } else { //task is already being executed, it can't be cancelled now so we just do nothing
                    return task;
                }
            }

            boolean finish() {
                if (this.task != null) { //the task was removed, cancel it
                    //we cancel the task here so that it doesn't happen inside of the ConcurrentHashMap#compute function, which doesn't like being
                    //  called multiple times from the same thread.
                    List<Task> dependencies = PUnsafe.pork_swapObject(this.task, TASK_DEPENDENCIES_OFFSET, null);
                    if (dependencies != null) { //the task was recursive and had some dependencies, let's release them since they aren't actually needed by this task any more
                        for (Task dependency : dependencies) {
                            SharedFutureScheduler.this.releaseTask(dependency);
                        }
                    }
                }
                return this.released;
            }
        }

        State state = new State();
        this.tasks.compute(expectedTask.param, state);
        return state.finish();
    }

    protected boolean beginTask(@NonNull Task expectedTask) {
        class State implements BiFunction<P, Task, Task> {
            boolean started;

            @Override
            public Task apply(@NonNull P param, Task task) {
                if (task != expectedTask //tasks don't match, do nothing
                    || task.refCnt < 0) { //task is currently being executed, we can't start executing it
                    if (DEBUG_PRINTS_ENABLED) {
                        if (task != expectedTask) {
                            fp2().log().info("begin: couldn't begin task at %s (mismatch)", param);
                        } else {
                            fp2().log().info("begin: couldn't begin task at %s (already executing)", param);
                        }
                    }

                    this.started = false;
                } else {
                    checkState(task.refCnt != 0);

                    if (DEBUG_PRINTS_ENABLED) {
                        fp2().log().info("begin: began executing task at %s", param);
                    }

                    //set reference count to -1 to indicate that it's started execution
                    task.refCnt = -1;
                    this.started = true;

                    //remove task from execution queue to prevent another thread from trying to start it
                    SharedFutureScheduler.this.unqueue(task);
                }
                return task;
            }
        }

        State state = new State();
        this.tasks.compute(expectedTask.param, state);
        return state.started;
    }

    protected void deleteTask(@NonNull Task expectedTask) {
        this.tasks.compute(expectedTask.param, (param, task) -> {
            checkState(task != null, "task for %s was already removed!", param);

            if (task == expectedTask) { //task remains unchanged, delete it
                if (DEBUG_PRINTS_ENABLED) {
                    fp2().log().info("delete: removed completed task at %s", param);
                }
                return null;
            } else { //tasks don't match, meaning the task has been re-scheduled
                if (DEBUG_PRINTS_ENABLED) {
                    fp2().log().info("delete: task at %s (%s) was re-scheduled during execution, adding %s to queue", param, expectedTask, task);
                }

                //new task must reference the task which was completed, let's clear that reference
                checkState(task.previous == expectedTask, "previous task for %s is not the expected task!", param);
                task.previous = null;

                //enqueue the new task and leave it in the map
                SharedFutureScheduler.this.enqueue(task);
                return task;
            }
        });
    }

    /**
     * Attempts to acquire a task in order to execute it as part of a batch.
     * <p>
     * If no matching task exists, a new one will be created and begun. If a task already exists and has not yet begun, it will be started. If a task exists and is already started,
     * the acquisition will fail.
     *
     * @param _param the parameter to acquire a task for
     * @return the acquired task, or {@code null} if acquisition failed
     */
    protected Task acquireTaskForBatch(@NonNull P _param) {
        class State implements BiFunction<P, Task, Task> {
            Task task;

            @Override
            public Task apply(@NonNull P param, Task task) {
                if (task == null) { //task doesn't exist, create new one
                    if (DEBUG_PRINTS_ENABLED) {
                        fp2().log().info("acquire: creating new task at %s, was previously null", param);
                    }

                    task = SharedFutureScheduler.this.createTask(param);

                    //set reference count to -1 to indicate that it's started execution
                    task.refCnt = -1;

                    //save task instance to return from outer method
                    this.task = task;
                } else if (task.refCnt < 0) { //task is currently being executed, acquisition failed
                    if (DEBUG_PRINTS_ENABLED) {
                        fp2().log().info("acquire: failed to acquire task at %s", param);
                    }

                    //return null from outer method
                    this.task = null;
                } else { //task is currently still queued, begin executing it
                    checkState(task.refCnt != 0);

                    //the previous task is non-null, therefore it's still running and we aren't allowed to start this one yet
                    if (task.previous != null) {
                        if (DEBUG_PRINTS_ENABLED) {
                            fp2().log().info("acquire: previous task at %s is still active", param);
                        }

                        //exit and keep existing value without changing anything
                        return task;
                    }

                    if (DEBUG_PRINTS_ENABLED) {
                        fp2().log().info("acquire: began executing task at %s", param);
                    }

                    //set reference count to -1 to indicate that it's started execution
                    task.refCnt = -1;

                    //remove task from execution queue to prevent another thread from trying to start it
                    SharedFutureScheduler.this.unqueue(task);

                    //save task instance to return from outer method
                    this.task = task;
                }

                return task;
            }
        }

        State state = new State();
        this.tasks.compute(_param, state);
        return state.task;
    }

    protected void pollAndExecuteSingleTask() {
        if (!this.running) {
            throw new SchedulerClosedError();
        }

        Task task = this.pollSingleTask();
        if (task == null //queue is empty
            || !this.beginTask(task)) { //we lost the "race" to begin executing the task
            return;
        }

        this.executeTask(task);
    }

    @SneakyThrows(InterruptedException.class)
    protected Task pollSingleTask() {
        //poll the queue, but don't wait indefinitely because we need to be able to exit if the executor stops running.
        // we don't want to use interrupts because they can cause unwanted side-effects (such as closing NIO channels).
        return this.queue.poll(1L, TimeUnit.SECONDS);
    }

    protected void executeTask(@NonNull Task initialTask) {
        if (!this.running) {
            throw new SchedulerClosedError();
        }

        Deque<Task> recursionStack = this.recursionStack.get();
        if (!recursionStack.isEmpty()) { //ensure that this recursion is actually legal
            P from = recursionStack.peekFirst().param;
            checkState(this.canRecurse(from, initialTask.param), "recursion from %s to %s is not permitted!", from, initialTask.param);
        }

        //begin recursion into task
        recursionStack.push(initialTask);

        //keep track of all the tasks being executed
        List<Task> allTasks = new ArrayList<>();
        allTasks.add(initialTask);

        try { //execute the task and complete future accordingly
            Map<P, V> values = new ObjObjOpenHashMap<>();
            values.put(initialTask.param, null);

            if (DEBUG_PRINTS_ENABLED) {
                fp2().log().info("execute: beginning to execute %s", initialTask.param);
            }

            this.function.work(initialTask.param, new Callback<P, V>() {
                @Override
                public void complete(@NonNull P param, @NonNull V value) {
                    checkArg(values.containsKey(param), "completed invalid parameter! got %s, expected one of %s", param, values.keySet());
                    checkState(values.put(param, value) == null, "parameter %s was already completed!", param);

                    if (DEBUG_PRINTS_ENABLED) {
                        fp2().log().info("callback complete: completed %s", param);
                    }
                }

                @Override
                public List<P> acquire(@NonNull Iterable<P> params, @NonNull AcquisitionStrategy strategy) {
                    List<P> acquired = new ArrayList<>();

                    params.forEach(param -> {
                        Task task;
                        switch (strategy) {
                            default: //the strategy is a hint, so fall back to any approach
                            case TRY_STEAL_EXISTING_OR_CREATE:
                                task = SharedFutureScheduler.this.acquireTaskForBatch(param);
                                break;
                        }

                        if (task != null) { //we were able to successfully acquire the task
                            checkState(!values.containsKey(task.param), "parameter %s was acquired twice?!?", param);

                            values.put(task.param, null);
                            allTasks.add(task);
                            acquired.add(param);
                        }
                    });

                    if (DEBUG_PRINTS_ENABLED) {
                        List<P> paramsAsList = new ArrayList<>();
                        params.forEach(paramsAsList::add);
                        fp2().log().info("callback acquire: acquired %d/%d tasks using %s (%s/%s, total %s)", acquired.size(), paramsAsList.size(), strategy, acquired, paramsAsList, values.keySet());
                    }

                    return acquired;
                }
            });

            if (DEBUG_PRINTS_ENABLED) {
                if (values.size() == 1) {
                    fp2().log().info("execute: finished execution of %s, no additional parameters were acquired", initialTask.param);
                } else {
                    fp2().log().info("execute: finished execution of %s, some additional parameters were acquired: ", initialTask.param, values.keySet());
                }
            }

            //complete the futures
            for (Task task : allTasks) {
                V value = values.get(task.param);
                checkState(value != null, "parameter %s was not completed!", task.param);

                //task was completed normally, set the future's result value
                task.complete(value);
            }
        } catch (SchedulerClosedError e) { //catch and rethrow this separately to prevent it from being used to complete the future
            allTasks.forEach(Task::cancel0); //cancel the futures to make sure they are all completed
            throw e;
        } catch (RecursiveTaskCancelledError e) { //a dependent task was cancelled, which likely means this one was too (but we need to make sure of it)
            if (allTasks.stream().noneMatch(Task::isCancelled)) { //this should be impossible
                allTasks.forEach(task -> task.completeExceptionally(e));
                this.group.manager().handle(e);
            }
        } catch (Throwable t) {
            allTasks.forEach(task -> task.completeExceptionally(t));
            if (this.running) { //only handle the exception if we aren't already shutting the scheduler down
                this.group.manager().handle(t);
            }
        } finally {
            { //recursion is complete, so pop the top off the recursion stack
                Task popped = recursionStack.pollFirst();
                checkState(initialTask == popped, "recursion stack is in invalid state: popped %s, but expected %s!", popped, initialTask);
            }

            //the task's been executed, remove it from the map
            allTasks.forEach(this::deleteTask);
        }
    }

    /**
     * @deprecated internal API, do not touch!
     */
    @Override
    @Deprecated
    public void run() {
        try {
            while (true) {
                this.pollAndExecuteSingleTask();
            }
        } catch (SchedulerClosedError e) {
            //swallow error and exit quietly
        }
    }

    protected void awaitJoin(@NonNull Task task) {
        //we don't want to actually BLOCK the worker thread while waiting for a task to complete! that would be stupid, let's make it do some actual work instead.

        //it would be best if we could start by executing the task we want to wait for, so let's start by trying to begin it now
        if (this.beginTask(task)) { //we won the race to execute the task! actually execute it and return immediately, nothing else remains to be done
            this.executeTask(task);
        } else { //keep ourselves occupied by doing other tasks until the one we're waiting for is completed
            while (!task.isDone()) {
                this.pollAndExecuteSingleTask();
            }
        }
    }

    @Override
    public List<V> scatterGather(@NonNull List<P> params) {
        List<Task> tasks = this.scatter(params);

        Deque<Task> recursionStack = this.recursionStack.get();
        Task parent = recursionStack.peek();
        if (parent != null) { //this is a recursive task, so we need to make sure the parent task is aware that it's resulted in child tasks being spawned
            if (!PUnsafe.compareAndSwapObject(parent, TASK_DEPENDENCIES_OFFSET, null, tasks)) { //there may only be one active scatter/gather per task at a time
                tasks.forEach(this::releaseTask);
                throw new IllegalStateException(PStrings.fastFormat("task for %s has already started recursion!", parent.param));
            }

            try {
                return this.gather(tasks);
            } finally {
                if (PUnsafe.compareAndSwapObject(parent, TASK_DEPENDENCIES_OFFSET, tasks, null)) { //don't release dependencies if they've already been released from
                    //  another thread (due to this task's cancellation)
                    tasks.forEach(this::releaseTask);
                }
            }
        } else { //top-level task, do a simple scatter/gather
            try {
                return this.gather(tasks);
            } finally {
                tasks.forEach(this::releaseTask);
            }
        }
    }

    protected List<Task> scatter(@NonNull List<P> params) {
        List<Task> tasks = new ArrayList<>(params.size());
        for (P param : params) {
            tasks.add(this.retainTask(param));
        }
        return tasks;
    }

    protected List<V> gather(@NonNull List<Task> tasks) {
        //race to complete each task
        for (Task task : tasks) {
            if (this.beginTask(task)) {
                this.executeTask(task);
            }
        }

        //join all the tasks, which will block if necessary
        List<V> values = new ArrayList<>(tasks.size());
        for (SharedFutureScheduler<P, V>.Task task : tasks) {
            values.add(task.join());
        }
        return values;
    }

    /**
     * Thrown when a child task is cancelled during recursive execution in order to immediately break out of the parent task.
     *
     * @author DaPorkchop_
     */
    protected static class RecursiveTaskCancelledError extends Error {
        public RecursiveTaskCancelledError(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Thrown when a child task throws an exception during recursive execution.
     *
     * @author DaPorkchop_
     */
    protected static class RecursiveTaskException extends RuntimeException {
        public RecursiveTaskException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Thrown when the scheduler has closed in order to immediately terminate worker threads.
     *
     * @author DaPorkchop_
     */
    protected static class SchedulerClosedError extends Error {
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    protected class Task extends CompletableFuture<V> {
        @NonNull
        protected final P param;

        protected int refCnt = 1;

        protected Task previous; //if this task was created while a previous one was being executed, this field contains a reference to the previous one

        //list of tasks whose results are required for the successful execution of the current task
        protected volatile List<Task> dependencies = null;

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return SharedFutureScheduler.this.releaseTask(this);
        }

        protected void cancel0() {
            super.cancel(false);
        }

        @Override
        public V join() {
            if (SharedFutureScheduler.this.group.threads().contains(Thread.currentThread())) {
                //we're on a worker thread, which means this task is being waited on recursively!
                //  let's steal work from the execution queue until this task is completed.
                SharedFutureScheduler.this.awaitJoin(this);

                Task parent = SharedFutureScheduler.this.recursionStack.get().peekFirst();
                if (parent != null) { //this is a recursive job, we want special handling for exceptions
                    try {
                        return super.join();
                    } catch (CancellationException e) {
                        throw new RecursiveTaskCancelledError(PStrings.fastFormat("task %s (dependency of %s) was cancelled", this.param, parent.param), e);
                    } catch (Throwable t) {
                        throw new RecursiveTaskException(PStrings.fastFormat("task %s (dependency of %s) threw an exception", this.param, parent.param), t);
                    }
                } else {
                    return super.join();
                }
            } else { //not recursive, block normally
                return BlockingSupport.managedBlock(this);
            }
        }

        @Override
        public String toString() {
            return this.getClass().getTypeName() + '@' + Integer.toHexString(this.hashCode()) + ",param=" + this.param;
        }
    }

    /**
     * A user-defined function which computes a value based on a given parameter.
     *
     * @author DaPorkchop_
     */
    @FunctionalInterface
    public interface WorkFunction<P, V> {
        /**
         * Wraps a regular {@link Function} into a {@link WorkFunction}.
         *
         * @param function the {@link Function} to wrap
         * @return the wrapped {@link Function}
         */
        static <P, V> WorkFunction<P, V> wrap(@NonNull Function<P, V> function) {
            return (param, callback) -> callback.complete(param, function.apply(param));
        }

        /**
         * Calls the function with the given parameter.
         * <p>
         * <strong>Parameter ownership:</strong><br>
         * Every {@link WorkFunction} invocation "owns" one or more parameters. The function is responsible for {@link Callback#complete(Object, Object) providing a result value} for
         * every parameter it owns before the invocation returns. Initially, an invocation owns only a single parameter; the one which the function receives as a method argument.
         * Invocations can acquire ownership of additional parameters using {@link Callback#acquire(List, AcquisitionStrategy) the given callback}.
         * <p>
         * The scheduler will ensure that no single parameter is owned by more than one invocation at a time.
         *
         * @param param    the parameter
         * @param callback the {@link Callback} instance to notify with the task result
         */
        void work(@NonNull P param, @NonNull Callback<P, V> callback);
    }

    /**
     * A callback to which will be accessed by a {@link WorkFunction} to notify task completion results.
     *
     * @author DaPorkchop_
     */
    public interface Callback<P, V> {
        /**
         * Sets the result value for the given parameter.
         *
         * @param param the parameter
         * @param value the result value
         */
        void complete(@NonNull P param, @NonNull V value);

        /**
         * Tries to acquire ownership of the given parameters.
         *
         * @param params   the parameters to try to acquire ownership of
         * @param strategy the strategy to use for acquiring the parameters' ownership. Note that this is merely a hint, the implementation is free to handle this any way it chooses.
         * @return the parameters that were actually acquired
         */
        List<P> acquire(@NonNull Iterable<P> params, @NonNull AcquisitionStrategy strategy);
    }

    /**
     * Defines the strategies a {@link WorkFunction} invocation may use for acquiring additional parameters through {@link Callback#acquire(List, AcquisitionStrategy)}.
     *
     * @author DaPorkchop_
     */
    public enum AcquisitionStrategy {
        /**
         * Ownership of a parameter will be acquired according to the following rules:
         * <ul>
         *     <li>if the parameter is already scheduled, ownership will only be acquired if the parameter has not yet begun execution by any other thread.</li>
         *     <li>if the parameter is not already scheduled, ownership will not be acquired.</li>
         * </ul>
         */
        TRY_STEAL_EXISTING,
        /**
         * Ownership of a parameter will be acquired according to the following rules:
         * <ul>
         *     <li>if the parameter is already scheduled, ownership will only be acquired if the parameter has not yet begun execution by any other thread.</li>
         *     <li>if the parameter is not already scheduled, ownership will be acquired in any case.</li>
         * </ul>
         */
        TRY_STEAL_EXISTING_OR_CREATE;
    }
}
