package me.videogamesm12.omegatrack.command.commands;

import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import me.videogamesm12.omegatrack.OmegaTrack;
import me.videogamesm12.omegatrack.command.AbstractOmegaTrackCommand;
import me.videogamesm12.omegatrack.util.UUIDUtil;

import java.sql.SQLException;

public class OptOutCommand extends AbstractOmegaTrackCommand
{
    public OptOutCommand(final OmegaTrack omegaTrack)
    {
        super(omegaTrack);
    }

    @Override
    public void executeChat(ChatSender sender, String args) throws CommandException
    {
        if (sender.getUuid().equals(UUIDUtil.SYSTEM_UUID))
        {
            throw new CommandException("You must be in-game to opt-out.");
        }

        this.omegaTrack.flags.getFlags(sender.getUuid()).setOptedOut(true);

        try
        {
            sender.getBot().sendCommand("/tell " + sender.getMsgSender() + " Deleting coordinate data connected to you...");
            this.omegaTrack.storage.dropCoordinatesByUuid(sender.getUuid());
        }
        catch (SQLException e)
        {
            sender.getBot().sendCommand("/tell " + sender.getMsgSender() + " There was a problem whilst trying to delete coordinate data connected to you. Please contact the maintainer of this bot (videogamesm12) &oimmediately&f.");
            e.printStackTrace();
        }

        this.omegaTrack.wiretap.unlink(sender.getUuid());
        sender.getBot().sendResponse("You will no longer be tracked by OmegaTrack.", sender.getMsgSender());
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
