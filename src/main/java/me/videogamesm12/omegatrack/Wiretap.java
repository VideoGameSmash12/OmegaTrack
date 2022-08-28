package me.videogamesm12.omegatrack;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerInfoPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddPlayerPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundTagQueryPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundEntityTagQuery;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.Getter;
import lombok.Setter;
import me.videogamesm12.omegatrack.util.UUIDUtil;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * <h1>Wiretap</h1>
 * <p>Listens for all entity spawn and player join events to determine the entity ID of players, primarily to optimize
 *    the process of tracking players.</p>
 *  --
 *  TODO:   Figure out a way to get the newest entity ID (probably using Paintings) and utilize it to query entity IDs
 *          for players that join to further improve the efficiency of the Tracker using the power of math.
 */
public class Wiretap extends SessionAdapter
{
    @Getter
    private final Map<UUID, Integer> uuids = new HashMap<>();
    //--
    private ScheduledExecutorService outBrute = Executors.newScheduledThreadPool(1);
    private Queue<Packet> outBruteQueue = new ConcurrentLinkedQueue<>();
    //--
    private ScheduledExecutorService out = Executors.newScheduledThreadPool(1);
    private Queue<Packet> outQueue = new ConcurrentLinkedQueue<>();
    //--
    @Getter
    @Setter
    private int currentId = 0;
    //--
    private Timer bruteTimer = new Timer();

    public Wiretap()
    {
        EpsilonBot.INSTANCE.getSession().addListener(this);

        out.scheduleAtFixedRate(() -> {
            for (int i = 0; i < outQueue.size(); i++)
            {
                if (!EpsilonBot.INSTANCE.getStateManager().isOnFreedomServer())
                    break;

                EpsilonBot.INSTANCE.sendPacket(outQueue.poll());
            }
        }, 0, 100, TimeUnit.MILLISECONDS);

        // Slightly throttled compared to the regular `out`, but this is to avoid spamfucking the server with possbly
        //  thousands of requests per second.
        outBrute.scheduleAtFixedRate(() -> {
            for (int i = 0; i < outBruteQueue.size(); i++)
            {
                if (!EpsilonBot.INSTANCE.getStateManager().isOnFreedomServer())
                    break;

                EpsilonBot.INSTANCE.sendPacket(outBruteQueue.poll());
            }
        }, 0, 333, TimeUnit.MILLISECONDS);

        // Sets up the bruteforcer.
        resetBruteforcer(0);
    }

    public void stop()
    {
        try
        {
            out.awaitTermination(5000, TimeUnit.MILLISECONDS);
            outBrute.awaitTermination(5000, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Processes packets with the end goal being to keep track of who is online and who is not.
     * @param session   Session
     * @param packet    Packet
     */
    @Override
    public void packetReceived(Session session, Packet packet)
    {
        // Self-registration as a player
        if (packet instanceof ClientboundLoginPacket login)
        {
            // We logged in, now what's our ID?
            int myId = login.getEntityId();

            // Cool, storing that in the list.
            link(myId, EpsilonBot.INSTANCE.getUuid());
        }
        // Response to queries we made previously
        else if (packet instanceof ClientboundTagQueryPacket tagQuery)
        {
            // We just link UUIDs with entity IDs here.
            int id = tagQuery.getTransactionId();

            // ID is already linked, so let's not even bother
            if (isLinked(id))
                return;

            int[] rawUuid = (int[]) tagQuery.getNbt().get("UUID").getValue();
            UUID uuid = UUIDUtil.fromIntArray(rawUuid);

            // Refuse to link entities that are already linked somewhere else or have opted out of being tracked
            if (!isLinked(uuid))
                link(id, uuid);
        }
        // In case someone teleports to the bot and wasn't already on the list...
        else if (packet instanceof ClientboundAddPlayerPacket playerAdd)
        {
            int id = playerAdd.getEntityId();
            UUID uuid = playerAdd.getUuid();

            link(id, uuid);
        }
        // Unlink players that leave the server
        else if (packet instanceof ClientboundPlayerInfoPacket playerInfo)
        {
            if (playerInfo.getAction() == PlayerListEntryAction.REMOVE_PLAYER)
            {
                // Get the UUID of the player that left
                UUID uuid = playerInfo.getEntries()[0].getProfile().getId();

                if (!isLinked(uuid))
                    return;

                int numeric = uuids.get(uuid);

                // Unlinks the player - Note that this may cause issues with admins that vanish, which is why we grabbed
                //                      the entity ID beforehand so that we can simply re-register them in case they are
                //                      on the server still
                unlink(uuid);

                // Circumvents vanished admins being present still by simply checking if the player is actually still on
                //  the server.
                doWiretapQuery(numeric);
            }
        }
    }

    /**
     * Send a request to the server for player data that will not get processed by the Tracker.
     * @param id    int
     */
    public void doWiretapQuery(int id)
    {
        outQueue.add(new ServerboundEntityTagQuery(id, id));
    }

    /**
     * Send a bruteforce request to the server for player data that will not get processed by the Tracker.
     * @param id    int
     */
    public void doWiretapBruteQuery(int id)
    {
        outBruteQueue.add(new ServerboundEntityTagQuery(id, id));
    }

    /**
     * Resets the bruteforcer
     */
    public void resetBruteforcer(int offset)
    {
        // Cancel any existing operations and kills the original Timer as we can't schedule tasks on timers that had
        //  `cancel()` called already
        if (bruteTimer != null)
        {
            bruteTimer.cancel();
            bruteTimer = null;
        }

        // Resets things back from square one
        setCurrentId(offset);

        // Sets up a new Timer
        bruteTimer = new Timer();
        bruteTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                // If we aren't on the server, let's go ahead and reset everything to 0 just in case the server
                //  restarted. Note that getting kicked for any reason would also result in this behavior.
                if (!EpsilonBot.INSTANCE.getStateManager().isOnFreedomServer())
                {
                    currentId = 0;
                    return;
                }

                // Wraps shit back
                if (currentId == Integer.MAX_VALUE)
                    currentId = 0;
                else
                    currentId++;

                // Has the account been linked already or opted out of tracking?
                if (!isLinked(currentId))
                    doWiretapBruteQuery(currentId);
            }
        }, 0, 250);
    }

    public boolean isLinked(UUID uuid)
    {
        return uuids.containsKey(uuid);
    }

    public boolean isLinked(int id)
    {
        return uuids.containsValue(id);
    }

    public UUID getById(int id)
    {
        return uuids.entrySet().stream().filter((entry) -> entry.getValue().equals(id)).findAny().map(Map.Entry::getKey).orElse(null);
    }

    /**
     * Links a player's UUID with the equivalent numerical entity ID.
     * @param id    int
     * @param uuid  UUID
     */
    public void link(int id, UUID uuid)
    {
        if (OmegaTrack.FLAGS.getFlags(uuid).isOptedOut())
            return;

        uuids.put(uuid, id);
    }

    public void unlink(UUID uuid)
    {
        uuids.remove(uuid);
    }
}
