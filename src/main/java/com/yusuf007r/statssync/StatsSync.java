package com.yusuf007r.statssync;


import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.arguments.ObjectiveArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.SharedSuggestionProvider.suggest;


public class StatsSync implements ModInitializer {
    public static final String MOD_ID = "stats-sync";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final UserCacheManager userCacheManager = new UserCacheManager();


    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("statSync")
                .requires(source -> source.hasPermission(4))
                .executes((c) -> new StatsSyncCommand(c).executeAllObjectivesAllPlayers())
                .then(argument("objective", new ObjectiveArgument()).executes(c -> new StatsSyncCommand(c).executeSpecificObjectiveAllPlayers())
                        .then(argument("player", StringArgumentType.word()).suggests((c, b) -> suggest(userCacheManager.getUserCache()
                                        .stream()
                                        .map(user -> user.profile()
                                                .getName()), b))
                                .executes(c -> new StatsSyncCommand(c).executeSpecificObjectiveSpecificPlayer())))));
    }
}