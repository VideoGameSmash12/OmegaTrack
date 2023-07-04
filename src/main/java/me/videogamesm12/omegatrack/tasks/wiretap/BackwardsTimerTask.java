package me.videogamesm12.omegatrack.tasks.wiretap;

import me.videogamesm12.omegatrack.Wiretap;

public class BackwardsTimerTask extends AbstractWiretapDependentTask
{
    public BackwardsTimerTask(final Wiretap wiretap)
    {
        super(wiretap);
    }

    @Override
    public void run()
    {
        // Has the account been linked already or opted out of tracking?
        if (!this.wiretap.isLinked(this.wiretap.currentBackwardsId))
        {
            this.wiretap.doWiretapBruteQuery(this.wiretap.currentBackwardsId);
        }

        // Decrement the ID for the next query if it's above 0
        if (this.wiretap.currentBackwardsId > 0)
        {
            this.wiretap.currentBackwardsId--;
        }
        // We've brute-forced as much as we could in this manner.
        else if (this.wiretap.currentBackwardsId == 0)
        {
            cancel();
        }
    }
}
