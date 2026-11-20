package com.github.hhhzzzsss.epsilonbot.modules;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.listeners.DisconnectListener;
import com.github.hhhzzzsss.epsilonbot.listeners.PacketListener;
import com.github.hhhzzzsss.epsilonbot.listeners.TickListener;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.key.Key;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.ClientCommand;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandNode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EntityEvent;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.HandPreference;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.GameEvent;
import org.geysermc.mcprotocollib.protocol.data.game.setting.ChatVisibility;
import org.geysermc.mcprotocollib.protocol.data.game.setting.ParticleStatus;
import org.geysermc.mcprotocollib.protocol.data.game.setting.SkinPart;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundClientInformationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundCommandsPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundEntityEventPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHealthPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundGameEventPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSetCarriedItemPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemPacket;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class StateManager implements PacketListener, TickListener, DisconnectListener {
	private final EpsilonBot bot;
	
	@Getter @Setter public static long commandDelay = 2000;
	private long nextRectifyTime = System.currentTimeMillis();

	@Getter @Setter private boolean isTotalFreedom = false;
	@Getter @Setter private boolean onFreedomServer = false;
	private long lastTimeOnServer = System.currentTimeMillis();
	private long nextServerJoinTime = System.currentTimeMillis();
	
	@Getter @Setter private boolean autoOp = true;
	@Getter private boolean opped = true;

	@Getter @Setter private boolean autoGamemode = true;
	@Getter @Setter private GameMode targetGamemode = GameMode.CREATIVE;
	@Getter private GameMode gamemode = GameMode.CREATIVE;

	@Getter @Setter private boolean autoWorld = true;
	@Getter @Setter private String targetWorld = "minecraft:flatlands";
	@Getter private Key worldName = Key.key("minecraft", "overworld");
	@Getter private int entityId = -1;
	
	@Override
	public void onPacket(Packet packet) {
		if (packet instanceof ClientboundGameEventPacket gameEvent) {
        	if (gameEvent.getNotification() == GameEvent.CHANGE_GAME_MODE) {
        		gamemode = (GameMode) gameEvent.getValue();
        	}
        }
		else if (packet instanceof ClientboundLoginPacket login) {
			entityId = login.getEntityId();
			gamemode = login.getCommonPlayerSpawnInfo().getGameMode();
			worldName = login.getCommonPlayerSpawnInfo().getWorldName();

			// Send client information
			List<SkinPart> skinParts = new ArrayList<>();
			skinParts.add(SkinPart.CAPE);
			skinParts.add(SkinPart.JACKET);
			skinParts.add(SkinPart.LEFT_SLEEVE);
			skinParts.add(SkinPart.RIGHT_SLEEVE);
			skinParts.add(SkinPart.LEFT_PANTS_LEG);
			skinParts.add(SkinPart.RIGHT_PANTS_LEG);
			skinParts.add(SkinPart.HAT);
			bot.sendPacket(new ServerboundClientInformationPacket(
					"en-us", 16, ChatVisibility.FULL, true, skinParts, HandPreference.RIGHT_HAND, false, true, ParticleStatus.MINIMAL));
		}
		else if (packet instanceof ClientboundEntityEventPacket entity) {
			if (entity.getEntityId() == entityId) {
				if (entity.getEvent() == EntityEvent.PLAYER_SET_NO_PERMISSIONS) {
					opped = false;
				}
				else if (entity.getEvent() == EntityEvent.PLAYER_SET_OWNER) {
					opped = true;
				}
			}
		}
		else if (packet instanceof ClientboundRespawnPacket respawn) {
			gamemode = respawn.getCommonPlayerSpawnInfo().getGameMode();
			worldName = respawn.getCommonPlayerSpawnInfo().getWorldName();
		}
		else if (packet instanceof ClientboundCommandsPacket commands) {
			boolean wasOnFreedomServer = onFreedomServer;
			onFreedomServer = false;
			for (CommandNode node : commands.getNodes()) {
				if (node.getName() == null) {
					continue;
				}

				if (node.getName().equals("2fa")) {
					isTotalFreedom = true;
				} else if (node.getName().equals("tfm")) {
					onFreedomServer = true;
				}
			}

			if (!isTotalFreedom) onFreedomServer = true;

			if (wasOnFreedomServer && !onFreedomServer) {
				lastTimeOnServer = System.currentTimeMillis();
			}
		} else if (packet instanceof ClientboundSetHealthPacket) {
			if (((ClientboundSetHealthPacket) packet).getHealth() <= 0.0) {
				bot.sendPacket(new ServerboundClientCommandPacket(ClientCommand.PERFORM_RESPAWN));
			}
		}
	}
	
	@Override
	public void onDisconnected(DisconnectedEvent event) {
		opped = true;
		gamemode = GameMode.CREATIVE;
		onFreedomServer = false;
	}

	@Override
	public void onTick() {
		if (System.currentTimeMillis() >= nextRectifyTime) {
			rectify();
		}
	}
	
	public void rectify() {
		long currentTime = System.currentTimeMillis();
		nextRectifyTime = currentTime;
		if (!onFreedomServer && currentTime >= nextServerJoinTime) {
			bot.sendPacket(new ServerboundSetCarriedItemPacket(0));
			bot.sendPacket(new ServerboundUseItemPacket(Hand.MAIN_HAND, 0, 0F, 0F));
			if (currentTime-lastTimeOnServer > 10*1000) {
				nextServerJoinTime = currentTime + 10*1000;
			}
			if (currentTime-lastTimeOnServer > 60*1000) {
				nextServerJoinTime = currentTime + 60*1000;
			}
			if (currentTime-lastTimeOnServer > 5*60*1000) {
				nextServerJoinTime = currentTime + 5*60*1000;
			}
			nextRectifyTime += commandDelay;
			return;
		}
		if (autoOp && !opped) {
			if (isTotalFreedom) bot.sendCommand("/hack");
			nextRectifyTime += commandDelay;
		}
		if (autoGamemode && gamemode != targetGamemode) {
			if (targetGamemode == GameMode.SURVIVAL) {
				bot.sendCommand("/gms");
			}
			else if (targetGamemode == GameMode.CREATIVE) {
				bot.sendCommand("/gmc");
			}
			else if (targetGamemode == GameMode.ADVENTURE) {
				bot.sendCommand("/gma");
			}
			// Spectator not supported
			nextRectifyTime += commandDelay;
		}
		if (isTotalFreedom && autoWorld && !worldName.asString().equals(targetWorld)) {
			bot.sendCommand("/world " + targetWorld.replace("minecraft:", ""));
		}
	}
}
