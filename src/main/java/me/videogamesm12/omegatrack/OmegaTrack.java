package me.videogamesm12.omegatrack;

import me.videogamesm12.omegatrack.storage.OTFlags;

/**
 * <h1>OmegaTrack</h1>
 * <p>The home of all things related to OmegaTrack.</p>
 */
public class OmegaTrack
{
    public static Tracker TRACKER;
    public static Wiretap WIRETAP;
    public static PostgreSQLStorage STORAGE;
    //--
    public static OTFlags FLAGS;

    public void start()
    {
        FLAGS = OTFlags.load();
        try
        {
            STORAGE = new PostgreSQLStorage();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        WIRETAP = new Wiretap();
        TRACKER = new Tracker();
    }

    public void stop()
    {
        WIRETAP = null;
        STORAGE = null;
        TRACKER = null;
    }
}
