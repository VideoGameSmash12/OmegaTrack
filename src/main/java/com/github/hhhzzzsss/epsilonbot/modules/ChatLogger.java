package com.github.hhhzzzsss.epsilonbot.modules;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.Logger;
import com.github.hhhzzzsss.epsilonbot.listeners.DisconnectListener;
import com.github.hhhzzzsss.epsilonbot.listeners.PacketListener;
import com.github.hhhzzzsss.epsilonbot.util.ChatUtils;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundDisguisedChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;

public class ChatLogger implements PacketListener, DisconnectListener {
	private final EpsilonBot bot;
	@Getter @Setter private boolean writeToFile;
	
	public ChatLogger(EpsilonBot bot, boolean writeToFile) {
		this.bot = bot;
		this.writeToFile = writeToFile;
	}

	@Override
	public void onPacket(Packet packet) {
		if (packet instanceof ClientboundLoginPacket) {
			log(String.format("Successfully logged in to %s:%d", bot.getHost(), bot.getPort()));
		}
		else if (packet instanceof ClientboundSystemChatPacket) {
			ClientboundSystemChatPacket t_packet = (ClientboundSystemChatPacket) packet;
			String fullText = ChatUtils.getFullText(t_packet.getContent());
			if (fullText.equals("") || fullText.startsWith("Command set: ")) {
				return;
			}
			log(fullText);
		}
		else if (packet instanceof ClientboundDisguisedChatPacket) {
			ClientboundDisguisedChatPacket t_packet = (ClientboundDisguisedChatPacket) packet;
			log(ChatUtils.getFullText(t_packet.getMessage()));
		}
		else if (packet instanceof ClientboundPlayerChatPacket) {
			ClientboundPlayerChatPacket t_packet = (ClientboundPlayerChatPacket) packet;
			if (t_packet.getUnsignedContent() != null) {
				log(ChatUtils.getFullText(t_packet.getUnsignedContent()));
			} else {
				log("<" + ChatUtils.getFullText(t_packet.getName()) + "> " + t_packet.getContent());
			}
		}
	}

	@Override
	public void onDisconnected(DisconnectedEvent packet) {
		log("Disconnected: " + packet.getReason());
	}
	
	public void log(String fullText) {
		System.out.println(fullText);
		if (writeToFile) Logger.log(fullText);
	}
}
