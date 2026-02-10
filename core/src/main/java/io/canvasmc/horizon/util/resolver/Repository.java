package io.canvasmc.horizon.util.resolver;

import java.net.URL;

/**
 * The repository definition for the dependency resolver, containing the name and URL of the repo
 *
 * @param name
 *     the name
 * @param url
 *     the url
 *
 * @author dueris
 */
public record Repository(String name, URL url) {}
