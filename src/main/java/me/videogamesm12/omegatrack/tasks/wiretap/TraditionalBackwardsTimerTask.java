package me.videogamesm12.omegatrack.tasks.wiretap;

import me.videogamesm12.omegatrack.Wiretap;

public class TraditionalBackwardsTimerTask extends AbstractWiretapDependentTask {
    public TraditionalBackwardsTimerTask(final Wiretap wiretap) {
        super(wiretap);
    }

    @Override
    public void run() {
        // Wraps back
        if (this.wiretap.currentTraditionalBackwardsId == 0)
            cancel();
        else
            this.wiretap.currentTraditionalBackwardsId--;

        // Has the account been linked already or opted out of tracking?
        if (!this.wiretap.isLinked(this.wiretap.currentTraditionalBackwardsId))
            this.wiretap.doWiretapBruteQuery(this.wiretap.currentTraditionalBackwardsId);

    }
}
