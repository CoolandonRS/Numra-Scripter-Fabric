package net.numra.scripter;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TimedCommand {
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private ParseResults<ServerCommandSource> command;
    private int time;
    private CommandDispatcher<ServerCommandSource> dispatcher;
    private Logger logger;
    private ScheduledFuture<?> future;
    
    public void start() {
        future = executor.scheduleAtFixedRate(this::run, time, time, TimeUnit.SECONDS);
    }
    
    public void cancel() {
        future.cancel(false);
    }
    
    private void run() {
        try {
            dispatcher.execute(command);
            logger.info("Ran scheduled command " + command.getReader().getString());
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
            logger.error("Invalid command " + command.getReader().getString());
        }
    }
    
    public TimedCommand(String commandStr, int time, MinecraftServer server, Logger logger) throws IllegalArgumentException {
        this.time = time;
        this.dispatcher = server.getCommandManager().getDispatcher();
        this.command = dispatcher.parse(commandStr, server.getCommandSource());
        this.logger = logger;
    }
}


















