package com.oop.autoreload;

import com.oop.orangeengine.main.task.OTask;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.oop.orangeengine.main.Engine.getEngine;

public class Checker extends OTask {
    private Map<String, Long> cache = new ConcurrentHashMap<>();
    private boolean firstRun = true;

    public Checker(AutoReload plugin) {
        File pluginsDir = plugin.getDataFolder().getParentFile();
        sync(false);
        delay(1000);
        repeat(true);
        runnable(() -> {
            if (plugin.isDisabling()) return;
            for (File file : Objects.requireNonNull(pluginsDir.listFiles(in -> in.getName().endsWith(".jar")))) {
                // Get last modified time from the cache
                Long lastModified = cache.get(file.getName());
                if (lastModified == null) {
                    if (firstRun) {
                        cache.put(file.getName(), file.lastModified());
                        continue;
                    }

                    cache.put(file.getName(), file.lastModified());
                    Handler.handle(plugin, file, plugin.type);
                    continue;
                }

                if (lastModified != file.lastModified()) {
                    cache.remove(file.getName());
                    cache.put(file.getName(), file.lastModified());

                    Handler.handle(plugin, file, plugin.type);
                }
            }

            if (firstRun)
                firstRun = false;
        });
        execute();

    }
}
