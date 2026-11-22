package me.videogamesm12.omegatrack.command.commands;

import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import me.videogamesm12.omegatrack.OmegaTrack;
import me.videogamesm12.omegatrack.command.AbstractOmegaTrackCommand;

import java.util.UUID;

public class UnlinkCommand extends AbstractOmegaTrackCommand
{
    public UnlinkCommand(OmegaTrack omegaTrack)
    {
        super(omegaTrack);
    }

    @Override
    public void executeChat(ChatSender sender, String arg) throws CommandException
    {
        String[] args = arg.split(" ");

        if (args.length != 2)
        {
            throw new CommandException("You must provide a numerical ID and a player UUID.");
        }

        final int id;

        try
        {
            id = Integer.parseInt(args[0]);

            if (!omegaTrack.wiretap.isLinked(id))
            {
                throw new CommandException("ID #" + id + " is not tied to any UUID.");
            }

            omegaTrack.wiretap.unlink(id);
        }
        catch (IllegalArgumentException ex)
        {
            final UUID uuid;

            try
            {
                uuid = UUID.fromString(args[1]);

                if (!omegaTrack.wiretap.isLinked(uuid))
                {
                    throw new CommandException("UUID " + uuid + " is not tied to any numerical ID.");
                }

                omegaTrack.wiretap.unlink(uuid);
            }
            catch (IllegalArgumentException ex2)
            {
                throw new CommandException("That was neither a numerical ID nor a UUID.");
            }
        }
    }

    @Override
    public String getName()
    {
        return "unlink";
    }

    @Override
    public String[] getSyntax()
    {
        return new String[] {"<uuid or ID>"};
    }

    @Override
    public String getDescription()
    {
        return "Unlink an entity ID or UUID.";
    }

    @Override
    public int getDefaultPermission()
    {
        return 2;
    }
}
