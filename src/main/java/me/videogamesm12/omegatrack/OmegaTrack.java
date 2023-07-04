package me.videogamesm12.omegatrack;

import lombok.Getter;
import me.videogamesm12.omegatrack.storage.OTFlags;

/**
 * <h1>OmegaTrack</h1>
 * <p>The home of all things related to OmegaTrack.</p>
 */
public class OmegaTrack
{
    @Getter
    public static Wiretap wiretap;
    //--
    @Getter
    private static Tracker tracker;
    @Getter
    private static PostgreSQLStorage postgreSQLStorage;
    @Getter
    private static OTFlags flagStorage;

    public void start()
    {
        flagStorage = OTFlags.load();
        try
        {
            postgreSQLStorage = new PostgreSQLStorage();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        wiretap = new Wiretap();
        tracker = new Tracker();
    }
}
