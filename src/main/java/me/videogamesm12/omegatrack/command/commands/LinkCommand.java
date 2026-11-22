package me.videogamesm12.omegatrack.command.commands;

import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import me.videogamesm12.omegatrack.OmegaTrack;
import me.videogamesm12.omegatrack.command.AbstractOmegaTrackCommand;

import java.util.UUID;

public class LinkCommand extends AbstractOmegaTrackCommand
{
    public LinkCommand(OmegaTrack omegaTrack)
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
        }
        catch (Exception ex)
        {
            throw new CommandException("Illegal number provided.");
        }

        final UUID uuid;

        try
        {
            uuid = UUID.fromString(args[1]);
        }
        catch (Exception ex)
        {
            throw new CommandException("Illegal UUID provided.");
        }

        if (omegaTrack.wiretap.isLinked(id))
        {
            throw new CommandException("ID #" + id + " is already tied to UUID " + omegaTrack.wiretap.getById(id));
        }

        if (omegaTrack.wiretap.isLinked(uuid))
        {
            throw new CommandException("UUID " + uuid + " is already tied to ID " + omegaTrack.wiretap.getUuids().get(uuid));
        }

        omegaTrack.wiretap.link(id, uuid);
        sender.getBot().sendResponse("ID #" + id + " has been tied to " + uuid + ".", sender.getUuid().toString());
    }

    @Override
    public String getName()
    {
        return "index";
    }

    @Override
    public String[] getSyntax()
    {
        return new String[] {"<id>", "<uuid>"};
    }

    @Override
    public String getDescription()
    {
        return "Link a numerical ID to a username.";
    }

    @Override
    public int getDefaultPermission()
    {
        return 2;
    }
}
