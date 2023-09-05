package me.videogamesm12.omegatrack.storage;

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

    public SQL sql = new SQL();

    public WiretapConfig wiretap = new WiretapConfig();

    @Getter
    public static class SQL
    {
        /**
         * The IP address of the PostgreSQL server that we store our data at
         */
        private String ip = "localhost";

        /**
         * The port of the PostgreSQL server that we store our data at
         */
        private int port = 5432;

        /**
         * The username for the PostgreSQL user that we use to store our data
         */
        private String username = "alexandria";

        /**
         * The password for the PostgreSQL user that we use to store our data
         */
        private String password = "password";

        /**
         * The name of the database that we store our data at
         */
        private String database = "omegatrack";
    }

    @Getter
    public static class WiretapConfig
    {
        /**
         * If set to true, the bot will not reset the backwards bruteforcer if an entity that isn't a squid spawns in.
         *  This is useful for in case
         */
        private boolean anythingButSquidsIgnoredOnSpawn = true;
    }
}
