package com.yusuf007r.statssync;

import static net.minecraft.server.command.CommandManager.*;


import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.ScoreboardCriterionArgumentType;
import net.minecraft.command.argument.ScoreboardObjectiveArgumentType;
import net.minecraft.scoreboard.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.stat.ServerStatHandler;
import net.minecraft.stat.Stat;
import net.minecraft.text.Text;
import net.minecraft.util.UserCache;
import net.minecraft.util.WorldSavePath;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class StatsSync implements ModInitializer {
    public static final String MOD_ID = "statssync";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    void updateObjective(MinecraftServer server, ScoreboardObjective objective, ScoreHolder scoreHolder) {
        ScoreboardCriterion criterion = objective.getCriterion();
        Optional<UUID> playerUUID = getPlayerUUID(server, scoreHolder);

        if (playerUUID.isEmpty()) {
            LOGGER.warn("Could not find UUID for player: {}", scoreHolder.getNameForScoreboard());
            return;
        }

        Path statsPath = getStatsFilePath(server, playerUUID.get());
        if (!statsPath.toFile().exists()) {
            LOGGER.warn("Stats file not found for player: {}", scoreHolder.getNameForScoreboard());
            return;
        }

        int statValue = getStatValue(server, statsPath, criterion);
        if (statValue > 0) {
            updateScore(server, objective, scoreHolder, statValue);
        }
    }

    private Optional<UUID> getPlayerUUID(@NotNull MinecraftServer server, ScoreHolder scoreHolder) {
        return Optional.ofNullable(server.getUserCache()).flatMap(cache -> cache.findByName(scoreHolder.getNameForScoreboard())).map(GameProfile::getId);
    }

    private @NotNull Path getStatsFilePath(@NotNull MinecraftServer server, UUID playerUUID) {
        return server.getSavePath(WorldSavePath.STATS).resolve(playerUUID + ".json");
    }

    private int getStatValue(MinecraftServer server, @NotNull Path statsPath, ScoreboardCriterion criterion) {
        ServerStatHandler statHandler = new ServerStatHandler(server, statsPath.toFile());
        return statHandler.getStat((Stat<?>) criterion);
    }

    private void updateScore(@NotNull MinecraftServer server, ScoreboardObjective objective, ScoreHolder scoreHolder, int value) {
        server.getScoreboard().getOrCreateScore(scoreHolder, objective).setScore(value);
    }


    @Override
    public void onInitialize() {
        LOGGER.info("Hello Fabric world!");

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("statSync").executes(context -> {
            ServerCommandSource source = context.getSource();
            MinecraftServer server = source.getServer();
            ServerScoreboard scoreboard = server.getScoreboard();


            source.sendFeedback(()-> Text.literal("Found " + scoreboard.getObjectives().size() + " Objectives, will try updating them with player stats"), false);
            scoreboard.getObjectives().forEach((obj) -> {
                try {
                    source.sendFeedback(()-> Text.literal("trying to update: " + obj.getDisplayName().getString()), false);
                    scoreboard.getKnownScoreHolders().forEach((scoreHolder -> {
                        updateObjective(server, obj, scoreHolder);
                    }));

                } catch (Exception e) {
                    LOGGER.info("Error while updating {}, {} the error was {}", obj.getDisplayName().getString(), obj.getCriterion().getName(), e.getMessage());
                    source.sendFeedback(()-> Text.literal("Error while updating: " + obj.getDisplayName().getString()), false);
                }
            });
            return 1;
        })));
    }
}