package me.videogamesm12.omegatrack.command;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.command.ChatCommand;
import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import me.videogamesm12.omegatrack.OmegaTrack;

public class OptInCommand extends ChatCommand
{
    @Override
    public void executeChat(ChatSender sender, String args) throws CommandException
    {
        OmegaTrack.FLAGS.getFlags(sender.getUuid()).setOptedOut(false);
        EpsilonBot.INSTANCE.sendChat("Got it, you will now be tracked by OmegaTrack.");
    }

    @Override
    public String getName()
    {
        return "optin";
    }

    @Override
    public String[] getSyntax()
    {
        return new String[0];
    }

    @Override
    public String getDescription()
    {
        return "Marks the player as someone who wishes to be tracked";
    }

    @Override
    public int getDefaultPermission()
    {
        return 0;
    }
}
