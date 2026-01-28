package io.canvasmc.horizon.inject.mixin.plugins.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.service.EmberClassLoader;
import io.canvasmc.horizon.service.transform.TransformPhase;
import io.papermc.paper.plugin.entrypoint.classloader.PaperSimplePluginClassLoader;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.security.CodeSource;

@Mixin(PaperSimplePluginClassLoader.class)
public class PaperSimplePluginClassLoaderMixin {

    @WrapOperation(method = "findClass", at = @At(value = "INVOKE", target = "Lio/papermc/paper/plugin/entrypoint/classloader/PaperSimplePluginClassLoader;defineClass(Ljava/lang/String;[BIILjava/security/CodeSource;)Ljava/lang/Class;"))
    public Class<?> horizon$transformClassBytes(final PaperSimplePluginClassLoader instance, final String name, final byte[] originalClassBytes, final int off, final int classBytesLengthOld, final CodeSource codeSource, final @NonNull Operation<Class<?>> original) {
        // we cannot use classBytesLengthOld, as if we transform this, this becomes inaccurate
        HorizonLoader horizon = HorizonLoader.getInstance();
        EmberClassLoader ember = horizon.getLaunchService().getClassLoader();

        // Note: we use TransformPhase.INITIALIZE
        final byte[] transformed = ember.transformer.transformBytes(name, originalClassBytes, TransformPhase.INITIALIZE);

        // call the original function again with the swapped variables
        return original.call(instance, name, transformed, off, transformed.length, codeSource);
    }
}
