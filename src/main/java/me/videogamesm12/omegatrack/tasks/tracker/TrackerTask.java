package me.videogamesm12.omegatrack.tasks.tracker;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundEntityTagQuery;
import me.videogamesm12.omegatrack.OmegaTrack;

import java.util.TimerTask;

public class TrackerTask extends TimerTask
{
    @Override
    public void run()
    {
        for (int i : OmegaTrack.WIRETAP.getUuids().values())
        {
            if (OmegaTrack.FLAGS.getFlags(OmegaTrack.WIRETAP.getById(i)).isOptedOut())
            {
                OmegaTrack.WIRETAP.unlink(OmegaTrack.WIRETAP.getById(i));
                continue;
            }

            EpsilonBot.INSTANCE.sendPacket(new ServerboundEntityTagQuery(i, i));
        }
    }
}
