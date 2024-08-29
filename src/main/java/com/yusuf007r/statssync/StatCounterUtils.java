package com.yusuf007r.statssync;

import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stat;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class StatCounterUtils {


  public static int getStatValue(@NotNull Path statsPath, ObjectiveCriteria criterion, MinecraftServer server) {
    ServerStatsCounter statHandler = new ServerStatsCounter(server, statsPath.toFile());
    return statHandler.getValue((Stat<?>) criterion);
  }
}
