# Horizon

## Introduction
Horizon is a MIXIN wrapper for PaperMC servers and forks, expanding plugin capabilities to allow for further customization and enhancements. Horizon is a project that is intended to supersede
a project by one of the core team members(Dueris), the project Eclipse. Eclipse was a plugin for Paper that allowed loading Spongepowered Mixins and access wideners and transformers.
This project of course came with lots of issues and many drawbacks that made Eclipse hard to work with most of the time. And so, Dueris achrived the project and decided to create Horizon,
which is the successor of Eclipse.

Horizon intends to fix the issues from Eclipse and create a more manageable, workable, and stable environment for plugins to work with, while incorporating plugin authors ideas in a much more powerful and
flexible manner.

Horizon acts more as a replacement wrapper for Paperclip(the launcher for Paper servers and forks). It boots the game in a very similar way, as it contains an iteration of the Paperclip launcher.
More details about this are outlined in the [Launch Process](#launch-process) section. Read the sections below to learn more about how Horizon works, and how to develop with it.

## Breakages and Incompatibilities
Horizon tries to not break much of anything, however there are some things its incompatible with.
- **The legacy plugin loader.** The legacy plugin loader(`LegacyPluginLoadingStrategy.java`) is completely unsupported in Horizon. This is due to Horizon having a few internal
  mixin injections to the `ModernPluginLoadingStrategy.java` file, and just supporting the legacy format is not in the scope of development ATM, as people should be using the
  modern plugin loading strategy. To ensure you are using the modern strategy, please ensure your server does not contain the startup flag `-Dpaper.useLegacyPluginLoading=true`.
- **UniverseSpigot.** Due to how Universes loader is set up, Horizon is fundamentally incompatible with Universe. Do not ask CanvasMC or Universe for support, it will not work and is not planned to work.
- **Spigot and Bukkit.** Horizon strictly works only for Paper servers and forks, and is untested on Spigot and Bukkit, and you will not receive support for using Spigot or Bukkit with Horizon

## How To
### Installation and Running
Horizon is fairly simple to install and get running. You can download Horizon from our website, https://canvasmc.io, and from there it is as simple as dropping the downloaded jar file
into the *same directory* as your server jar. **DO NOT REPLACE THE SERVER JAR!!** Horizon works as an external wrapper for the server jar, so the server jar is needed to be present for Horizon
to function correctly. The default file Horizon will search for is `server.jar`, which is configurable wth the `horizon.yml` configuration file:

```yaml
pluginsDirectory: plugins
serverJar: server.jar
cacheLocation: cache/horizon
extraPlugins: []
serverName: horizon
```

This `horizon.yml` file is intended for more extensive configuration of Horizon, allowing setting the server jar name, since not all servers will have their server jar named `server.jar`, or you can then
have multiple server jars, and swap between the target Horizon uses.
- The `cacheLocation` is simply for storing JIJ plugins and such, and is cleared on each boot of Horizon. We don't recommend changing it, but
you can if there are conflicts or some issue arises and you need to change the location.
- The option `extraPlugins` allows for adding additional plugins to the Horizon classpath to be loaded. Horizon dose also read from the `--add-plugin` argument that is passed to the server
- The `serverName` option is an optional override for the server mod name, as it gets overridden in Horizon automatically by its internal mixin inject
- The `pluginsDirectory` option should always point to your plugins directory for both Paper plugins and Horizon plugins, however you can separate them if you need or want to.

Once all options are configured to your liking, you can simply boot the Horizon jar like normal and your server will function with Horizon as its bootstrapper!

> [!NOTE]
> You can disable internal mixin injections by using flags like `-DHorizon.disable.mixin.{mixin package location}`

### Basics of Developing a Horizon Plugin
Developing with Horizon is *generally* simple. One tool you can use is the gradle plugin, which is described more in detail below. To start with developing a Horizon plugin,
you need to add a few more things to your plugin yml. You need something like this:
```yaml
horizon:
  mixins:
    - "mixins.test.json"
  wideners:
    - "widener.at"
  load-datapack-entry: true
```
Horizon reads from the `plugin.yml` or `paper-plugin.yml` from the plugin jar entries to build the `ObjectTree` representing your plugin configuration.
Each option in the `horizon` field is completely optional, the only thing required is the `horizon` field for your plugin to be marked and loaded by Horizon.
- `mixins` - This is a `String[]` option just defines the spongepowered mixin configuration files in your plugin artifact. Like if it were `test.mixins.json`, the entry should be in the root of your resources, named `test.mixins.json`
- `wideners` - This is the same as the `mixins` field, but defines Forge access transformers for your plugin. The team originally wanted to use Fabric wideners, but to keep consistency with Papers
  build system and paperweight, we decided it would be best to use transformers instead. You can find detailed documentation regarding Forge access transformers [here](#mixins-and-ats)
- `load-datapack-entry` - This is just a `boolean` option, `false` by default, that defines if your plugin should be loaded as a datapack entry too, similar to how the Fabric loader loads mods as datapacks too. While somewhat useless in more modern versions
  of the game due to Papers lifecycle API, this introduces a more direct way to load your plugin as a datapack, and also supports the `/minecraft:reload` command to reload your plugin datapack assets

> [!NOTE]
> More information on Mixins and ATs is below in the [Mixins && ATs](#mixins-and-ats) section.

To check if your plugin is loaded as a Horizon plugin successfully, you can read the plugin data tree in the startup logs, which will look similar to this:
```terminaloutput
[23:23:34] [INFO]: Found 2 plugin(s):
        - horizon local
        - testplugin 1.0.0-SNAPSHOT
```

Another way is by checking the `/plugins` command, which is replaced with the Horizon internal mixins to include Horizon plugins

![plugin command output](assets/plugin_command_out.png)

### JIJ(Jar In jar)
Another capability Horizon plugins have is JIJ(Jar In Jar). JIJ is a feature that allows Horizon plugins to attach Horizon plugins, Paper plugins, or external libraries.
All JIJ plugins will be loaded to the `horizon.yml:cacheLocation` configured location, which is fetchable via Horizons API, which is documented [below](#horizon-plugin-api).
All Paper plugins will be loaded as normal, and their plugin data folders will still remain in the same place as normal. Horizon plugins will function the exact same, and will load
mixins like normal. External libraries will just be appended to the game classpath with Ember like if it were a library added by the server jar, which is accessible after Horizon launches the game.

You can learn about how to use our gradle plugin to assist in Horizon plugin development below. It is also recommended to familiarize yourself with the new classloader hierarchy in the [New Classloading Tree](#new-classloading-tree) section

### Gradle Plugin
-- note: gradle plugin, need toffik for docs on that
-- note: mention JIJ

## New Classloading Tree
For obvious reasons, the classloading hierarchy tree has changed. A diagram is shown below with a general visual example of what this lookslike

![classloading tree](assets/ember.png)

Paper(and forks) is similar to this tree, however Ember is replaced with a normal `URLClassLoader`. The `DYNAMIC` classloader shown in the diagram
is a `URLClassLoader` instance that allows modification of the URLs added to it post-init. Horizon plugins, instead of being loaded with every other
plugin in their own classloader, is linked to the Ember classloader. This **does** mean that Horizon plugins **cannot** invoke code in non-Horizon plugins.

One way to get around this is by using an abstraction layer, and a 2nd plugin. You could have a Paper plugin and a Horizon plugin loaded(using JIJ too for more compact file managing)
and create interfaces in the Horizon plugin that the Paper plugin implements, of which the Paper plugin will then access code in other non-Horizon plugin jars.
By doing a system like this, you could implement a Horizon plugin being able to speak to other Paper plugins. Do note though, all Horizon plugins are visible to the Paper
plugin classloaders.

## Launch Process

## ObjectTree API

## Mixins and ATs

## Horizon Plugin API

## Conclusion