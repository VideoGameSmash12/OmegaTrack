package me.videogamesm12.omegatrack.command.commands;

import com.github.hhhzzzsss.epsilonbot.command.ChatCommand;
import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import me.videogamesm12.omegatrack.OmegaTrack;
import me.videogamesm12.omegatrack.command.AbstractOmegaTrackCommand;

public class WTCommand extends AbstractOmegaTrackCommand
{
    public WTCommand(final OmegaTrack omegaTrack)
    {
        super(omegaTrack);
    }

    @Override
    public void executeChat(ChatSender sender, String args) throws CommandException
    {
        sender.getBot().sendResponse("Traditional ID #" + this.omegaTrack.wiretap.getCurrentTraditionalId() + ". Backwards ID #" + this.omegaTrack.wiretap.getCurrentBackwardsId() + ". " + this.omegaTrack.wiretap.getUuids().size() + " player(s) have been indexed.", sender.getMsgSender());
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
