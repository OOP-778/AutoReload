package com.oop.autoreload.cmd;

import com.oop.orangeengine.command.OCommand;
import com.oop.orangeengine.command.arg.CommandArgument;
import com.oop.orangeengine.main.Engine;
import com.oop.orangeengine.main.util.data.pair.OPair;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class PluginsListArg extends CommandArgument<File> {
    public PluginsListArg() {
        setIdentity("plugin");
        setDescription("File of the plugin");
        setMapper(in -> {
            List<File> files = listPlugins();
            Optional<File> first = files.stream()
                    .filter(file -> file.getName().startsWith(in))
                    .findFirst();
            return new OPair<>(first.orElse(null), "Failed to find file by: " + in);
        });
    }

    @Override
    public void onAdd(OCommand command) {
        command.nextTabComplete((previous, args) -> listPlugins().stream().map(file -> file.getName().replace(".jar", "")).collect(Collectors.toList()));
    }

    private List<File> listPlugins() {
        File parentFile = Engine.getInstance().getOwning().getDataFolder().getParentFile();
        return Arrays.asList(Objects.requireNonNull(parentFile.listFiles(file -> file.getName().contains(".jar"))));
    }
}
