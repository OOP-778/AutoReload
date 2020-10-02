package com.oop.autoreload;

import com.oop.orangeengine.file.OFile;
import com.oop.orangeengine.main.plugin.EnginePlugin;
import com.oop.orangeengine.main.task.ClassicTaskController;
import com.oop.orangeengine.main.task.TaskController;
import com.oop.orangeengine.yaml.Config;

public class AutoReload extends EnginePlugin {

    public HandlerType type;

    @Override
    public void enable() {
        if (!getDataFolder().exists())
            getDataFolder().mkdirs();

        getOLogger()
                .setMainColor("&a");
        getOLogger()
                .setSecondaryColor("&2");

        OFile configFile = new OFile(getDataFolder(), "config.yml").createIfNotExists(true);
        Config config = new Config(configFile);

        long updateCheckTimer = config.getAs("file check timer", Long.class);
        type = HandlerType.match(config.getAs("handler type"));

        new Checker(this).delay(updateCheckTimer);
        getOLogger().print("Started checker");
    }

    @Override
    public TaskController provideTaskController() {
        return new ClassicTaskController(this);
    }
}
