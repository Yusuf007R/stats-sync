package com.yusuf007r.statssync;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringUtil;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static net.minecraft.commands.SharedSuggestionProvider.suggest;

public class UserCacheManager {
  private final Map<String, Entry> byName = new HashMap<String, Entry>();
  public static final Logger LOGGER = StatsSync.LOGGER;
  private final MinecraftServer server;
  private final CommandContext<CommandSourceStack> context;

  public record Entry(GameProfile profile, Date expirationDate) {

  }

  public UserCacheManager(CommandContext<CommandSourceStack> context) {
    this.context = context;
    this.server = context.getSource().getServer();
    this.init();
  }


  public Collection<Entry> getUserCache() {
    return byName.values();
  }

  public Optional<Entry> getUserByName(String name) {
    return Optional.ofNullable(getAllProfiles().get(name));
  }

  public Map<String, Entry> getAllProfiles() {
    Map<String, Entry> tempByName = new HashMap<>(byName);

    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
      String playerName = player.getName().getString();
      tempByName.put(playerName, new Entry(player.getGameProfile(), new Date()));
    }

    return tempByName;
  }

  private void add(Entry entry) {
    if (!StringUtil.isValidPlayerName(entry.profile.getName())) return;
    if ((byName.containsKey(entry.profile.getName()) && byName.get(entry.profile.getName()).expirationDate.after(
            entry.expirationDate))) return;
    byName.put(entry.profile.getName(), entry);
  }

  private void init() {
    try {
      Path path = Paths.get("./usercache.json");
      if (Files.exists(path)) {
        String content = new String(Files.readAllBytes(path));
        JsonElement jsonElement = JsonParser.parseString(content);
        if (jsonElement.isJsonArray()) {
          jsonElement.getAsJsonArray().forEach(json -> processJson(json).ifPresent(this::add));
        }

      }
    } catch (IOException e) {
      LOGGER.error(e.getMessage());
    }

  }

  private Optional<Entry> processJson(JsonElement json) {
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT);
    if (json.isJsonObject()) {
      JsonObject jsonObject = json.getAsJsonObject();
      JsonElement jsonElement = jsonObject.get("name");
      JsonElement jsonElement2 = jsonObject.get("uuid");
      JsonElement jsonElement3 = jsonObject.get("expiresOn");
      if (jsonElement != null && jsonElement2 != null) {
        String string = jsonElement2.getAsString();
        String string2 = jsonElement.getAsString();
        Date date = null;
        if (jsonElement3 != null) {
          try {
            date = dateFormat.parse(jsonElement3.getAsString());
          } catch (ParseException var12) {
            LOGGER.error(var12.getMessage());
          }
        }

        if (string2 != null && string != null && date != null) {
          UUID uUID;
          try {
            uUID = UUID.fromString(string);
          } catch (Throwable var11) {
            return Optional.empty();
          }

          GameProfile gameProfile = new GameProfile(uUID, string2);
          return Optional.of(new Entry(gameProfile, date));
        } else {
          return Optional.empty();
        }
      } else {
        return Optional.empty();
      }
    } else {
      return Optional.empty();
    }
  }


  public CompletableFuture<Suggestions> getSuggestions(SuggestionsBuilder builder) {
    Stream<String> profileNames = getAllProfiles().values().stream()
            .map(entry -> entry.profile().getName());

    return suggest(profileNames, builder);
  }
}
