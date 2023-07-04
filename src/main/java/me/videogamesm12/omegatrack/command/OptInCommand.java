package me.videogamesm12.omegatrack.command;

import com.github.hhhzzzsss.epsilonbot.command.ChatCommand;
import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import me.videogamesm12.omegatrack.OmegaTrack;
import me.videogamesm12.omegatrack.util.UUIDUtil;

public class OptInCommand extends ChatCommand
{
    @Override
    public void executeChat(ChatSender sender, String args) throws CommandException
    {
        if (sender.getUuid().equals(UUIDUtil.SYSTEM_UUID))
        {
            throw new CommandException("You must be in-game to opt-in.");
        }

        OmegaTrack.FLAGS.getFlags(sender.getUuid()).setOptedOut(false);
        sender.getBot().sendResponse("You will now be tracked by OmegaTrack. To start being tracked immediately, teleport to me or reconnect.", sender.getMsgSender());
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
