package io.canvasmc.horizon.util;

/**
 * Essentially just a normal function, transforming {@link F} to {@link T}, allowing throwing exceptions that will be
 * caught by the code executing the function
 *
 * @param <F>
 *     from
 * @param <T>
 *     to
 *
 * @author dueris
 */
@FunctionalInterface
public interface ExceptionCatchingFunction<F, T> {
    /**
     * Applies the function
     *
     * @param from
     *     the before object
     *
     * @return the object after the function is applied
     *
     * @throws Throwable
     *     if an exception occurs
     */
    T transform(F from) throws Throwable;
}