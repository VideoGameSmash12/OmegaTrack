package me.videogamesm12.omegatrack.tasks.wiretap;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import me.videogamesm12.omegatrack.Wiretap;

public class TraditionalTimerTask extends AbstractWiretapDependentTask
{
    public TraditionalTimerTask(final Wiretap wiretap)
    {
        super(wiretap);
    }

    @Override
    public void run()
    {
        // If we aren't on the server, let's go ahead and reset everything to 0 just in case the server
        //  restarted. Note that getting kicked for any reason would also result in this behavior.
        if (!this.epsilonBot.getStateManager().isOnFreedomServer())
        {
            this.wiretap.currentTraditionalId = 0;
            return;
        }

        // Wraps back
        if (this.wiretap.currentTraditionalId == Integer.MAX_VALUE)
            this.wiretap.currentTraditionalId = 0;
        else
            this.wiretap.currentTraditionalId++;

        // Has the account been linked already or opted out of tracking?
        if (!this.wiretap.isLinked(this.wiretap.currentTraditionalId))
            this.wiretap.doWiretapBruteQuery(this.wiretap.currentTraditionalId);
    }
}
