package com.yusuf007r.statssync;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stat;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.slf4j.Logger;

import java.util.Optional;

public class StatsCommand {


  final private CommandSourceStack source;
  final private MinecraftServer server;
  final private CommandContext<CommandSourceStack> context;
  final private UserCacheManager userCacheManager;
  public static final Logger LOGGER = StatsSync.LOGGER;


  StatsCommand(CommandContext<CommandSourceStack> context) {
    this.context = context;
    this.source = context.getSource();
    this.server = source.getServer();
    this.userCacheManager = new UserCacheManager(context);
  }



  public int execute(){
    ServerPlayer serverPlayer = source.getPlayer();
    assert serverPlayer != null;
    ServerStatsCounter serverStatsCounter = serverPlayer.getStats();

    String stat = StringArgumentType.getString(context, "stat");
    Optional<ObjectiveCriteria> x = Stat.byName(stat);
    assert x.isPresent();
    int value =serverStatsCounter.getValue((Stat<?>) x.get());
    source.sendSuccess(()-> Component.literal(String.valueOf(value)), false);
    return 1;
  }
}
