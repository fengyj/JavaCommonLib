package me.fengyj.springdemo.utils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.fengyj.springdemo.exception.ErrorSeverity;
import me.fengyj.springdemo.exception.ExceptionUtils;
import me.fengyj.springdemo.exception.GeneralException;

public class ThreadUtils {

    private static final Logger logger = LoggerFactory.getLogger(ThreadUtils.class);

    private static final BiConsumer<Thread, Throwable> defaultUncaughtExceptionHandler = (t, e) ->

    ExceptionUtils.log(logger, GeneralException.create(
            ErrorSeverity.Warning,
            String.format("Got an unhandled exception caused thread (%s) terminated.", t.getName()),
            e));

    public static ExecutorService createPool(String poolName) {

        return Executors.newCachedThreadPool(new NamedThreadFactory(poolName, defaultUncaughtExceptionHandler));
    }

    public static ExecutorService createPool(String poolName, BiConsumer<Thread, Throwable> threadExceptionHandle) {

        return Executors.newCachedThreadPool(new NamedThreadFactory(poolName, threadExceptionHandle));
    }

    public static ExecutorService createFixedPool(String poolName, Integer threadCount) {

        if (threadCount == null)
            threadCount = Runtime.getRuntime().availableProcessors();

        if (threadCount == 1)
            return Executors.newSingleThreadExecutor(
                    new NamedThreadFactory(poolName, defaultUncaughtExceptionHandler));
        else
            return Executors.newFixedThreadPool(
                    threadCount,
                    new NamedThreadFactory(poolName, defaultUncaughtExceptionHandler));
    }

    public static ExecutorService createPoolBaseOnCpu(String poolName, double timesOfCpuCount) {

        return Executors.newFixedThreadPool(
                Double.valueOf(Math.ceil(Runtime.getRuntime().availableProcessors() * timesOfCpuCount)).intValue(),
                new NamedThreadFactory(poolName, defaultUncaughtExceptionHandler));
    }

    public static ExecutorService createPoolBaseOnCpu(
            String poolName,
            int threadCount,
            BiConsumer<Thread, Throwable> threadExceptionHandle) {

        return Executors.newFixedThreadPool(threadCount, new NamedThreadFactory(poolName, threadExceptionHandle));
    }

    public static boolean shutdown(ExecutorService execSvc, String msg, long timeout, TimeUnit unit, Logger logger) {

        try {
            execSvc.shutdown();
            if (!execSvc.awaitTermination(timeout, unit)) {
                execSvc.shutdownNow();
                GeneralException.create(ErrorSeverity.Error, msg, null).log(logger);
                logThreadsInfo();
                return false;
            }
            return true;
        } catch (InterruptedException ex) {
            execSvc.shutdownNow();
            logger.error("Thread has been interrupted.");
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public static void waitAll(List<Future<?>> futures) {

        if (futures == null || futures.isEmpty())
            return;

        futures.forEach(f -> {
            try {
                f.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                throw GeneralException.create(ErrorSeverity.Error, "Unhandled exception in the thread.", e);
            }
        });
    }

    public static void sleep(int ms) {

        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void logThreadsInfo() {

        Thread.getAllStackTraces().keySet().stream()
                .filter(t -> t.getState() != Thread.State.TERMINATED)
                .forEach(t -> logger.info(String.format(
                        "%sThread info: [%d - %s - %s] - %s: %s",
                        t.isDaemon() ? "Daemon " : "",
                        t.getId(),
                        t.getName(),
                        t.getThreadGroup().getName(),
                        t.getState(),
                        String.join(";", Arrays.stream(t.getStackTrace())
                                .map(s -> String.format(
                                        "%s.%s (%d)",
                                        s.getClassName(),
                                        s.getMethodName(),
                                        s.getLineNumber()))
                                .limit(30).toArray(String[]::new)))));
    }

    public static <T> CompletableFuture<T> failFuture(Throwable fault) {

        CompletableFuture<T> failFuture = new CompletableFuture<>();
        failFuture.completeExceptionally(fault);
        return failFuture;
    }

    public static CompletableFuture<Void> voidFuture() {

        return CompletableFuture.completedFuture(null);
    }

    public static void runAndForgot(Runnable action, boolean isDaemon, String threadName) {

        Thread thread = new Thread(action, threadName == null ? "RunAndForgot" : threadName);
        thread.setDaemon(isDaemon);
        thread.start();
    }

    static final class NamedThreadFactory implements ThreadFactory {

        // private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

        NamedThreadFactory(String prefix, BiConsumer<Thread, Throwable> threadExceptionHandle) {

            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = prefix +
                    "-thread-";
            uncaughtExceptionHandler = threadExceptionHandle::accept;
        }

        public Thread newThread(Runnable r) {

            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            t.setUncaughtExceptionHandler(this.uncaughtExceptionHandler);
            return t;
        }
    }
}