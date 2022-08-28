package com.github.hhhzzzsss.epsilonbot.modules;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.build.BuilderSession;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import com.github.hhhzzzsss.epsilonbot.listeners.DisconnectListener;
import com.github.hhhzzzsss.epsilonbot.listeners.PacketListener;
import com.github.hhhzzzsss.epsilonbot.listeners.TickListener;
import com.github.hhhzzzsss.epsilonbot.mapart.MapartBuildState;
import com.github.hhhzzzsss.epsilonbot.mapart.MapartBuilderSession;
import com.github.hhhzzzsss.epsilonbot.mapart.MapartCheckerThread;
import com.github.hhhzzzsss.epsilonbot.util.ChatUtils;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSetTimePacket;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.text.Component;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

@RequiredArgsConstructor
public class BuildHandler implements TickListener, PacketListener, DisconnectListener {
    public static final int ACTIONS_PER_TIME_PACKET = 20;

    public final EpsilonBot bot;
    @Getter @Setter BuilderSession builderSession = null;

    private int actionQuota = 0;

    public static final long BUILD_CHECK_DELAY = 2000;
    private long nextBuildCheckTime = System.currentTimeMillis();

    public static final int MAX_MAPART_QUEUE = 5;
    private Queue<MapartCheckerThread> unloadedMapartQueue = new LinkedBlockingQueue<>();
    @Getter private Queue<MapartCheckerThread> mapartQueue = new LinkedBlockingQueue<>();

    @Override
    public void onTick() {
        if (!bot.getStateManager().isOnFreedomServer()) {
            builderSession = null;
            return;
        }

        if (builderSession == null) {
            long currentTime = System.currentTimeMillis();
            if (currentTime > nextBuildCheckTime) {
                checkMapart();
                nextBuildCheckTime = currentTime + BUILD_CHECK_DELAY;
            }
        } else {
            if (actionQuota > 0) {
                builderSession.onAction();
                actionQuota--;
                if (builderSession.isStopped()) {
                    builderSession = null;
                }
            }
        }

        checkUnloadedMapartQueue();
    }

    @Override
    public void onPacket(Packet packet) {
        if (!bot.getStateManager().isOnFreedomServer()) {
            builderSession = null;
            return;
        }

        if (packet instanceof ClientboundSetTimePacket) {
            actionQuota = ACTIONS_PER_TIME_PACKET;
            if (builderSession != null) {
                builderSession.onTimePacket();
                if (builderSession.isStopped()) {
                    builderSession = null;
                }
            }
        } else if (packet instanceof ClientboundChatPacket) {
            ClientboundChatPacket t_packet = (ClientboundChatPacket) packet;
            Component message = t_packet.getMessage();
            String strMessage = ChatUtils.getFullText(message);
            if (builderSession != null) {
                builderSession.onChat(strMessage);
                if (builderSession.isStopped()) {
                    builderSession = null;
                }
            }
        }
    }

    @Override
    public void onDisconnected(DisconnectedEvent event) {
        builderSession = null;
    }

    private void checkMapart() {
        if (MapartBuildState.buildStateExists()) {
            try {
                setBuilderSession(new MapartBuilderSession(bot, MapartBuildState.loadBuildState()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (!mapartQueue.isEmpty()) {
            try {
                setBuilderSession(mapartQueue.poll().getBuilderSession());
                bot.sendChat("Loading queued mapart for " + mapartQueue.peek().getUrl().toString());
            } catch (IOException e) {
                bot.sendChat("Exception occured while loading queued mapart: " + e.getMessage());
            }
        }
    }

    private void checkUnloadedMapartQueue() {
        while (!unloadedMapartQueue.isEmpty() && !unloadedMapartQueue.peek().isAlive()) {
            MapartCheckerThread mct = unloadedMapartQueue.poll();
            if (mct.getException() != null) {
                bot.sendChat("Error while loading mapart: " + mct.getException().getMessage());
            } else {
                mapartQueue.add(mct);
                bot.sendChat("Queued mapart");
            }
        }
    }

    public void queueMapart(MapartCheckerThread mct) throws CommandException {
        if (mapartQueue.size() + unloadedMapartQueue.size() > MAX_MAPART_QUEUE) {
            throw new CommandException("The mapart queue is full");
        }
        mct.start();
        unloadedMapartQueue.add(mct);
    }
}
