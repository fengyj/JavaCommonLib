package me.fengyj.common.exceptions.functionalinterfaces;

@FunctionalInterface
public interface ExceptionableSupplier<T, E extends Exception> {

    /**
     * Gets a result.
     *
     * @return a result
     */
    T get() throws E;
}
