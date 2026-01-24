package com.app.order.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.concurrent.ExecutorCompletionService;

public class StructuredConcurrencyHelper {

    private static final ExecutorService VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Execute two tasks in parallel, fail if any fails
     */
    public static <A, B> Pair<A, B> parallel(Supplier<A> task1, Supplier<B> task2) throws Exception {
        var future1 = VIRTUAL_EXECUTOR.submit(task1::get);
        var future2 = VIRTUAL_EXECUTOR.submit(task2::get);
        return new Pair<>(future1.get(), future2.get());
    }

    /**
     * Execute three tasks in parallel, fail if any fails
     */
    public static <A, B, C> Triple<A, B, C> parallel(Supplier<A> task1, Supplier<B> task2, Supplier<C> task3)
            throws Exception {
        var future1 = VIRTUAL_EXECUTOR.submit(task1::get);
        var future2 = VIRTUAL_EXECUTOR.submit(task2::get);
        var future3 = VIRTUAL_EXECUTOR.submit(task3::get);
        return new Triple<>(future1.get(), future2.get(), future3.get());
    }

    /**
     * Execute four tasks in parallel, fail if any fails
     */
    public static <A, B, C, D> Quad<A, B, C, D> parallel(Supplier<A> task1, Supplier<B> task2, Supplier<C> task3,
            Supplier<D> task4) throws Exception {
        var future1 = VIRTUAL_EXECUTOR.submit(task1::get);
        var future2 = VIRTUAL_EXECUTOR.submit(task2::get);
        var future3 = VIRTUAL_EXECUTOR.submit(task3::get);
        var future4 = VIRTUAL_EXECUTOR.submit(task4::get);
        return new Quad<>(future1.get(), future2.get(), future3.get(), future4.get());
    }

    /**
     * Execute multiple tasks, return first successful result
     */
    @SafeVarargs
    public static <T> T firstSuccess(Supplier<T>... tasks) throws Exception {
        // Simulating ShutdownOnSuccess (first to finish)
        var completionService = new ExecutorCompletionService<T>(VIRTUAL_EXECUTOR);
        for (var task : tasks)
            completionService.submit(task::get);
        return completionService.take().get();
    }

    // Result containers
    public record Pair<A, B>(A first, B second) {
    }

    public record Triple<A, B, C>(A first, B second, C third) {
    }

    public record Quad<A, B, C, D>(A first, B second, C third, D fourth) {
    }
}
