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

    private General general = new General();

    public SQL sql = new SQL();

    public WiretapConfig wiretap = new WiretapConfig();

    @Getter
    public static class General
    {
        /**
         * <p>If set to true, the bot will opt to instead contact the server directly with a special message asking for
         *  all player entity IDs. This mode completely disables entity brute-forcing because there's no need to do so
         *  if the server directly provides entity IDs for us. A huge benefit to this mode is that it's dramatically
         *  faster to get entity IDs than brute-forcing things.</p>
         */
        private boolean usingDeltaCommunication = true;
    }

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
        private String password = "alexandria";

        /**
         * The name of the database that we store our data at
         */
        private String database = "omegatrack";

        /**
         * The period of time between each time the bot sends queued data to the database
         */
        private int queueInterval = 50;
    }

    @Getter
    public static class WiretapConfig
    {
        /**
         * If set to true, the bot will not reset the backwards bruteforcer if an entity that isn't a squid spawns in.
         *  This is useful for in case someone tries to spam entities to slow down the player-finding process.
         * This has no effect when {@code usingDeltaCommunication} is set to true, as this is used for the bruteforcing
         *  process.
         */
        private boolean anythingButSquidsIgnoredOnSpawn = true;
    }
}
