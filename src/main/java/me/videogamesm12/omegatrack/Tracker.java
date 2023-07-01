package me.videogamesm12.omegatrack;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundTagQueryPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundEntityTagQuery;
import com.github.steveice10.opennbt.tag.builtin.DoubleTag;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import me.videogamesm12.omegatrack.data.PositionDataset;
import me.videogamesm12.omegatrack.util.UUIDUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * <h1>Tracker</h1>
 * <p>The part of OmegaTrack that tracks known players.</p>
 */
public class Tracker extends SessionAdapter
{
    private final ScheduledExecutorService querier = new ScheduledThreadPoolExecutor(1);

    public Tracker()
    {
        querier.scheduleAtFixedRate(() ->
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
        }, 0, 1500, TimeUnit.MILLISECONDS);

        EpsilonBot.INSTANCE.getSession().addListener(this);
    }

    @Override
    public void packetReceived(Session session, Packet packet)
    {
        if (packet instanceof ClientboundTagQueryPacket queryPacket
                && queryPacket.getNbt().contains("EnderItems"))
        {
            int[] uuid = (int[]) queryPacket.getNbt().get("UUID").getValue();
            UUID convUuid = UUIDUtil.fromIntArray(uuid);

            if (OmegaTrack.FLAGS.getFlags(convUuid).isOptedOut())
                return;

            String world = (String) queryPacket.getNbt().get("Dimension").getValue();
            Object posRaw = queryPacket.getNbt().get("Pos").getValue();

            if (!(posRaw instanceof ArrayList))
                return;

            List<DoubleTag> doubles = (List<DoubleTag>) queryPacket.getNbt().get("Pos").getValue();

            System.out.println("Sending " + convUuid + " to database");
            try
            {
                OmegaTrack.STORAGE.queue(new PositionDataset(uuid, world, doubles.get(0).getValue(), doubles.get(2).getValue()));
            }
            catch (Throwable ex)
            {
                ex.printStackTrace();
            }
        }
    }

    public void stop()
    {
        try
        {
            querier.awaitTermination(5000, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }
}
