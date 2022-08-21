package net.numra.scripter;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.numra.scripter.command.ScripterCommand;
import net.numra.scripter.config.Config;
import net.numra.scripter.config.ConfigType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.Map.entry;

public class Scripter implements ModInitializer {
    private final File configDir = new File(FabricLoader.getInstance().getConfigDir().toFile(), "numra/scripter/");
    public final Logger logger = LogManager.getLogger("NumraScripter");
    public static final Map<ConfigType, Integer> configVersions = Map.ofEntries(
            entry(ConfigType.Main, 1),
            entry(ConfigType.Script, 1),
            entry(ConfigType.Schedule, 1)
    );
    private final Map<ConfigType, Config> configs = Map.ofEntries(
            entry(ConfigType.Main, new Config(new File(configDir, "config.conf"), logger, config -> {
                config.node("enabled").raw(true);
                config.node("scriptDir").raw(new File(configDir, "scripts").getAbsolutePath());
                CommentedConfigurationNode versionNode = config.node("version");
                versionNode.raw(configVersions.get(ConfigType.Main));
                versionNode.comment("Don't change me!");
            })),
            entry(ConfigType.Script, new Config(new File(configDir, "scripts.conf"), logger, config -> {
                config.comment("[Script name]: [Script file]");
                config.node("scripts").node("example").raw("example.bat");
                CommentedConfigurationNode versionNode = config.node("version");
                versionNode.raw(configVersions.get(ConfigType.Script));
                versionNode.comment("Don't change me!");
            })),
            entry(ConfigType.Schedule, new Config(new File(configDir, "schedules.conf"), logger, config -> {
                config.comment("time: [time in seconds]\ncommand: [command to run]");
                CommentedConfigurationNode listNode = config.node("timed").appendListNode();
                listNode.node("time").raw(86400);
                listNode.node("command").raw("example");
                CommentedConfigurationNode versionNode = config.node("version");
                versionNode.raw(configVersions.get(ConfigType.Schedule));
                versionNode.comment("Don't change me!");
            }))
    );
    
    private boolean enabled;
    private File scriptDir;
    private HashMap<String, File> scripts = new HashMap<>();
    private ArrayList<TimedCommand> timedCommands = new ArrayList<>();
    
    @Override
    public void onInitialize() {
        configs.forEach((type, config) -> config.verifyVersion(configVersions.get(type), type));
    
        CommandRegistrationCallback.EVENT.register(new ScripterCommand(this));
        
        ServerLifecycleEvents.SERVER_STARTED.register(this::reloadConfigs);
    }
    
    public Map<String, File> getScripts() {
        return scripts;
    }
    
    public boolean isLooseDisabled() {
        return !enabled;
    }
    
    public void reloadConfigs(MinecraftServer server) {
        logger.info("Reloading Configs");
        
        configs.values().forEach(Config::reload);
        
        loadConfigVars(server);
    }
    
    private void loadConfigVars(MinecraftServer server) {
        // config.yml
        CommentedConfigurationNode main = configs.get(ConfigType.Main).get();
        enabled = main.node("enabled").getBoolean();
        if (!enabled) {
            logger.warn("Disabled by config.");
            return;
        }
        scriptDir = new File(main.node("scriptDir").getString(new File(configDir, "scripts").getAbsolutePath()));
        if (!scriptDir.exists() && !scriptDir.mkdirs()) logger.error("Script Dir Creation Failed");
    
        // scripts.yml
        CommentedConfigurationNode script = configs.get(ConfigType.Script).get();
        scripts.clear();
        script.node("scripts").childrenMap().forEach((key, node) -> scripts.put((String) key, new File(scriptDir, Objects.requireNonNull(node.getString()))));
        // schedules.yml
        timedCommands.forEach(TimedCommand::cancel);
        timedCommands.clear();
        CommentedConfigurationNode schedule = configs.get(ConfigType.Schedule).get();
        schedule.node("timed").childrenList().forEach(node -> {
            int time = node.node("time").getInt();
            String commandStr = node.node("command").getString();
            if (time <= 0 || commandStr == null) {
                logger.warn("Invalid entry in schedule.conf! Skipping.");
            } else {
                try {
                    timedCommands.add(new TimedCommand(commandStr, time, server, logger));
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid entry in schedule.conf! Skipping.");
                }
            }
        });
        timedCommands.forEach(TimedCommand::start);
    }
}
