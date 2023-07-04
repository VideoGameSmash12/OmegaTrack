package me.videogamesm12.omegatrack.command;

import com.github.hhhzzzsss.epsilonbot.command.ChatCommand;
import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import me.videogamesm12.omegatrack.OmegaTrack;

public class WTCommand extends ChatCommand
{
    @Override
    public void executeChat(ChatSender sender, String args) throws CommandException
    {
        sender.getBot().sendResponse("Traditional ID #" + OmegaTrack.getWiretap().getCurrentTraditionalId()
                + ". Backwards ID #" + OmegaTrack.getWiretap().getCurrentBackwardsId() + ". "
                + OmegaTrack.getWiretap().getUuids().size() + " player(s) have been indexed.", sender.getMsgSender());
    }

    @Override
    public String getName()
    {
        return "wiretap";
    }

    @Override
    public String[] getSyntax()
    {
        return new String[0];
    }

    @Override
    public String getDescription()
    {
        return "Get the status of the Wiretap.";
    }

    @Override
    public int getDefaultPermission()
    {
        return 1;
    }
}
