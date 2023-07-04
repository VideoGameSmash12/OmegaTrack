package me.videogamesm12.omegatrack;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import me.videogamesm12.omegatrack.exception.StartFailureException;
import me.videogamesm12.omegatrack.storage.OTFlags;

import java.util.Timer;

/**
 * <h1>OmegaTrack</h1>
 * <p>The home of all things related to OmegaTrack.</p>
 */
public class OmegaTrack
{
    public final Tracker tracker;
    public final Wiretap wiretap;
    public final PostgreSQLStorage storage;
    //--
    public final OTFlags flags;
    public final EpsilonBot epsilonBot;
    public final Timer timer = new Timer();

    public OmegaTrack(final EpsilonBot epsilonBot) throws StartFailureException {
        this.epsilonBot = epsilonBot;
        this.flags = OTFlags.load(this.epsilonBot);
        try
        {
            this.storage = new PostgreSQLStorage(this.timer);
        }
        catch (Exception ex)
        {
            throw new StartFailureException(ex);
        }
        this.wiretap = new Wiretap(this);
        this.tracker = new Tracker(this);
    }

    public void start() {
        this.wiretap.start();
        this.tracker.start();
    }

    public void stop() {
        this.timer.cancel();
    }
}
