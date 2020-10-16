package com.oop.autoreload;

import com.oop.orangeengine.yaml.Config;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPluginLoader;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Handler {
    public static void handle(AutoReload autoReload, File file, HandlerType type) {
        Runnable rn = () -> {
            switch (type) {
                case PLUGIN_RELOAD:
                    try {
                        try {
                            unload(file);
                        } catch (Throwable throwable) {}
                        load(file);
                        autoReload.getOLogger().print("Reloaded {} using {} handler", file.getName().replace(".jar", ""), type.name().toLowerCase());
                    } catch (Throwable throwable) {
                        new IllegalStateException("Failed to handle " + file.getName(), throwable).printStackTrace();
                    }
                    break;
                case SERVER_RELOAD:
                    autoReload.getOLogger().print("Reloaded {} using {} handler", file.getName().replace(".jar", ""), type.name().toLowerCase());
                    Bukkit.reload();
                    break;

                case SERVER_RESTART:
                    autoReload.getOLogger().print("Reloaded {} using {} handler", file.getName().replace(".jar", ""), type.name().toLowerCase());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart");
                    break;
            }
        };

        Bukkit.getScheduler().runTask(autoReload, rn);
    }

    public static void load(File pluginFile) {
        Plugin target;

        try {
            target = Bukkit.getPluginManager().loadPlugin(pluginFile);
        } catch (Throwable e) {
            throw new IllegalStateException("Failed to load plugin by name " + pluginFile.getName().replace(".jar", ""), e);
        }

        target.onLoad();
        Bukkit.getPluginManager().enablePlugin(target);
    }

    public static void unload(File file) {
        String name = null;

        try {
            JarFile jarFile = new JarFile(file);
            final Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                final JarEntry entry = entries.nextElement();
                if (entry.getName().equalsIgnoreCase("plugin.yml")) {
                    JarEntry fileEntry = jarFile.getJarEntry(entry.getName());

                    InputStream input = jarFile.getInputStream(fileEntry);
                    Config pluginYml = new Config(new InputStreamReader(
                            input, StandardCharsets.UTF_8
                    ));
                    name = pluginYml.getAs("name");
                }
            }
        } catch (Throwable throwable) {
            throw new IllegalStateException("Failed to read name of the plugin of file: " + file.getName());
        }

        PluginManager pluginManager = Bukkit.getPluginManager();
        Plugin plugin = pluginManager.getPlugin(name);
        if (plugin == null) return;

        SimpleCommandMap commandMap = null;

        List<Plugin> plugins = null;

        Map<String, Plugin> names = null;
        Map<String, Command> commands = null;
        Map<Event, SortedSet<RegisteredListener>> listeners = null;

        boolean reloadlisteners = true;

        pluginManager.disablePlugin(plugin);

        try {

            Field pluginsField = Bukkit.getPluginManager().getClass().getDeclaredField("plugins");
            pluginsField.setAccessible(true);
            plugins = (List<Plugin>) pluginsField.get(pluginManager);

            Field lookupNamesField = Bukkit.getPluginManager().getClass().getDeclaredField("lookupNames");
            lookupNamesField.setAccessible(true);
            names = (Map<String, Plugin>) lookupNamesField.get(pluginManager);

            try {
                Field listenersField = Bukkit.getPluginManager().getClass().getDeclaredField("listeners");
                listenersField.setAccessible(true);
                listeners = (Map<Event, SortedSet<RegisteredListener>>) listenersField.get(pluginManager);
            } catch (Exception e) {
                reloadlisteners = false;
            }

            Field commandMapField = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            commandMap = (SimpleCommandMap) commandMapField.get(pluginManager);

            Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            commands = (Map<String, Command>) knownCommandsField.get(commandMap);

        } catch (Throwable e) {
            e.printStackTrace();
        }

        pluginManager.disablePlugin(plugin);

        if (plugins != null && plugins.contains(plugin))
            plugins.remove(plugin);

        if (names != null && names.containsKey(name))
            names.remove(name);

        if (listeners != null && reloadlisteners) {
            for (SortedSet<RegisteredListener> set : listeners.values()) {
                set.removeIf(value -> value.getPlugin() == plugin);
            }
        }

        if (commandMap != null) {
            for (Iterator<Map.Entry<String, Command>> it = commands.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, Command> entry = it.next();
                if (entry.getValue() instanceof PluginCommand) {
                    PluginCommand c = (PluginCommand) entry.getValue();
                    if (c.getPlugin() == plugin) {
                        c.unregister(commandMap);
                        it.remove();
                    }
                }
            }
        }

        // Attempt to close the classloader to unlock any handles on the plugin's jar file.
        ClassLoader cl = plugin.getClass().getClassLoader();

        if (cl instanceof URLClassLoader) {
            try {
                Field pluginField = cl.getClass().getDeclaredField("plugin");
                pluginField.setAccessible(true);
                pluginField.set(cl, null);

                Field pluginInitField = cl.getClass().getDeclaredField("pluginInit");
                pluginInitField.setAccessible(true);
                pluginInitField.set(cl, null);

            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                throw new IllegalStateException("Failed to unload Class Loader of " + plugin.getName(), ex);
            }

            try {
                ((URLClassLoader) cl).close();
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to unload Class Loader of " + plugin.getName(), ex);
            }
        }

        try {
            Field f = ClassLoader.class.getDeclaredField("classes");
            f.setAccessible(true);

            Vector<Class> classes = (Vector<Class>) f.get(cl);
            classes.clear();
        } catch (Exception exception) {}

        // Will not work on processes started with the -XX:+DisableExplicitGC flag, but lets try it anyway.
        // This tries to get around the issue where Windows refuses to unlock jar files that were previously loaded into the JVM.
        System.gc();
    }
}
