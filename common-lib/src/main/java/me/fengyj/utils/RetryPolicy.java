package me.fengyj.springdemo.utils;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.fengyj.springdemo.exception.RetrievableException;

public class RetryPolicy {

    private static final Logger logger = LoggerFactory.getLogger(RetryPolicy.class);

    private final int maxRetryTimes;
    private final int retryInterval;
    private final Set<AdditionIntervalPolicy> intervalPolicies;
    private final Random random;

    /**
     * define a retry policy
     *
     * @param maxRetryTimes    max retry times
     * @param retryInterval    interval in seconds
     * @param intervalPolicies if it's null, will use fixed interval to retry.
     */
    public RetryPolicy(int maxRetryTimes, double retryInterval, AdditionIntervalPolicy... intervalPolicies) {

        this.maxRetryTimes = Math.max(maxRetryTimes, 1);
        this.retryInterval = retryInterval < 0 ? 0 : (int) (retryInterval * 1000);
        if (intervalPolicies == null || intervalPolicies.length == 0)
            this.intervalPolicies = Collections.emptySet();
        else
            this.intervalPolicies = EnumSet.of(intervalPolicies[0], intervalPolicies);
        if (this.intervalPolicies.contains(AdditionIntervalPolicy.Jitter))
            this.random = new Random();
        else
            this.random = null;
    }

    public <T> T execute(
            RetriableSupplier<T> supplier,
            Consumer<RetrievableException> actionWhenCatch,
            Runnable actionWhenFinally) {

        RetrievableException lastEx = null;
        int retry = 0;
        while (retry++ < this.maxRetryTimes) {
            try {
                return supplier.get();
            } catch (RetrievableException ex) {
                ex.setTriedTimes(retry);
                lastEx = ex;
                if (actionWhenCatch != null)
                    actionWhenCatch.accept(ex);
                if (retry == this.maxRetryTimes) {
                    logger.debug("Tried over the max times and failed.", ex);
                    throw ex;
                } else if (this.retryInterval > 0) {
                    ThreadUtils.sleep(getInterval(retry));
                }
            } finally {
                if (actionWhenFinally != null)
                    actionWhenFinally.run();
            }
        }
        throw lastEx;
    }

    private <T> CompletableFuture<T> executeAsync(
            Supplier<CompletableFuture<T>> supplier,
            Consumer<Throwable> exceptionHandle,
            int triedTimes) {

        final int times = triedTimes + 1;

        CompletableFuture<T> future = supplier.get();

        return future.thenApply(CompletableFuture::completedFuture)
                .exceptionally(ex -> {
                    Throwable fail = ex instanceof CompletionException ? ex.getCause() : ex;
                    if (times < this.maxRetryTimes) {
                        try {
                            exceptionHandle.accept(ex);
                        } catch (RetrievableException e) {
                            if (this.retryInterval > 0) {

                                ThreadUtils.sleep(getInterval(times));
                                return executeAsync(
                                        supplier,
                                        exceptionHandle,
                                        times);
                            }
                        } catch (Throwable e) {
                            fail = e;
                        }
                    } else if (fail instanceof RetrievableException) {
                        logger.debug("Tried over the max times and failed.", fail);
                    }
                    CompletableFuture<T> failFuture = new CompletableFuture<>();
                    failFuture.completeExceptionally(fail);
                    return failFuture;
                }).thenCompose(Function.identity());
    }

    public <T> CompletableFuture<T> executeAsync(
            Supplier<CompletableFuture<T>> supplier,
            Consumer<Throwable> exceptionHandle) {

        return executeAsync(supplier, exceptionHandle, 0);
    }

    public void execute(
            RetriableRunnable action,
            Consumer<RetrievableException> actionWhenCatch,
            Runnable actionWhenFinally) {

        int retry = 0;
        RetrievableException lastEx = null;
        while (retry++ < this.maxRetryTimes) {
            try {
                action.run();
                return;
            } catch (RetrievableException ex) {
                lastEx = ex;
                if (actionWhenCatch != null)
                    actionWhenCatch.accept(ex);
                if (retry >= this.maxRetryTimes) {

                    LogUtils.logDebug(logger, String.format(
                            "Tried %d times because of %s.",
                            this.maxRetryTimes,
                            ExceptionUtils.toString(ex.getCause() == null ? ex : ex.getCause())));
                    throw ex;
                } else if (this.retryInterval > 0) {

                    ThreadUtils.sleep(getInterval(retry));
                }
            } finally {
                if (actionWhenFinally != null)
                    actionWhenFinally.run();
            }
        }
        throw lastEx;
    }

    private int getInterval(int times) {

        int interval = this.retryInterval;

        if (this.intervalPolicies.contains(AdditionIntervalPolicy.Increasing)) {
            interval = (int) (interval * Math.pow(times, 1.5));

            if (this.intervalPolicies.contains(AdditionIntervalPolicy.Jitter)) {
                int jitter = (int) (this.retryInterval * Math.pow(times, 1.3));
                jitter = jitter / 2 - random.nextInt(jitter);
                interval = interval + jitter;
            }
        } else {

            if (this.intervalPolicies.contains(AdditionIntervalPolicy.Jitter)) {
                interval = interval + (int) (this.retryInterval * 0.4)
                        - random.nextInt((int) (this.retryInterval * 0.8));
            }
        }

        return interval;
    }

    public enum AdditionIntervalPolicy {

        /**
         * if the initial interval is 1 second, and 5 times to retry.
         * times interval total
         * 0 0 0
         * 1 1.2 1.2
         * 2 2.8 4.0
         * 3 5.2 9.2
         * 4 8.0 17.2
         * <p>
         * if the initial interval is 2 seconds
         * times interval total
         * 0 0 0
         * 1 2.1 2.1
         * 2 5.6 7.7
         * 3 10.4 18.1
         * 4 16.0 34.1
         */
        Increasing,
        /**
         * the jitter formula is 0.4 * initial interval.
         * the jitter formula is 0.4 * initial interval.
         * if plus Increasing mode, the jitter formula is:
         * jitter = initial interval pow(retryTime, 1.3) / 2 - random(initial interval
         * pow(retryTime, 1.3))
         */
        Jitter
    }

    @FunctionalInterface
    public interface RetriableRunnable {

        void run() throws RetrievableException;
    }

    @FunctionalInterface
    public interface RetriableSupplier<T> {

        T get() throws RetrievableException;
    }
}
