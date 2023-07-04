package me.videogamesm12.omegatrack.tasks.wiretap;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.steveice10.packetlib.packet.Packet;

import java.util.Queue;
import java.util.TimerTask;

public class WiretapTask extends TimerTask
{
    private final Queue<Packet> associatedPacketQueue;

    public WiretapTask(final Queue<Packet> associatedPacketQueue)
    {
        this.associatedPacketQueue = associatedPacketQueue;
    }

    @Override
    public void run()
    {
        for (int i = 0; i < this.associatedPacketQueue.size(); i++)
        {
            if (!EpsilonBot.INSTANCE.getStateManager().isOnFreedomServer())
                break;

            EpsilonBot.INSTANCE.sendPacket(this.associatedPacketQueue.poll());
        }
    }
}
