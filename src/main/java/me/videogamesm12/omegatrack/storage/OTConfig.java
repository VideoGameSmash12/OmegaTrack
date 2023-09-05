package me.videogamesm12.omegatrack.storage;

import com.github.steveice10.mc.protocol.data.game.entity.type.EntityType;
import com.google.gson.Gson;
import lombok.Data;
import lombok.Getter;

import java.io.*;
import java.nio.charset.StandardCharsets;

@Data
public class OTConfig
{
    private static final Gson GSON = new Gson();
    //--
    public static OTConfig INSTANCE;

    static
    {
        File file = new File("omegatrack.json");

        if (file.exists())
        {
            try
            {
                INSTANCE = GSON.fromJson(new FileReader("omegatrack.json"), OTConfig.class);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                copyDefaults();
            }
        }
        else
        {
            copyDefaults();
        }
    }

    public static void copyDefaults()
    {
        INSTANCE = new OTConfig();

        try (Writer writer = new OutputStreamWriter(new FileOutputStream("omegatrack.json"), StandardCharsets.UTF_8))
        {
            writer.write(GSON.toJson(INSTANCE));
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    //--

    public String sqlIp = "localhost";

    public int sqlPort = 5432;

    public String sqlUser = "alexandria";

    public String sqlPassword = "alexandria";

    public WiretapConfig wiretap = new WiretapConfig();

    @Getter
    public static class WiretapConfig
    {
        /**
         * If set to true, the bot will not reset the backwards bruteforcer if an entity that isn't a squid spawns in
         */
        private boolean anythingButSquidsIgnoredOnSpawn = true;
    }
}
