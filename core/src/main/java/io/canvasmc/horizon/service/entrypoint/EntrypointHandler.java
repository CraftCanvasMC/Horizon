package io.canvasmc.horizon.service.entrypoint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the interface target definition for the interface. Provides the method name, and method argument types for
 * the entrypoint target
 *
 * @author dueris
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EntrypointHandler {

    /**
     * The method name to invoke in the handler
     *
     * @return method name
     */
    String value();

    /**
     * The argument types for the method invocation
     *
     * @return the class arg types
     */
    Class<?>[] argTypes();
}
