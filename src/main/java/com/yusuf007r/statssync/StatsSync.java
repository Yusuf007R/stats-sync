package com.yusuf007r.statssync;


import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.arguments.ObjectiveArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;


public class StatsSync implements ModInitializer {
  public static final String MOD_ID = "stats-sync";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);


  public void onInitialize() {
    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
            literal("statSync").requires(source -> source.hasPermission(4))
                    .executes((c) -> new StatsSyncCommand(c).executeAllObjectivesAllPlayers())
                    .then(argument("objective", new ObjectiveArgument()).executes(
                                    c -> new StatsSyncCommand(c).executeSpecificObjectiveAllPlayers())
                            .then(argument("player", StringArgumentType.word()).suggests(
                                            (c, b) -> new UserCacheManager(c).getSuggestions(b))
                                    .executes(
                                            c -> new StatsSyncCommand(c).executeSpecificObjectiveSpecificPlayer())))));

//    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
//            literal("stats").then(argument("stat", StringArgumentType.greedyString()).suggests((c, b) -> {
//              List<String> list = Lists.<String>newArrayList(ObjectiveCriteria.getCustomCriteriaNames());
//
//              for (StatType<?> statType : BuiltInRegistries.STAT_TYPE) {
//                for (Object object : statType.getRegistry()) {
//                  String string = this.getName(statType, object);
//                  list.add(string);
//                }
//              }
//
//              return suggest(list, b);
//            }).executes(c -> new StatsCommand(c).execute()))));
  }
//
//  public <T> String getName(StatType<T> statType, Object object) {
//    return Stat.buildName(statType, (T) object);
//  }
}