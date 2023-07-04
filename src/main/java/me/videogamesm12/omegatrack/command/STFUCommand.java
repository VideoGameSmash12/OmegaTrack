package me.videogamesm12.omegatrack.command;

import com.github.hhhzzzsss.epsilonbot.command.ChatCommand;
import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import me.videogamesm12.omegatrack.OmegaTrack;
import me.videogamesm12.omegatrack.storage.OTFlags;
import me.videogamesm12.omegatrack.util.UUIDUtil;

public class STFUCommand extends ChatCommand
{
    @Override
    public void executeChat(ChatSender sender, String args) throws CommandException
    {
        if (sender.getUuid().equals(UUIDUtil.SYSTEM_UUID))
        {
            throw new CommandException("You must be in-game to do this.");
        }

        final OTFlags.UserFlags flags = OmegaTrack.FLAGS.getFlags(sender.getUuid());
        flags.setSupposedToShutUp(!flags.isSupposedToShutUp());

        final String status = flags.isSupposedToShutUp() ? "no longer" : "now";

        sender.getBot().sendResponse("You will " + status + " be reminded when you are opted-in and you join the server.", sender.getMsgSender());
    }

    @Override
    public String getName()
    {
        return "stfu";
    }

    @Override
    public String[] getSyntax()
    {
        return new String[0];
    }

    @Override
    public String getDescription()
    {
        return "Instructs the bot to not notify you if you are opted-in and you join the server";
    }

    @Override
    public int getDefaultPermission()
    {
        return 0;
    }
}
