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
            for (int i : OmegaTrack.getWiretap().getUuids().values())
            {
                if (OmegaTrack.getFlagStorage().getFlags(OmegaTrack.getWiretap().getById(i)).isOptedOut())
                {
                    OmegaTrack.getWiretap().unlink(OmegaTrack.getWiretap().getById(i));
                    continue;
                }

                EpsilonBot.INSTANCE.sendPacket(new ServerboundEntityTagQuery(i, i));
            }
        }, 0, 3000, TimeUnit.MILLISECONDS);

        EpsilonBot.INSTANCE.getSession().addListener(this);
    }

    @Override
    public void packetReceived(Session session, Packet packet)
    {
        if (packet instanceof ClientboundTagQueryPacket queryPacket
                && queryPacket.getNbt().contains("EnderItems"))
        {
            // Gets the user's UUID
            final Object uuidObject = queryPacket.getNbt().get("UUID").getValue();
            final UUID uuid;

            // For compatibility with Delta
            if (uuidObject instanceof String string)
            {
                uuid = UUID.fromString(string);
            }
            // For unpatched servers
            else
            {
                uuid = UUIDUtil.fromIntArray((int[]) uuidObject);
            }

            // Players who are not opted-in should not be tracked.
            if (OmegaTrack.getFlagStorage().getFlags(uuid).isOptedOut())
                return;

            final String world = (String) queryPacket.getNbt().get("Dimension").getValue();
            final Object posRaw = queryPacket.getNbt().get("Pos").getValue();

            // Prevents non-list position coordinates from being indexed (primarily because we don't know what it is)
            if (!(posRaw instanceof ArrayList))
                return;

            // Extracts the position from the NBT
            final List<DoubleTag> doubles = (List<DoubleTag>) queryPacket.getNbt().get("Pos").getValue();

            System.out.println("Sending " + uuid + " to database");
            try
            {
                // Queues the dataset to be sent to the database
                OmegaTrack.getPostgreSQLStorage().queue(new PositionDataset(uuid, world, doubles.get(0).getValue(), doubles.get(2).getValue()));
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
