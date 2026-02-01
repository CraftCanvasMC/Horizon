![logo](assets/horizon_logo.png)

---

## Introduction

Horizon is a MIXIN wrapper for PaperMC servers and forks, expanding plugin capabilities to allow for further
customization and enhancements.

Horizon acts more like a replacement wrapper for Paperclip (the launcher for Paper servers and forks). It boots the game in
a very similar way, as it contains an iteration of the Paperclip launcher. Please read the sections below to
learn more about how Horizon works and how to develop with it.

Horizon supports all versions of Minecraft newer than `1.20.6`, and also supports snapshots, pre-releases, and release-candidates.

- For installing and running, see [here](#Installation-and-Running).
- For development, start [here](#Basics-of-Developing-a-Horizon-Plugin)

## Background
Horizon is a project that is intended to supersede
a project by one of the core team members(Dueris), the project Eclipse. Eclipse was a plugin for Paper based
on the program Ignite that allowed loading SpongePowered Mixins, access wideners, and transformers. Eclipse
and Ignite code are contained within Horizon since obviously, Horizon supersedes Eclipse, and as such has a
similar concept and similar/derived code, and Eclipse is based on/forked from Ignite directly.
This project, of course, had many issues and drawbacks that made Eclipse difficult to work with most of the time. And
so, Dueris archived the project and decided to create Horizon, which is the successor of Eclipse.

Horizon intends to fix the issues from Eclipse and create a more manageable, workable, and stable environment for
plugins to work with, while incorporating plugin authors' ideas in a much more powerful and flexible manner.

## Breakages and Incompatibilities

Horizon tries not to break much; however, there are some things it's incompatible with.

- **The legacy plugin loader.** The legacy plugin loader(`LegacyPluginLoadingStrategy.java`) is completely unsupported
  in Horizon. This is due to Horizon having a few internal
  mixin injections to the `ModernPluginLoadingStrategy.java` file, and just supporting the legacy format is not in the
  scope of development ATM, as people should be using the
  modern plugin loading strategy. To ensure you are using the modern strategy, please ensure your server does not
  contain the startup flag `-Dpaper.useLegacyPluginLoading=true`.
- **UniverseSpigot.** Due to how Universe's loader is set up, Horizon is fundamentally incompatible with Universe. Do
  not ask CanvasMC or Universe for support; it will not work and is not planned to work.
- **Spigot and Bukkit.** Horizon strictly works only for Paper servers and forks, and is untested on Spigot and Bukkit,
  and you will not receive support for using Spigot or Bukkit with Horizon
- **Ignite and Eclipse.** Eclipse is a fork of Ignite, and Horizon derives some source from Eclipse since this project
  supersedes Eclipse. As such, they are both completely and fundamentally incompatible with Horizon. Horizon, in
  comparison
  to Ignite, is generally more inclined and intended for expanding upon Ignite's initial structure and idea, aimed at
  specifically, plugin usage to match closer to things like Paper's patching style and such (transformers, for example).
  This also uses things like the plugin YAML and such, and is compatible with using the `main` entrypoint in plugins,
  unlike Ignite. Horizon is intended to expand plugin capabilities further for Paper, while Ignite is meant for a more
  universal MIXIN launcher for Java servers.

## How To

### Installation and Running

Horizon is simple to install and get running. You can download Horizon from our website, https://canvasmc.io, and from
there it is as simple as dropping the downloaded JAR file
into the *same directory* as your server JAR. **DO NOT REPLACE THE SERVER JAR!!** Horizon works as an external wrapper
for the server JAR, so the server JAR needs to be present for Horizon
to function correctly. The default file Horizon will search for is `server.jar`, which is configurable wth the
`horizon.yml` configuration file:

```yaml
pluginsDirectory: plugins
serverJar: server.jar
cacheLocation: cache/horizon
extraPlugins: [ ]
serverName: horizon
```

This `horizon.yml` file is intended for more extensive configuration of Horizon, allowing setting the server JAR name,
since not all servers will have their server JAR named `server.jar`, or you can then
have multiple server JARs and swap between the target Horizons they use.

- The `cacheLocation` is simply for storing JIJ plugins and such, and is cleared on each boot of Horizon. We don't
  recommend changing it, but you can if there are conflicts or some issue
  arises, and you need to change the location.
- The option `extraPlugins` allows for adding additional plugins to the Horizon classpath to be loaded. Horizon also
  reads from the `--add-plugin` JVM argument that is passed to the server
- The `serverName` option is an optional override for the server mod name, as it gets overridden in Horizon
  automatically by its internal mixin inject
- The `pluginsDirectory` option should always point to your plugins directory for both Paper plugins and Horizon
  plugins; however, you can separate them if you need or want to.

Once all options are configured to your liking, you can boot the Horizon JAR as usual, and your server will run with
Horizon as its bootstrapper!

### Plugin Development

All Horizon documentation for plugin development can be found in [here](https://docs.canvasmc.io)