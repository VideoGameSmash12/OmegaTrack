package me.videogamesm12.omegatrack.tasks.wiretap;

import me.videogamesm12.omegatrack.Wiretap;

import java.util.TimerTask;

public abstract class AbstractWiretapDependentTask extends TimerTask
{
    protected final Wiretap wiretap;

    protected AbstractWiretapDependentTask(final Wiretap wiretap)
    {
        this.wiretap = wiretap;
    }
}
