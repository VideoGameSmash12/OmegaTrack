package me.videogamesm12.omegatrack.command;

import com.github.hhhzzzsss.epsilonbot.command.ChatCommand;
import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import me.videogamesm12.omegatrack.OmegaTrack;

import java.util.UUID;

public class AmIIndexedCommand extends ChatCommand
{
    @Override
    public void executeChat(ChatSender sender, String args) throws CommandException
    {
        if (sender.getUuid().equals(UUID.fromString("00000000-0000-0000-0000-000000000000")))
        {
            throw new CommandException("Non-players are not indexed by default.");
        }

        final String result = OmegaTrack.WIRETAP.getUuids().containsKey(sender.getUuid()) ?
                "Yes, you are indexed." : "No, you are not currently indexed.";

        sender.getBot().sendResponse(result, sender.getMsgSender());
    }

    @Override
    public String getName()
    {
        return "isindexed";
    }

    @Override
    public String[] getSyntax()
    {
        return new String[0];
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
