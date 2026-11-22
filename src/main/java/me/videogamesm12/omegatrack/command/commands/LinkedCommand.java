package me.videogamesm12.omegatrack.command.commands;

import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import me.videogamesm12.omegatrack.OmegaTrack;
import me.videogamesm12.omegatrack.command.AbstractOmegaTrackCommand;
import me.videogamesm12.omegatrack.util.UUIDUtil;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class LinkedCommand extends AbstractOmegaTrackCommand
{

    public LinkedCommand(OmegaTrack omegaTrack)
    {
        super(omegaTrack);
    }

    @Override
    public void executeChat(ChatSender sender, String arg) throws CommandException
    {
        final String[] args = arg.split(" ");

        if (args.length == 0)
        {
            if (sender.getUuid().equals(UUIDUtil.SYSTEM_UUID))
            {
                throw new CommandException("Non-players are not linked by default.");
            }

            final Integer id = omegaTrack.wiretap.getUuids().get(sender.getUuid());

            sender.getBot().sendResponse(id != null ?
                    "You are currently linked to ID #" + id + "." :
                    "You are not currently linked.", sender.getUuid().toString());
        }
        else if (args.length == 1)
        {
            if (sender.getPermission() < 1)
            {
                throw new CommandException("You do not have permissions to check the link status of other users.");
            }

            try
            {
                final UUID uuid = UUID.fromString(args[0]);
                final Integer id = omegaTrack.wiretap.getUuids().get(uuid);

                sender.getBot().sendResponse(id != null ?
                        "UUID " + uuid + " is currently linked to ID #" + id + "." :
                        "UUID " + uuid + " is not currently linked to any numerical ID.", sender.getUuid().toString());
                return;
            }
            catch (IllegalArgumentException ignored)
            {
            }

            final Optional<UUID> nameToUuid = omegaTrack.epsilonBot.getPlayerListTracker().getUuidMap().entrySet()
                    .stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase(args[0]))
                    .map(Map.Entry::getValue)
                    .findAny();

            if (nameToUuid.isPresent())
            {
                final UUID uuid = nameToUuid.get();
                final Integer id = omegaTrack.wiretap.getUuids().get(uuid);

                sender.getBot().sendResponse(id != null ?
                        "User " + args[0] + " is currently linked to ID #" + id + "." :
                        "User " + args[0] + " is not currently linked to any numerical ID.", sender.getUuid().toString());
                return;
            }

            try
            {
                final int id = Integer.parseInt(args[0]);
                final UUID uuid = omegaTrack.wiretap.getById(id);

                sender.getBot().sendResponse(uuid != null ?
                        "ID " + id + " is currently linked to UUID " + uuid + "." :
                        "ID " + id + " is not currently linked to any UUID.", sender.getUuid().toString());
            }
            catch (NumberFormatException ex)
            {
                throw new CommandException(args[0] + " is neither a known name, a UUID, nor a numerical ID.");
            }
        }

        throw new CommandException("Usage: linked " + (sender.getPermission() >= 1 ? getSyntax()[0] : ""));
    }

    @Override
    public String getName()
    {
        return "linked";
    }

    @Override
    public String[] getSyntax()
    {
        return new String[] {"[name, uuid, or numerical ID]"};
    }

    @Override
    public String getDescription()
    {
        return "Returns whether or not you are indexed currently.";
    }

    @Override
    public int getDefaultPermission()
    {
        return 0;
    }
}
