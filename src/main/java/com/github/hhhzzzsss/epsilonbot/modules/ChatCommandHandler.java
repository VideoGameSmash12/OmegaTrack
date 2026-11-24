package com.github.hhhzzzsss.epsilonbot.modules;

import com.github.hhhzzzsss.epsilonbot.Config;
import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.command.*;
import com.github.hhhzzzsss.epsilonbot.listeners.PacketListener;
import com.github.hhhzzzsss.epsilonbot.util.ChatUtils;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.apache.commons.lang3.ArrayUtils;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundDisguisedChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;

import java.util.Map;
import java.util.UUID;

public class ChatCommandHandler implements PacketListener
{
	private final EpsilonBot bot;
	private final CommandList commandList;

	public ChatCommandHandler(EpsilonBot bot, CommandList commandList)
	{
		this.bot = bot;
		this.commandList = commandList;
	}

	private void handleFullChat(Component message) {
		// Read message
		String strMessage = ChatUtils.getFullText(message).replaceAll("§[0-9a-fklmnor]", "");

		if (message instanceof TextComponent && ChatUtils.isEssentialsMessage(strMessage))
		{
			final String[] nameAndMessage = ChatUtils.readEssentialsMessage(strMessage);
			final UUID uuid = nameAndMessage[0].contains("~") ?
					bot.getPlayerListTracker().getPlayerList().entrySet()
							.stream()
							.filter(entry ->
                                    ChatUtils.getFullText(entry.getValue().getDisplayName())
                                            .replaceAll("§[0-9a-fklmnor]", "")
											.endsWith(nameAndMessage[0]))
							.findAny()
							.map(Map.Entry::getKey)
							.orElse(null):
					bot.getPlayerListTracker().getUuidMap().get(nameAndMessage[0]);

			if (uuid == null)
			{
				EpsilonBot.getLogger().warn("Unable to find player list entry for sender {}", nameAndMessage[0]);
				return;
			}

			// Don't acknowledge your own messages lol
			if (uuid.equals(bot.getUuid()))
			{
				return;
			}

			// Reject blacklisted users
			if (ModerationManager.isBlacklisted(uuid))
			{
				return;
			}

			final String username = bot.getPlayerListTracker().getPlayerList().get(uuid).getName();
			final String rawMessage = nameAndMessage[1];

			// Nice try.
			if (rawMessage.contains("<") || rawMessage.contains(">"))
			{
				return;
			}

			final String[] command = rawMessage.split(" ");

			if (command.length == 0)
			{
				return;
			}

			try
			{
				runCommand(uuid, username, command[0], ArrayUtils.subarray(command, 1, command.length));
			}
			catch (CommandException ex)
			{
				bot.sendMsg("Error: " + ex.getMessage(), uuid.toString());
			}
		}
	}

	public void onPacket(Packet packet)
	{
		if (packet instanceof ClientboundDisguisedChatPacket disguised)
		{
			handleFullChat(disguised.getMessage());
		}
		else if (packet instanceof ClientboundSystemChatPacket system)
		{
			handleFullChat(system.getContent());
		}
	}

	public void runCommand(@NonNull UUID uuid,
						   @NonNull String username,
						   @NonNull String command,
						   @NonNull String[] arguments) throws CommandException
	{
		if (command.isEmpty())
		{
			return;
		}

		// Find our command
		final Command entry = commandList.get(command.toLowerCase());
		if (entry == null)
		{
			throw new CommandException("Unknown command - " + command);
		}

		if (!(entry instanceof ChatCommand cmd))
		{
			throw new CommandException("This command cannot be run from Minecraft chat");
		}

		// Trusted users have level 2
		// Moderators have level 1
		// Regular users have level 0
		int permission = Config.getConfig().getTrusted().contains(uuid.toString()) ? 2 : ModerationManager.isStaff(uuid) ? 1 : 0;

		if (cmd.getPermission() > permission)
		{
			throw new CommandException("You don't have permission to run this command");
		}

		cmd.executeChat(new ChatSender(bot, uuid, username, permission), String.join(" ", arguments));
	}
}
