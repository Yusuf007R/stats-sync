package com.yusuf007r.statssync;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ObjectiveArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stat;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public class StatsSyncCommand {

  final private CommandSourceStack source;
  final private MinecraftServer server;
  final private ServerScoreboard scoreboard;
  final private CommandContext<CommandSourceStack> context;
  private final UserCacheManager userCacheManager;
  public static final Logger LOGGER = StatsSync.LOGGER;


  StatsSyncCommand(CommandContext<CommandSourceStack> context) {
    this.context = context;
    this.source = context.getSource();
    this.server = source.getServer();
    this.scoreboard = server.getScoreboard();
    this.userCacheManager = new UserCacheManager(context);

  }


  public int executeAllObjectivesAllPlayers() {
    source.sendSuccess(() -> Component.literal("Updating all objectives for all players"), false);
    scoreboard.getObjectives()
            .forEach(this::updateObjectiveForAllPlayers);

    return 1;
  }

  public int executeSpecificObjectiveAllPlayers() throws CommandSyntaxException {
    Objective objective = ObjectiveArgument.getObjective(context, "objective");
    source.sendSuccess(() -> Component.literal("Updating objective " + objective.getDisplayName()
            .getString() + " for all players"), false);
    updateObjectiveForAllPlayers(objective);

    return 1;
  }

  public int executeSpecificObjectiveSpecificPlayer() throws CommandSyntaxException {
    Objective objective = ObjectiveArgument.getObjective(context, "objective");
    String playerName = StringArgumentType.getString(context, "player");
    Optional<UserCacheManager.Entry> entry = userCacheManager.getUserByName(playerName);
    if (entry.isEmpty()) {
      LOGGER.error("couldn't find user {} in the cache", playerName);
    }
    source.sendSuccess(() -> Component.literal("Updating objective " + objective.getDisplayName()
            .getString() + " for player " + entry.orElseThrow()
            .profile()
            .getName()), false);
    updateObjective(objective, entry.orElseThrow()
            .profile());

    return 1;
  }


  private void updateObjectiveForAllPlayers(Objective objective) {
    try {
      source.sendSuccess(() -> Component.literal("Updating: " + objective.getDisplayName()
              .getString()), false);
      for (UserCacheManager.Entry entry : userCacheManager.getUserCache()) {
        updateObjective(objective, entry.profile());
      }
    } catch (Exception e) {
      LOGGER.error("Error while updating {}: {}", objective.getDisplayName()
              .getString(), e.getMessage());
      source.sendSuccess(() -> Component.literal("Error while updating: " + objective.getDisplayName()
              .getString()), false);
    }
  }

  void updateObjective(@NotNull Objective objective, GameProfile profile) {
    ObjectiveCriteria criterion = objective.getCriteria();
    UUID playerUUID = profile.getId();

    Path statsPath = getStatsFilePath(playerUUID);
    if (!statsPath.toFile()
            .exists()) {
      LOGGER.warn("Stats file not found for player: {}", profile.getName());
      return;
    }

    int statValue = getStatValue(statsPath, criterion);
    if (statValue > 0) {
      updateScore(objective, profile, statValue);
    }
  }


  private @NotNull Path getStatsFilePath(UUID playerUUID) {
    return server.getWorldPath(LevelResource.PLAYER_STATS_DIR)
            .resolve(playerUUID + ".json");
  }


  private int getStatValue(@NotNull Path statsPath, ObjectiveCriteria criterion) {
    ServerStatsCounter statHandler = new ServerStatsCounter(server, statsPath.toFile());
    return statHandler.getValue((Stat<?>) criterion);
  }

  
  private void updateScore(Objective objective, GameProfile profile, int value) {
    ScoreAccess scoreAccess = scoreboard.getOrCreatePlayerScore(ScoreHolder.fromGameProfile(profile), objective);
    scoreAccess.set(value);
  }

}
