package me.videogamesm12.omegatrack.tasks.wiretap;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import me.videogamesm12.omegatrack.Wiretap;

import java.util.TimerTask;

public abstract class AbstractWiretapDependentTask extends TimerTask
{
    protected final Wiretap wiretap;
    protected final EpsilonBot epsilonBot;

    protected AbstractWiretapDependentTask(final Wiretap wiretap)
    {
        this.wiretap = wiretap;
        this.epsilonBot = this.wiretap.getEpsilonBot();
    }
}
