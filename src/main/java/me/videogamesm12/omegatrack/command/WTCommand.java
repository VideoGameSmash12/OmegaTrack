package me.videogamesm12.omegatrack.command;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import com.github.hhhzzzsss.epsilonbot.command.ChatCommand;
import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import me.videogamesm12.omegatrack.OmegaTrack;

public class WTCommand extends ChatCommand
{
    @Override
    public void executeChat(ChatSender sender, String args) throws CommandException
    {
        EpsilonBot.INSTANCE.sendChat("I am currently at ID #" + OmegaTrack.WIRETAP.getCurrentId());
        EpsilonBot.INSTANCE.sendChat("I have indexed " + OmegaTrack.WIRETAP.getUuids().size() + " players so far.");
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
