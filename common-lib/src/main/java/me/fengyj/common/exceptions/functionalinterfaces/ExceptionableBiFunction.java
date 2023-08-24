package me.fengyj.common.exceptions.functionalinterfaces;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

@FunctionalInterface
public interface ExceptionableBiFunction <T1, T2, R, E extends Exception> {

    /**
     * Applies this function to the given argument.
     *
     * @param arg1 the function 1st argument
     * @param arg2 the function 2nd argument
     * @return the function result
     */
    R apply(T1 arg1, T2 arg2) throws E;
}
