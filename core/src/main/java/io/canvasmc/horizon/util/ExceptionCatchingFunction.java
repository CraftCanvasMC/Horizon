package io.canvasmc.horizon.util;

@FunctionalInterface
public interface ExceptionCatchingFunction<F, T> {
    T transform(F from) throws Throwable;
}