package me.videogamesm12.omegatrack;

import me.videogamesm12.omegatrack.storage.OTFlags;

import java.util.Timer;

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
    public static final Timer TIMER = new Timer();

    public void start()
    {
        FLAGS = OTFlags.load();
        try
        {
            STORAGE = new PostgreSQLStorage(TIMER);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        WIRETAP = new Wiretap(TIMER);
        TRACKER = new Tracker(TIMER);
    }

    public void stop()
    {
        WIRETAP = null;
        STORAGE = null;
        TRACKER = null;
    }
}
