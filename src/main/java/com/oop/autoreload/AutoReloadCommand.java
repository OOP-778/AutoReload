package com.oop.autoreload;

import com.oop.autoreload.cmd.PluginsListArg;
import com.oop.orangeengine.command.OCommand;
import com.oop.orangeengine.main.Engine;
import com.oop.orangeengine.main.Helper;

import java.io.File;

public class AutoReloadCommand extends OCommand {
    public AutoReloadCommand() {
        label("autoreload");
        permission("autoreload.use");
        subCommand(
                new OCommand()
                .label("reload")
                .argument(new PluginsListArg())
                .onCommand(command -> {
                    File pluginFile = command.getArgAsReq("plugin");
                    AutoReload plugin = (AutoReload) Engine.getInstance().getOwning();

                    try {
                        Handler.handle(plugin, pluginFile, plugin.type);
                        command.getSenderAsPlayer().sendMessage(Helper.color("&aSuccess in reloading " + pluginFile.getName().replace(".jar", "")));
                    } catch (Throwable throwable) {
                        command.getSenderAsPlayer().sendMessage(Helper.color("&cError while reloading" +  pluginFile.getName().replace(".jar", "") + ". Check console."));
                        throwable.printStackTrace();
                    }
                })
        );
    }
}
