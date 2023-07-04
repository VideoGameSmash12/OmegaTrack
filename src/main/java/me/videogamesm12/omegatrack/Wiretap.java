package me.videogamesm12.omegatrack;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction;
import com.github.steveice10.mc.protocol.data.game.entity.EntityEvent;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerInfoPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundEntityEventPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundRemoveEntitiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddPlayerPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundTagQueryPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundEntityTagQuery;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.Getter;
import lombok.Setter;
import me.videogamesm12.omegatrack.storage.OTFlags;
import me.videogamesm12.omegatrack.tasks.wiretap.BackwardsTimerTask;
import me.videogamesm12.omegatrack.tasks.wiretap.TraditionalBackwardsTimerTask;
import me.videogamesm12.omegatrack.tasks.wiretap.TraditionalTimerTask;
import me.videogamesm12.omegatrack.tasks.wiretap.WiretapTask;
import me.videogamesm12.omegatrack.util.UUIDUtil;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
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
    private Queue<Packet> outBruteQueue = new ConcurrentLinkedQueue<>();
    private Queue<Packet> outQueue = new ConcurrentLinkedQueue<>();
    //--
    @Getter
    @Setter
    public int currentTraditionalId = 0;
    @Getter
    @Setter
    public int currentTraditionalBackwardsId = 0;
    @Getter
    @Setter
    public int currentBackwardsId = 0;
    //--
    @Getter
    private int maxId = 0;
    //--
    private final Timer timer;
    private final TimerTask regularTimerTask;
    private final TimerTask bruteForceTimerTask;
    private TimerTask backwardsTimerTask;
    private TimerTask traditionalBackwardsTimerTask;
    private TimerTask traditionalTimerTask;

    public Wiretap(final Timer timer)
    {
        this.timer = timer;
        EpsilonBot.INSTANCE.getSession().addListener(this);

        this.regularTimerTask = new WiretapTask(this.outQueue);
        timer.scheduleAtFixedRate(this.regularTimerTask, 0, 100);
        // Slightly throttled compared to the regular `out`, but this is to avoid spamming the server with possibly
        //  thousands of requests per second.
        this.bruteForceTimerTask = new WiretapTask(this.outBruteQueue);
        timer.scheduleAtFixedRate(this.bruteForceTimerTask, 0, 333);

        // Sets up the bruteforcer.
        resetBruteforcer(0);
    }

    public void stop()
    {
        this.regularTimerTask.cancel();
        this.bruteForceTimerTask.cancel();

        if (this.backwardsTimerTask != null)
        {
            this.backwardsTimerTask.cancel();
        }

        if (this.traditionalBackwardsTimerTask != null)
        {
            this.traditionalBackwardsTimerTask.cancel();
        }

        if (this.traditionalTimerTask != null)
        {
            this.traditionalTimerTask.cancel();
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

            // Is it the newest ID we know?
            if (myId > maxId)
            {
                maxId = myId;
                resetBackwardsBruteforcer(myId);
            }
        }
        // Response to queries we made previously
        else if (packet instanceof ClientboundTagQueryPacket tagQuery)
        {
            // We just link UUIDs with entity IDs here.
            int id = tagQuery.getTransactionId();

            // ID is already linked, so let's not even bother
            if (isLinked(id))
                return;

            final Object uuidObject = tagQuery.getNbt().get("UUID").getValue();
            UUID uuid;
            if (uuidObject instanceof String string)
            {
                uuid = UUID.fromString(string);
            }
            else
            {
                uuid = UUIDUtil.fromIntArray((int[]) uuidObject);
            }

            // Refuse to link entities that are already linked somewhere else or have opted out of being tracked
            if (!isLinked(uuid))
            {
                link(id, uuid);
            }
        }
        // If a mob despawns and its ID is higher than our known maximum ID, reset the backwards bruteforcer. We use
        //  this find out what the latest entity ID is when a player joins so that we can index them faster.
        else if (packet instanceof ClientboundRemoveEntitiesPacket entityRemove)
        {
            int maximum = 0;

            for (int id : entityRemove.getEntityIds())
            {
                if (id > maximum)
                {
                    maximum = id;
                }
            }

            if (maximum > maxId)
            {
                resetBackwardsBruteforcer(maximum);
            }
        }
        // If a mob dies and its ID is higher than our known maximum ID, reset the backwards bruteforcer. We use this to
        //  find out what the latest entity ID is when a player joins so that we can index them faster.
        else if (packet instanceof ClientboundEntityEventPacket entityEvent)
        {
            if (entityEvent.getStatus() == EntityEvent.LIVING_DEATH && entityEvent.getEntityId() > maxId)
            {
                resetBackwardsBruteforcer(entityEvent.getEntityId());
            }
        }
        /*// If a pig spawns in, assume we spawned it in and reset the backwards bruteforcer. We use this to find out what
        //  the latest entity ID is when a player joins so that we can index them faster.
        else if (packet instanceof ClientboundAddEntityPacket entityAdd)
        {
            if (entityAdd.getType() == EntityType.PIG && entityAdd.getEntityId() > maxId)
            {
                resetBackwardsBruteforcer(entityAdd.getEntityId());
            }
        }*/
        // Link players manually and if their entity ID is larger than the largest known player entity ID, reset the
        //  backwards bruteforcer.
        else if (packet instanceof ClientboundAddPlayerPacket playerAdd)
        {
            // Link ourselves because we know our current entity ID
            if (playerAdd.getUuid().equals(EpsilonBot.INSTANCE.getUuid()))
            {
                link(playerAdd.getEntityId(), EpsilonBot.INSTANCE.getUuid());
                resetBackwardsBruteforcer(playerAdd.getEntityId());
            }
            // Manually link the entity ID with the UUID, bypassing brute-forcing attempts entirely
            else
            {
                int id = playerAdd.getEntityId();
                UUID uuid = playerAdd.getUuid();

                link(id, uuid);

                // Reset the backwards bruteforcer if the max ID is less than the ID of the player that teleported to you
                if (maxId < id)
                {
                    maxId = id;
                    resetBackwardsBruteforcer(id);
                }
            }
        }
        // Perform certain actions when a player joins or leaves the server
        else if (packet instanceof ClientboundPlayerInfoPacket playerInfo)
        {
            // Begin tracking the player and notify them of such if they opted in
            if (playerInfo.getAction() == PlayerListEntryAction.ADD_PLAYER)
            {
                final UUID uuid = playerInfo.getEntries()[0].getProfile().getId();
                final OTFlags.UserFlags flags = OmegaTrack.FLAGS.getFlags(uuid);

                // If they are opted-in...
                if (!flags.isOptedOut())
                {
                    // Remind them that this is the case
                    if (!flags.isSupposedToShutUp())
                    {
                        EpsilonBot.INSTANCE.sendCommand("/etell " + uuid + " Just a reminder: you have opted in to "
                                + "being tracked by OmegaTrack. If you wish for this to stop, use the command !optout. "
                                + "To disable messages like these, use the !stfu command.");
                    }

                    // Attempt to find the latest entity ID by spawning in a Pig
                    EpsilonBot.INSTANCE.sendCommand("/spawnmob pig 1");
                }
            }
            // Unlink players that leave the server
            else if (playerInfo.getAction() == PlayerListEntryAction.REMOVE_PLAYER)
            {
                // Get the UUID of the player that left
                final UUID uuid = playerInfo.getEntries()[0].getProfile().getId();

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
     * Resets the backwards bruteforcer. This is separate from the traditional brute-forcing methods because the way it
     *  gets IDs is by listening for relevant entity IDs.
     */
    public void resetBackwardsBruteforcer(int myId)
    {
        setCurrentBackwardsId(myId);

        if (this.backwardsTimerTask != null) {
            this.backwardsTimerTask.cancel();
        }

        // Backwards bruteforce
        this.backwardsTimerTask = new BackwardsTimerTask(this);
        this.timer.scheduleAtFixedRate(this.backwardsTimerTask, 0, 100);
    }

    /**
     * Resets the traditional bruteforcers.
     */
    public void resetBruteforcer(int offset)
    {
        // Cancel any existing operations and kills the original Timer as we can't schedule tasks on timers that had
        //  `cancel()` called already
        if (this.traditionalBackwardsTimerTask != null)
        {
            this.traditionalBackwardsTimerTask.cancel();
        }

        if (this.traditionalTimerTask != null)
        {
            this.traditionalTimerTask.cancel();
        }

        // Resets things back from square one
        setCurrentTraditionalId(offset);
        setCurrentTraditionalBackwardsId(offset);

        // Traditional forwards brute-forcing - This takes the offset and starts brute-forcing entity IDs starting at
        //  that number. On start-up by default it starts at 0 just in case the server restarted.
        this.traditionalTimerTask = new TraditionalTimerTask(this);
        this.timer.schedule(this.traditionalTimerTask, 0, 100);

        // Traditional backwards brute-forcing - Usually on start-up this doesn't do anything but if the offset is set
        //  via the command, this takes the offset and then starts brute-forcing IDs in the opposite direction.
        this.traditionalBackwardsTimerTask = new TraditionalBackwardsTimerTask(this);
        this.timer.schedule(this.traditionalBackwardsTimerTask, 0, 100);
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
