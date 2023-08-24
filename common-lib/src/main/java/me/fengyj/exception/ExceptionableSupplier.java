package me.fengyj.exception;

@FunctionalInterface
public interface ExceptionableSupplier<T, E extends Exception> {

    /**
     * Gets a result.
     *
     * @return a result
     */
    T get() throws E;
}
