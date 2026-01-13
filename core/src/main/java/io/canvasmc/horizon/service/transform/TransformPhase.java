package io.canvasmc.horizon.service.transform;

/**
 * Defines the lifecycle transformation phase of a class transformation.
 * <p>
 * Transformation phases allow transformation logic to be applied at
 * specific points during class loading and modification
 */
public enum TransformPhase {

    /**
     * The transformation is applied during initial class loading
     */
    INITIALIZE,

    /**
     * The transformation phase for when MIXIN is being applied
     */
    MIXIN
}
