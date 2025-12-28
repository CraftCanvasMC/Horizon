# Horizon

## Introduction
Horizon is a MIXIN wrapper for PaperMC servers and forks, expanding plugin capabilities to allow for further customization and enhancements. Horizon is a project that is intended to supersede
a project by one of the core team members(Dueris), the project Eclipse. Eclipse was a plugin for Paper that allowed loading Spongepowered Mixins and access wideners and transformers.
This project, of course, came with many issues and drawbacks that made Eclipse hard to work with most of the time. And so, Dueris achrived the project and decided to create Horizon,
which is the successor of Eclipse.

Horizon intends to fix the issues from Eclipse and create a more manageable, workable, and stable environment for plugins to work with, while incorporating plugin authors' ideas in a much more powerful and
flexible manner.

Horizon acts more as a replacement wrapper for Paperclip(the launcher for Paper servers and forks). It boots the game in a very similar way, as it contains an iteration of the Paperclip launcher.
More details about this are outlined in the [Launch Process](#launch-process) section. Please read the sections below to learn more about how Horizon works and how to develop with it.

## Breakages and Incompatibilities
Horizon tries not to break much of anything; however, there are some things it's incompatible with.
- **The legacy plugin loader.** The legacy plugin loader(`LegacyPluginLoadingStrategy.java`) is completely unsupported in Horizon. This is due to Horizon having a few internal
  mixin injections to the `ModernPluginLoadingStrategy.java` file, and just supporting the legacy format is not in the scope of development ATM, as people should be using the
  modern plugin loading strategy. To ensure you are using the modern strategy, please ensure your server does not contain the startup flag `-Dpaper.useLegacyPluginLoading=true`.
- **UniverseSpigot.** Due to how Universe's loader is set up, Horizon is fundamentally incompatible with Universe. Do not ask CanvasMC or Universe for support; it will not work and is not planned to work.
- **Spigot and Bukkit.** Horizon strictly works only for Paper servers and forks, and is untested on Spigot and Bukkit, and you will not receive support for using Spigot or Bukkit with Horizon

## How To
### Installation and Running
Horizon is simple to install and get running. You can download Horizon from our website, https://canvasmc.io, and from there it is as simple as dropping the downloaded JAR file
into the *same directory* as your server JAR. **DO NOT REPLACE THE SERVER JAR!!** Horizon works as an external wrapper for the server JAR, so the server JAR needs to be present for Horizon
to function correctly. The default file Horizon will search for is `server.jar`, which is configurable wth the `horizon.yml` configuration file:

```yaml
pluginsDirectory: plugins
serverJar: server.jar
cacheLocation: cache/horizon
extraPlugins: []
serverName: horizon
```

This `horizon.yml` file is intended for more extensive configuration of Horizon, allowing setting the server JAR name, since not all servers will have their server JAR named `server.jar`, or you can then
have multiple server JARs and swap between the target Horizons they use.
- The `cacheLocation` is simply for storing JIJ plugins and such, and is cleared on each boot of Horizon. We don't recommend changing it, but
you can if there are conflicts or some issue arises, and you need to change the location.
- The option `extraPlugins` allows for adding additional plugins to the Horizon classpath to be loaded. Horizon also reads from the `--add-plugin` JVM argument that is passed to the server
- The `serverName` option is an optional override for the server mod name, as it gets overridden in Horizon automatically by its internal mixin inject
- The `pluginsDirectory` option should always point to your plugins directory for both Paper plugins and Horizon plugins; however, you can separate them if you need or want to.

Once all options are configured to your liking, you can boot the Horizon JAR as usual, and your server will run with Horizon as its bootstrapper!

> [!NOTE]
> You can disable internal mixin injections by using flags like `-DHorizon.disable.mixin.{mixin package location}`

### Basics of Developing a Horizon Plugin
Developing with Horizon is *generally* simple. One tool you can use is the Gradle plugin, which is described in more detail below. To start with, developing a Horizon plugin,
you need to add a few more things to your plugin YML. You need something like this:
```yaml
horizon:
  mixins:
    - "mixins.test.json"
  wideners:
    - "widener.at"
  load-datapack-entry: true
```
Horizon reads from the `plugin.yml` or `paper-plugin.yml` from the plugin JAR entries to build the `ObjectTree` representing your plugin configuration.
Each option in the `horizon` field is optional; the only required field is `horizon` for your plugin to be marked and loaded by Horizon.
- `mixins` - This is a `String[]` option that defines the SpongePowered mixin configuration files in your plugin artifact. Like if it were `test.mixins.json`, the entry should be in the root of your resources, named `test.mixins.json`
- `wideners` - This is the same as the `mixins` field, but defines Forge access transformers for your plugin. The team initially wanted to use Fabric wideners, but to keep consistency with Paper's
  build system and paperweight, we decided it would be best to use transformers instead. You can find detailed documentation regarding Forge access transformers [here](#mixins-and-ats)
- `load-datapack-entry` - This is just a `boolean` option, `false` by default, that defines if your plugin should be loaded as a datapack entry too, similar to how the Fabric loader loads mods as datapacks too. While somewhat useless in more modern versions
  of the game due to Papers lifecycle API, this introduces a more direct way to load your plugin as a datapack, and also supports the `/minecraft:reload` command to reload your plugin datapack assets

> [!NOTE]
> More information on Mixins and ATs is available in the [Mixins && ATs](#mixins-and-ats) section below.

To check if your plugin is loaded as a Horizon plugin successfully, you can read the plugin data tree in the startup logs, which will look similar to this:
```terminaloutput
[23:23:34] [INFO]: Found 2 plugin(s):
        - horizon local
        - testplugin 1.0.0-SNAPSHOT
```

Another way is by checking the `/plugins` command, which is replaced with the Horizon internal mixins to include Horizon plugins.

![plugin command output](assets/plugin_command_out.png)

You can learn about how to use our Gradle plugin to assist in Horizon plugin development [below](#gradle-plugin), along with [JIJ](#jijjar-in-jar). It is also recommended to familiarize yourself with the new classloader hierarchy in the [New Classloading Tree](#new-classloading-tree) section

### JIJ(JAR In JAR)
Another capability Horizon plugins have is JIJ(JAR In JAR). JIJ is a feature that allows Horizon plugins to attach Horizon plugins, Paper plugins, or external libraries.
All JIJ plugins will be loaded from the `horizon.yml:cacheLocation` configured location, which is fetchable via Horizons API, which is documented [below](#horizon-plugin-api).
All Paper plugins will be loaded as usual, and their plugin data folders will remain in the same place. Horizon plugins will function exactly the same, and will load
mixins like normal. External libraries will be appended to the game classpath with Ember, as if they were libraries added by the server JAR, and will be accessible after Horizon launches the game.

### Gradle Plugin
In order to start developing plugins for Horizon, it is required that you use the `horizon` gradle plugin in your build scripts together with the `weaver-userdev` plugin.
The `horizon` plugin automatically applies your ATs to the server JAR your plugin is going to be developed against, allowing you to compile against it and access the server's internals,
and the `userdev` plugin allows it to achieve all that.

Below is shown an example `build.gradle.kts` configuration structure to give you an idea on how to start developing!

```kotlin
plugins {
    id("io.canvasmc.weaver.userdev") version "xxx"
    id("io.canvasmc.horizon") version "xxx"
}

dependencies {
    paperDevBundle("1.21.11-R0.1-SNAPSHOT")
}

horizon {
    accessTransformerFiles.from(
        file("build-data/first.at"),
        file("build-data/second.at")
    )
}
```

In addition, using the `shadow` gradle plugin is *unsupported* and you should instead opt-in to JiJ'ing your dependencies by using the appropriate configurations, just like this:
```kotlin
dependencies {
    implementation("io.canvasmc:horizon:1.0.0") // <- required for accesing the Horizon API in dev.
    includeMixinPlugin("io.canvasmc:nice-horizon-plugin:1.0.0")
    includePlugin("io.canvasmc:nice-plugin:1.0.0")
    includeLibrary("io.canvasmc:nice-library:1.0.0")
}
```
The above configurations define in what directory the dependency is going to be placed in the JAR file structure, their names are pretty intuitive in themselves,
however each one is described in detail below anyway.

For a Horizon-based plugin dependency, the appropriate configuration is `includeMixinPlugin`, which puts it under `META-INF/jars/horizon`.

For a normal plugin dependency, aka not-horizon, you should use the `includePlugin` configuration, which places it under `META-INF/jars/plugin`.

And finally, for a library, the configuration to use is `includeLibrary`, which places it under `META-INF/jars/libs`.

All of those dependencies will be loaded at the server startup, for a more detailed overlook refer to the [JIJ](#jijjar-in-jar) section.
## New Classloading Tree
For obvious reasons, the classloading hierarchy tree has changed. A diagram is shown below with a general visual example of what this looks like

![classloading tree](assets/ember.png)

Paper (and forks) is similar to this tree; however, Ember is replaced with a normal `URLClassLoader`. The `DYNAMIC` classloader shown in the diagram
is a `URLClassLoader` instance that allows modification of the URLs added to it post-init. Horizon plugins, instead of being loaded with every other
plugin in their own classloader, are linked to the Ember classloader. This **does** mean that Horizon plugins **cannot** invoke code in non-Horizon plugins.

One way to get around this is by using an abstraction layer and a 2nd plugin. You could have a Paper plugin and a Horizon plugin loaded(using JIJ too for more compact file managing)
and create interfaces in the Horizon plugin that the Paper plugin implements, of which the Paper plugin will then access code in other non-Horizon plugin JARs.
By implementing a system like this, you could create a Horizon plugin that can communicate with other Paper plugins. Do note, though, that all Horizon plugins are visible to the Paper
plugin classloaders.

## Launch Process

## ObjectTree API

## Mixins and ATs

## Horizon Plugin API

## Conclusion
