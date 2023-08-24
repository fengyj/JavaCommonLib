package me.fengyj.common.exceptions.functionalinterfaces;

import java.util.Objects;
import java.util.function.BiConsumer;

@FunctionalInterface
public interface ExceptionableBiConsumer <T1, T2, E extends Exception> {
    void accept(T1 arg1, T2 arg2) throws E;

    default ExceptionableBiConsumer<T1, T2, E> andThen(ExceptionableBiConsumer<? super T1, T2, E> after) throws E {
        Objects.requireNonNull(after);
        return (T1 arg1, T2 arg2) -> { accept(arg1, arg2); after.accept(arg1, arg2); };
    }

    default ExceptionableBiConsumer<T1, T2, E> andThen(BiConsumer<? super T1, ? super T2> after) throws E {
        Objects.requireNonNull(after);
        return (T1 arg1, T2 arg2) -> { accept(arg1, arg2); after.accept(arg1, arg2); };
    }
}
