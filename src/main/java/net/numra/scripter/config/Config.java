package net.numra.scripter.config;

import org.apache.logging.log4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;

import java.io.File;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

public class Config {
    private static final Map<ConfigType, Map<Integer, List<Integer>>> compatibleVersions = Map.ofEntries(
            entry(ConfigType.Main, Map.ofEntries(
                    entry(1, List.of(1))
            )),
            entry(ConfigType.Script, Map.ofEntries(
                    entry(1, List.of(1))
            )),
            entry(ConfigType.Schedule, Map.ofEntries(
                    entry(1, List.of(1))
            ))
    );
    
    private Logger logger;
    private ConfigInitializer initializer;
    private CommentedConfigurationNode config;
    private HoconConfigurationLoader loader;
    private final File file;
    
    public void unsafeReload() throws ConfigurateException {
        config = loader.load();
    }
    
    public boolean reload() {
        try {
            unsafeReload();
            return true;
        } catch (ConfigurateException e) {
            e.printStackTrace();
            logger.error("Unable to reload config " + file.getName());
            return false;
        }
    }
    
    public CommentedConfigurationNode get() {
        return config;
    }
    
    public File getFile() {
        return file;
    }
    
    public void unsafeSave() throws ConfigurateException {
        loader.save(config);
    }
    
    public boolean save() {
        try {
            unsafeSave();
            return true;
        } catch (ConfigurateException e) {
            e.printStackTrace();
            logger.error("Unable to save config " + file.getName());
            return false;
        }
    }
    
    public boolean delete() {
        boolean success = file.delete();
        if (success) reload();
        return success;
    }
    
    public boolean compatibleVersion(int version, ConfigType type) {
        return compatibleVersions.get(type).get(version).contains(config.node("version").getInt(-1));
    }
    
    public void verifyVersion(int version, ConfigType type) {
        if (!compatibleVersion(version, type)) {
            this.delete();
            this.initializer.init(config);
        }
    }
    
    public Config(File file, Logger logger, ConfigInitializer initializer) {
        this.logger = logger;
        this.initializer = initializer;
        this.file = file;
        this.loader = HoconConfigurationLoader.builder().path(file.toPath()).build();
        reload();
        if (!file.exists()) {
            initializer.init(config);
            save();
        }
        reload();
    }
}
