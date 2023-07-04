package me.videogamesm12.omegatrack.tasks.wiretap;

import com.github.steveice10.packetlib.packet.Packet;
import me.videogamesm12.omegatrack.Wiretap;

import java.util.Queue;

public class PacketSenderTask extends AbstractWiretapDependentTask
{
    private final Queue<Packet> associatedPacketQueue;

    public PacketSenderTask(final Wiretap wiretap, final Queue<Packet> associatedPacketQueue)
    {
        super(wiretap);
        this.associatedPacketQueue = associatedPacketQueue;
    }

    @Override
    public void run()
    {
        for (int i = 0; i < this.associatedPacketQueue.size(); i++)
        {
            if (!this.epsilonBot.getStateManager().isOnFreedomServer())
                break;

            this.epsilonBot.sendPacket(this.associatedPacketQueue.poll());
        }
    }
}
