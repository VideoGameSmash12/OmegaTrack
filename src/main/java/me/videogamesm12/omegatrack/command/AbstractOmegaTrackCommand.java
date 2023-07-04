package me.videogamesm12.omegatrack.command;

import com.github.hhhzzzsss.epsilonbot.command.ChatCommand;
import me.videogamesm12.omegatrack.OmegaTrack;

public abstract class AbstractOmegaTrackCommand extends ChatCommand
{
    protected final OmegaTrack omegaTrack;

    protected AbstractOmegaTrackCommand(final OmegaTrack omegaTrack)
    {
        this.omegaTrack = omegaTrack;
    }
}
