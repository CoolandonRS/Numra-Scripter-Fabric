package net.numra.scripter.config;

import org.spongepowered.configurate.CommentedConfigurationNode;

public interface ConfigInitializer {
    void init(CommentedConfigurationNode node);
}
