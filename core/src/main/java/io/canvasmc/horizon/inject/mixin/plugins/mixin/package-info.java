// paper does a remap via ClassloaderBytecodeModifier.bytecodeModifier()#modify, we are going to do a very
// similar approach to this in how we go about this, by running it through our own transformer

// the ember classloaders implementation for 'Class<?> findClass(String name)'
// - internal findClass(String name, TransformPhase phase)
//   - internal transformData(name, phase), which essentially creates nullable transformed ClassData
//     which is transformed via the class transformer and its services
//   - if the returned ClassData is null, it returns null, which then throws a class not found error
//   - the important part of this method though is it executes transformBytes(), which is the leading
//     transformation service for the byte array, we will do the same thing here
package io.canvasmc.horizon.inject.mixin.plugins.mixin;