package me.videogamesm12.omegatrack.command;

import com.github.hhhzzzsss.epsilonbot.command.ChatCommand;
import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import me.videogamesm12.omegatrack.OmegaTrack;

import java.sql.SQLException;
import java.util.UUID;

public class OptOutCommand extends ChatCommand
{
    @Override
    public void executeChat(ChatSender sender, String args) throws CommandException
    {
        if (sender.getUuid().equals(UUID.fromString("00000000-0000-0000-0000-000000000000")))
        {
            throw new CommandException("You must be in-game to opt-out.");
        }

        OmegaTrack.FLAGS.getFlags(sender.getUuid()).setOptedOut(true);
        try
        {
            OmegaTrack.STORAGE.dropCoordinatesByUuid(sender.getUuid());
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        OmegaTrack.WIRETAP.unlink(sender.getUuid());
        sender.getBot().sendResponse("Got it, you will no longer be tracked by OmegaTrack.", sender.getMsgSender());
    }

    @Override
    public String getName()
    {
        return "optout";
    }

    @Override
    public String[] getSyntax()
    {
        return new String[0];
    }

    @Override
    public String getDescription()
    {
        return "Marks you as a player who does not wish to have their coordinates tracked.";
    }

    @Override
    public int getDefaultPermission()
    {
        return 0;
    }
}
