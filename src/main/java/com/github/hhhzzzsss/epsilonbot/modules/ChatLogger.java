package com.github.hhhzzzsss.epsilonbot.modules;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.listeners.DisconnectListener;
import com.github.hhhzzzsss.epsilonbot.listeners.PacketListener;
import com.github.hhhzzzsss.epsilonbot.util.ChatUtils;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundDisguisedChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;

public class ChatLogger implements PacketListener, DisconnectListener
{
	private final EpsilonBot bot;

	public ChatLogger(EpsilonBot bot)
	{
		this.bot = bot;
	}

	@Override
	public void onPacket(Packet packet)
	{
		if (packet instanceof ClientboundLoginPacket)
		{
			EpsilonBot.getLogger().info("Successfully logged into {}:{}", bot.getHost(), bot.getPort());
		}
		else if (packet instanceof ClientboundSystemChatPacket system)
		{
            String fullText = ChatUtils.getFullText(system.getContent());
			if (fullText.isEmpty() || fullText.startsWith("Command set: "))
			{
				return;
			}
			EpsilonBot.getLogger().info(fullText);
		}
		else if (packet instanceof ClientboundDisguisedChatPacket disguised)
		{
            EpsilonBot.getLogger().info(ChatUtils.getFullText(disguised.getMessage()));
		}
		else if (packet instanceof ClientboundPlayerChatPacket chat)
		{
            if (chat.getUnsignedContent() != null)
			{
				EpsilonBot.getLogger().info(ChatUtils.getFullText(chat.getUnsignedContent()));
			} else
			{
                EpsilonBot.getLogger().info("<{}> {}", ChatUtils.getFullText(chat.getName()), chat.getContent());
			}
		}
	}

	@Override
	public void onDisconnected(DisconnectedEvent packet)
	{
		EpsilonBot.getLogger().info("Disconnected: {}", packet.getReason());
		if (packet.getCause() != null)
		{
			EpsilonBot.getLogger().error("Uh oh, exception led to us disconnecting", packet.getCause());
		}
	}
}
