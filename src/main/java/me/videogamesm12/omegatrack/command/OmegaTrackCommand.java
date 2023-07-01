package me.videogamesm12.omegatrack.command;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.command.ChatCommand;
import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;

public class OmegaTrackCommand extends ChatCommand
{
    @Override
    public void executeChat(ChatSender sender, String args) throws CommandException
    {
        sender.getBot().sendResponse("OmegaTrack is a fork of EpsilonBot that tracks in-game coordinates for players who opt-in. This data will be used to help archive the server worlds in the future.", sender.getMsgSender());
        sender.getBot().sendResponse("For more information: https://gist.github.com/VideoGameSmash12/274165f38bde9597b7c5c2de10fa60a9", sender.getMsgSender());
    }

    @Override
    public String getName()
    {
        return "omegatrack";
    }

    @Override
    public String[] getSyntax()
    {
        return new String[0];
    }

    @Override
    public String getDescription()
    {
        return "Provides information about OmegaTrack.";
    }

    @Override
    public int getDefaultPermission()
    {
        return 0;
    }
}
