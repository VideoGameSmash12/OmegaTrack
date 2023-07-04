package me.videogamesm12.omegatrack.command;

import com.github.hhhzzzsss.epsilonbot.command.ChatCommand;
import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import me.videogamesm12.omegatrack.OmegaTrack;

public class ResetCommand extends ChatCommand
{
    @Override
    public void executeChat(ChatSender sender, String args) throws CommandException
    {
        OmegaTrack.getWiretap().resetBruteforcer(0);
        sender.getBot().sendResponse("Okay, I've reset the bruteforcer for you.", sender.getMsgSender());
    }

    @Override
    public String getName()
    {
        return "reset";
    }

    @Override
    public String[] getSyntax()
    {
        return new String[0];
    }

    @Override
    public String getDescription()
    {
        return "Resets the bruteforcer back to the currently set offset.";
    }

    @Override
    public int getDefaultPermission()
    {
        return 1;
    }
}
