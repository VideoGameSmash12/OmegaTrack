package me.videogamesm12.omegatrack;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import me.videogamesm12.omegatrack.data.PositionDataset;
import me.videogamesm12.omegatrack.tasks.tracker.TrackerTask;
import me.videogamesm12.omegatrack.util.UUIDUtil;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundTagQueryPacket;

import java.util.*;

/**
 * <h1>Tracker</h1>
 * <p>The part of OmegaTrack that tracks known players.</p>
 */
public class Tracker extends SessionAdapter
{
    private final OmegaTrack omegaTrack;
    private final EpsilonBot epsilonBot;
    private final TrackerTask trackerTask;
    private final Timer timer;

    public Tracker(final OmegaTrack omegaTrack)
    {
        this.omegaTrack = omegaTrack;
        this.timer = this.omegaTrack.timer;
        this.epsilonBot = this.omegaTrack.epsilonBot;
        this.trackerTask = new TrackerTask(this.omegaTrack);
    }

    public void start()
    {
        this.epsilonBot.getSession().addListener(this);
        this.timer.scheduleAtFixedRate(this.trackerTask, 0, 3000);
    }

    @Override
    public void packetReceived(Session session, Packet packet)
    {
        if (packet instanceof ClientboundTagQueryPacket queryPacket
                && queryPacket.getNbt() != null
                && queryPacket.getNbt().containsKey("EnderItems"))
        {
            // Gets the user's UUID
            final Object uuidObject = queryPacket.getNbt().get("UUID");
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
            if (this.omegaTrack.flags.getFlags(uuid).isOptedOut())
                return;

            final String world = queryPacket.getNbt().getString("Dimension");

            // Extracts the position from the NBT
            final List<Double> pos = queryPacket.getNbt().getList("Pos", NbtType.DOUBLE);

            // Refuse to record empty data
            if (pos.isEmpty())
            {
                return;
            }

            // Record the data
            EpsilonBot.getLogger().debug("Queueing data for {}", uuid);
            try
            {
                // Queues the dataset to be sent to the database
                this.omegaTrack.storage.queue(new PositionDataset(uuid, world, pos.get(0), pos.get(2)));
            }
            catch (Throwable ex)
            {
                EpsilonBot.getLogger().error("Failed to queue data for user {}", uuid, ex);
            }
        }
    }

    public void stop()
    {
        this.trackerTask.cancel();
    }
}
