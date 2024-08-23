package com.yusuf007r.statssync;

import com.google.common.collect.Maps;
import com.google.gson.*;
import com.mojang.authlib.GameProfile;
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

public class UserCacheManager {
    private final Map<String, Entry> byName = Maps.newHashMap();
    public static final Logger LOGGER = StatsSync.LOGGER;

    public record Entry(GameProfile profile, Date expirationDate) {

    }

    public UserCacheManager() {
        this.init();
    }


    public Collection<Entry> getUserCache() {
        return byName.values();
    }

    public Optional<Entry> getUserByName(String name) {
        return Optional.ofNullable(byName.get(name));
    }

    private void add(Entry entry) {
        if (!StringUtil.isValidPlayerName(entry.profile.getName())) return;
        if ((byName.containsKey(entry.profile.getName()) && byName.get(entry.profile.getName()).expirationDate.after(entry.expirationDate)))
            return;
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
}
