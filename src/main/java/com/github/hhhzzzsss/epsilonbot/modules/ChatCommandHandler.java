package com.github.hhhzzzsss.epsilonbot.modules;

import com.github.hhhzzzsss.epsilonbot.Config;
import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.command.*;
import com.github.hhhzzzsss.epsilonbot.listeners.PacketListener;
import com.github.hhhzzzsss.epsilonbot.util.ChatUtils;
import com.github.hhhzzzsss.epsilonbot.util.HashUtils;
import com.mojang.brigadier.CommandDispatcher;
import lombok.Getter;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.apache.commons.lang3.ArrayUtils;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundDisguisedChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatCommandHandler implements PacketListener
{
	private final EpsilonBot bot;
	private final CommandList commandList;
	@Getter private String prefix;
	@Getter private ArrayList<String> alternatePrefixes;
	@Getter private Pattern prefixPattern;
	@Getter private Pattern commandPattern;
	@Getter private Pattern msgCommandPattern;
	@Getter private Pattern chatCommandPattern;
	@Getter private Pattern discordCommandPattern;

	public ChatCommandHandler(EpsilonBot bot, CommandList commandList, String prefix, ArrayList<String> alternatePrefixes) {
		this.bot = bot;
		this.commandList = commandList;
		this.setPrefix(prefix, alternatePrefixes);
	}

	private void handleFullChat(Component message) {
		// Read message
		String strMessage = ChatUtils.getFullText(message).replaceAll("§[0-9a-fklmnor]", "");

		if (message instanceof TextComponent && ChatUtils.isEssentialsMessage(strMessage))
		{
			final String[] nameAndMessage = ChatUtils.readEssentialsMessage(strMessage);
			final UUID uuid = nameAndMessage[0].startsWith("~") ?
					bot.getPlayerListTracker().getPlayerList().entrySet()
							.stream()
							.filter(entry ->
                                    nameAndMessage[0].equals(ChatUtils.getFullText(entry.getValue().getDisplayName())
                                            .replaceAll("§[0-9a-fklmnor]", "")))
							.findAny()
							.map(Map.Entry::getKey)
							.orElse(null):
					bot.getPlayerListTracker().getUuidMap().get(nameAndMessage[0]);

			if (uuid == null)
			{
				System.out.println("Unable to find player list entry for " + nameAndMessage[0]);
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

	/**
	 * Sets the prefix and compiles a new command pattern using this prefix
	 *
	 * @param prefix The command prefix
	 */
	public void setPrefix(String prefix, ArrayList<String> alternatePrefixes) {
		ArrayList<String> allPrefixes = new ArrayList<>();
		allPrefixes.add(prefix);
		allPrefixes.addAll(alternatePrefixes);
		String prefixMatchingString = String.join("|", allPrefixes);

		prefixPattern = Pattern.compile(prefixMatchingString);
		commandPattern = Pattern.compile(String.format("((?:%s)\\S+)(.*)?", prefixMatchingString));
		msgCommandPattern = Pattern.compile(String.format("\\| (\\S+) \\((.+?)\\) > You: ((?:%s)\\S+)(.*)?", prefixMatchingString)); // obsolete
		chatCommandPattern = Pattern.compile(String.format("(.*(\\S+)\\s*(?: »|:) )+((?:%s)\\S+)(.*)?", prefixMatchingString));
		discordCommandPattern = Pattern.compile(String.format("(\\[Discord\\] .*: )((?:`)\\S+)(.*)?(.*)?", prefixMatchingString)); // discord format no longer uses colon, but now this is covered by the chat command pattern
	}
}
