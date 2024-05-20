package me.videogamesm12.omegatrack;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction;
import com.github.steveice10.mc.protocol.data.game.entity.EntityEvent;
import com.github.steveice10.mc.protocol.data.game.entity.type.EntityType;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerInfoRemovePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerInfoUpdatePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundEntityEventPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundRemoveEntitiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundTagQueryPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundEntityTagQuery;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.Getter;
import lombok.Setter;
import me.videogamesm12.omegatrack.storage.OTConfig;
import me.videogamesm12.omegatrack.storage.OTFlags;
import me.videogamesm12.omegatrack.tasks.wiretap.BackwardsTimerTask;
import me.videogamesm12.omegatrack.tasks.wiretap.TraditionalBackwardsTimerTask;
import me.videogamesm12.omegatrack.tasks.wiretap.TraditionalTimerTask;
import me.videogamesm12.omegatrack.tasks.wiretap.PacketSenderTask;
import me.videogamesm12.omegatrack.util.UUIDUtil;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * <h1>Wiretap</h1>
 * <p>Listens for all entity spawn and player join events to determine the entity ID of players, primarily to optimize
 *    the process of tracking players.</p>
 */
public class Wiretap extends SessionAdapter
{
    private final OmegaTrack omegaTrack;
    @Getter
    private final EpsilonBot epsilonBot;
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

    public void start()
    {
        this.epsilonBot.getSession().addListener(this);

        // This gets sent regardless of Delta communication because we use it to send special messages.
        this.timer.scheduleAtFixedRate(this.regularTimerTask, 0, 100);

        // Sets up entity brute-forcing if we are not using Delta communication
        if (!OTConfig.INSTANCE.getGeneral().isUsingDeltaCommunication())
        {
            // Slightly throttled compared to the regular `out`, but this is to avoid spamming the server with possibly
            //  thousands of requests per second.
            this.timer.scheduleAtFixedRate(this.bruteForceTimerTask, 0, 333);
            // Sets up the bruteforcer.
            resetBruteforcer(0);
        }
    }

    public Wiretap(final OmegaTrack omegaTrack)
    {
        this.omegaTrack = omegaTrack;
        this.timer = this.omegaTrack.timer;
        this.epsilonBot = this.omegaTrack.epsilonBot;
        this.regularTimerTask = new PacketSenderTask(this,this.outQueue);
        this.bruteForceTimerTask = new PacketSenderTask(this, this.outBruteQueue);
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
        // Delta communication specific
        if (OTConfig.INSTANCE.getGeneral().isUsingDeltaCommunication())
        {
            // Process the special response containing entity IDs of every online player.
            if (packet instanceof ClientboundTagQueryPacket tagQuery && tagQuery.getNbt().contains("DeltaEntityIDs"))
            {
                final Map<String, Tag> response = tagQuery.getNbt().getValue();
                CompoundTag uuidSet = (CompoundTag) response.get("DeltaEntityIDs");
                uuidSet.getValue().forEach((uuid, tag) ->
                {
                    if (tag instanceof IntTag id && !isLinked(UUID.fromString(uuid)))
                    {
                        // Link the entity ID with the UUID
                        link(id.getValue(), UUID.fromString(uuid));
                    }
                    else
                    {
                        System.out.println(tag.getValue());
                    }
                });
            }
            // Perform certain actions when a player leaves the server
            else if (packet instanceof ClientboundPlayerInfoRemovePacket remove)
            {
                // For every player that left...
                remove.getProfileIds().forEach(entry ->
                {
                    if (!isLinked(entry))
                        return;

                    // Unlinks the player - Note that this may cause issues with admins that vanish, which is why we grabbed
                    //                      the entity ID beforehand so that we can simply re-register them in case they are
                    //                      on the server still
                    unlink(entry);

                    // Circumvents vanished admins being present still
                    outQueue.add(new ServerboundEntityTagQuery(20101111, 20140324));
                });
            }
            // Perform certain actions when a player joins the server
            else if (packet instanceof ClientboundPlayerInfoUpdatePacket playerInfo)
            {
                // Begin tracking the players and notify them of such if they opted in
                if (playerInfo.getActions().contains(PlayerListEntryAction.ADD_PLAYER))
                {
                    Arrays.stream(playerInfo.getEntries()).forEach(entry ->
                    {
                        final UUID uuid = entry.getProfile().getId();
                        final OTFlags.UserFlags flags = this.omegaTrack.flags.getFlags(uuid);

                        // If they are opted-in...
                        if (!flags.isOptedOut())
                        {
                            // Remind them that this is the case
                            if (!flags.isSupposedToShutUp())
                            {
                                this.epsilonBot.sendCommand("/etell " + uuid + " Just a reminder: you have opted in to "
                                        + "being tracked by OmegaTrack. If you wish for this to stop, use the command !optout. "
                                        + "To disable messages like these, use the !stfu command.");
                            }

                            outQueue.add(new ServerboundEntityTagQuery(20101111, 20140324));
                        }
                    });
                }
            }

            return;
        }

        // Self-registration as a player
        if (packet instanceof ClientboundLoginPacket login)
        {
            // We logged in, now what's our ID?
            int myId = login.getEntityId();

            // Cool, storing that in the list.
            link(myId, this.epsilonBot.getUuid());

            // Is it the newest ID we know?
            if (myId > maxId)
            {
                maxId = myId;
                resetBackwardsBruteforcer(myId);
            }
        }
        // Response to queries we made previously
        else if (packet instanceof ClientboundTagQueryPacket tagQuery && tagQuery.getNbt().contains("EnderItems"))
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
                maxId = maximum;
                resetBackwardsBruteforcer(maximum);
            }
        }
        // If an entity spawns in and its ID is higher than our known maximum ID, reset the backwards bruteforcer. We
        //  use this to find out what the latest entity ID is when a player joins so that we can index them faster.
        else if (packet instanceof ClientboundAddEntityPacket addEntity)
        {
            if (addEntity.getType() == EntityType.PLAYER)
            {
                // Link ourselves because we know our current entity ID
                if (addEntity.getUuid().equals(this.epsilonBot.getUuid()))
                {
                    link(addEntity.getEntityId(), this.epsilonBot.getUuid());
                    resetBackwardsBruteforcer(addEntity.getEntityId());
                }
                // Manually link the entity ID with the UUID, bypassing brute-forcing attempts entirely
                else
                {
                    int id = addEntity.getEntityId();
                    UUID uuid = addEntity.getUuid();

                    link(id, uuid);

                    // Reset the backwards bruteforcer if the max ID is less than the ID of the player that teleported to you
                    if (maxId < id)
                    {
                        maxId = id;
                        resetBackwardsBruteforcer(id);
                    }
                }
            }
            else if ((!OTConfig.INSTANCE.getWiretap().isAnythingButSquidsIgnoredOnSpawn() || addEntity.getType() == EntityType.SQUID)
                    && addEntity.getEntityId() > maxId)
            {
                maxId = addEntity.getEntityId();
                resetBackwardsBruteforcer(addEntity.getEntityId());
            }
        }
        // If a mob dies and its ID is higher than our known maximum ID, reset the backwards bruteforcer. We used to use
        //  this to find out what the latest entity ID is, but it now serves as a fallback.
        else if (packet instanceof ClientboundEntityEventPacket entityEvent)
        {
            if (entityEvent.getEvent() == EntityEvent.LIVING_DEATH && entityEvent.getEntityId() > maxId)
            {
                maxId = entityEvent.getEntityId();
                resetBackwardsBruteforcer(entityEvent.getEntityId());
            }
        }
        // Perform certain actions when a player joins or leaves the server
        else if (packet instanceof ClientboundPlayerInfoUpdatePacket playerInfo)
        {
            // Begin tracking the players and notify them of such if they opted in
            if (playerInfo.getActions().contains(PlayerListEntryAction.ADD_PLAYER))
            {
                Arrays.stream(playerInfo.getEntries()).forEach(entry ->
                {
                    final UUID uuid = entry.getProfile().getId();
                    final OTFlags.UserFlags flags = this.omegaTrack.flags.getFlags(uuid);

                    // If they are opted-in...
                    if (!flags.isOptedOut())
                    {
                        // Remind them that this is the case
                        if (!flags.isSupposedToShutUp())
                        {
                            this.epsilonBot.sendCommand("/etell " + uuid + " Just a reminder: you have opted in to "
                                    + "being tracked by OmegaTrack. If you wish for this to stop, use the command !optout. "
                                    + "To disable messages like these, use the !stfu command.");
                        }

                        // Attempt to find the latest entity ID by spawning in a Pig
                        this.epsilonBot.sendCommand("/spawnmob squid 1");
                    }
                });
            }
        }
        // Perform certain actions when a player leaves the server
        else if (packet instanceof ClientboundPlayerInfoRemovePacket remove)
        {
            // For every player that left...
            remove.getProfileIds().forEach(entry ->
            {
                if (!isLinked(entry))
                    return;

                int numeric = uuids.get(entry);

                // Unlinks the player - Note that this may cause issues with admins that vanish, which is why we grabbed
                //                      the entity ID beforehand so that we can simply re-register them in case they are
                //                      on the server still
                unlink(entry);

                // Circumvents vanished admins being present still by simply checking if the player is actually still on
                //  the server.
                doWiretapQuery(numeric);
            });
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

        if (this.backwardsTimerTask != null)
        {
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
        if (this.omegaTrack.flags.getFlags(uuid).isOptedOut())
            return;

        uuids.put(uuid, id);
    }

    public void unlink(UUID uuid)
    {
        uuids.remove(uuid);
    }
}
