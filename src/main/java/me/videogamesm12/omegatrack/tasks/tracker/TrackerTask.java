package me.videogamesm12.omegatrack.tasks.tracker;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundEntityTagQuery;
import me.videogamesm12.omegatrack.OmegaTrack;

import java.util.TimerTask;

public class TrackerTask extends TimerTask
{
    private final OmegaTrack omegaTrack;

    public TrackerTask(final OmegaTrack omegaTrack)
    {
        this.omegaTrack = omegaTrack;
    }

    @Override
    public void run()
    {
        for (int i : this.omegaTrack.wiretap.getUuids().values())
        {
            if (this.omegaTrack.flags.getFlags(this.omegaTrack.wiretap.getById(i)).isOptedOut())
            {
                this.omegaTrack.wiretap.unlink(this.omegaTrack.wiretap.getById(i));
                continue;
            }

            this.omegaTrack.epsilonBot.sendPacket(new ServerboundEntityTagQuery(i, i));
        }
    }
}
