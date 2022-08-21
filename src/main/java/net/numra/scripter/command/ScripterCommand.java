package net.numra.scripter.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.numra.scripter.Scripter;

import java.io.File;
import java.io.IOException;


import static net.minecraft.server.command.CommandManager.*;

public class ScripterCommand implements CommandRegistrationCallback {
    private final Scripter mod;
    
    // Indentation is weird but IntelliJ doesn't like me trying to fix it.
    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        LiteralCommandNode<ServerCommandSource> scripterCommand = dispatcher.register(
                literal("scripter")
                        .requires(source -> source.hasPermissionLevel(4))
                        .executes(context -> {
                            context.getSource().sendError(Text.literal("No arguments. Try /scripter help"));
                            return 1;
                        })
                        .then(literal("help")
                                .executes(context -> {
                                    context.getSource().sendFeedback(Text.literal("""
                                            /scripter help
                                                Sends a list of all commands
                                            /scripter reload
                                                Reloads Scripter configs
                                            /scripter run [script]
                                                Runs the script named [script]
                                            /sc
                                                Alias for /scripter
                                            """), false);
                                    return 1;
                                })
                        )
                        .then(literal("reload")
                                .executes(context -> {
                                    mod.reloadConfigs(context.getSource().getServer());
                                    context.getSource().sendFeedback(Text.literal("Scripter Configs Reloaded"), true);
                                    return 1;
                                })
                        )
                        .then(literal("run")
                                .executes(context -> {
                                    context.getSource().sendError(Text.literal("Missing argument. Try /scripter help"));
                                    return 1;
                                })
                                .then(argument("scriptName", StringArgumentType.string())
                                        .suggests(((context, builder) -> CommandSource.suggestMatching(mod.getScripts().keySet(), builder)))
                                        .executes(context -> {
                                            if (mod.isLooseDisabled()) {
                                                context.getSource().sendError(Text.literal("Mod is disabled"));
                                                return 1;
                                            }
                                            String scriptName = StringArgumentType.getString(context, "scriptName");
                                            File script = mod.getScripts().get(scriptName);
                                            if (script == null) {
                                                context.getSource().sendError(Text.literal("Unknown script name"));
                                                return 1;
                                            }
                                            if (!script.exists()) {
                                                context.getSource().sendError(Text.literal("File doesn't exist"));
                                                return 1;
                                            }
                                            if (!script.canExecute()) {
                                                context.getSource().sendError(Text.literal("File is not executable"));
                                                return 1;
                                            }
                                            try {
                                                Runtime.getRuntime().exec(script.getAbsolutePath());
                                                context.getSource().sendFeedback(Text.literal("Running script " + scriptName), true);
                                                mod.logger.info(context.getSource().getDisplayName().copy().append(Text.literal(" ran script " + scriptName)));
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                                context.getSource().sendError(Text.literal("Unknown error. Check logs."));
                                            }
                                            return 1;
                                        })
                                )
                        )
        );
        dispatcher.register(literal("sc").requires(source -> source.hasPermissionLevel(4)).redirect(scripterCommand));
    }
    
    public ScripterCommand(Scripter mod) {
        this.mod = mod;
    }
}
