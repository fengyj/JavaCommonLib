package me.fengyj.common.exceptions.functionalinterfaces;

@FunctionalInterface
public interface ExceptionableRunnable<E extends Exception> {
    void run() throws E;
}
