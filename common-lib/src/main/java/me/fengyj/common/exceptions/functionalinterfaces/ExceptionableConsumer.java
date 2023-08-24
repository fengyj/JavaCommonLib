package me.fengyj.common.exceptions.functionalinterfaces;

import java.util.Objects;
import java.util.function.Consumer;

@FunctionalInterface
public interface ExceptionableConsumer<T, E extends Exception> {
    void accept(T arg) throws E;

    default ExceptionableConsumer<T, E> andThen(ExceptionableConsumer<? super T, E> after) throws E {
        Objects.requireNonNull(after);
        return (T t) -> { accept(t); after.accept(t); };
    }

    default ExceptionableConsumer<T, E> andThen(Consumer<? super T> after) throws E {
        Objects.requireNonNull(after);
        return (T t) -> { accept(t); after.accept(t); };
    }
}
