package io.canvasmc.horizon.util;

import java.io.File;
import java.util.jar.JarFile;

/**
 * Represents a pair of a IO file and a jar file
 *
 * @param ioFile  the io file
 * @param jarFile the jar file
 */
public record FileJar(File ioFile, JarFile jarFile) {
}
