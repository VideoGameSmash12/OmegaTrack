package me.videogamesm12.omegatrack.storage;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OTFlags
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File STORAGE = new File("flags.json");
    private final EpsilonBot epsilonBot;

    public OTFlags(final EpsilonBot epsilonBot)
    {
        this.epsilonBot = epsilonBot;
    }

    public static OTFlags load(final EpsilonBot epsilonBot)
    {
        if (STORAGE.exists())
        {
            try
            {
                return GSON.fromJson(new FileReader(STORAGE), OTFlags.class);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }

        return new OTFlags(epsilonBot);
    }

    private final Map<UUID, UserFlags> userFlags = new HashMap<>();

    @NotNull
    public UserFlags getFlags(UUID uuid)
    {
        if (!userFlags.containsKey(uuid))
            userFlags.put(uuid, new UserFlags());

        return userFlags.get(uuid);
    }

    public void save()
    {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(STORAGE), StandardCharsets.UTF_8))
        {
            writer.write(GSON.toJson(this));
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            this.epsilonBot.getChatLogger().log("Here's what was in memory that couldn't be saved:");
            this.epsilonBot.getChatLogger().log(GSON.toJson(this));
        }
    }

    @Data
    public static class UserFlags
    {
        private boolean optedOut = true;
        private boolean dontMsg = false;
        private boolean supposedToShutUp = false;
    }
}