package me.videogamesm12.omegatrack.command;

import com.github.hhhzzzsss.epsilonbot.command.ChatCommand;
import com.github.hhhzzzsss.epsilonbot.command.ChatSender;
import com.github.hhhzzzsss.epsilonbot.command.CommandException;
import me.videogamesm12.omegatrack.OmegaTrack;

public class SetOffsetCommand extends ChatCommand
{
    @Override
    public void executeChat(ChatSender sender, String arg) throws CommandException
    {
        String[] args = arg.split(" ");

        if (arg.trim().isEmpty())
        {
            throw new CommandException("Please provide a number.");
        }

        int number;

        try
        {
            number = Integer.parseInt(args[0]);
        }
        catch (Exception ex)
        {
            throw new CommandException("Illegal number provided.");
        }

        //OmegaTrack.WIRETAP.setCurrentId(number);
        OmegaTrack.WIRETAP.resetBruteforcer(number);
        sender.getBot().sendResponse("Okay, reset the grabber to start at #" + number + ".", sender.getMsgSender());
    }

    @Override
    public String getName()
    {
        return "setoffset";
    }

    @Override
    public String[] getSyntax()
    {
        return new String[0];
    }

    @Override
    public String getDescription()
    {
        return "Sets the offset of where to start at in the grabber and resets the grabber itself";
    }

    @Override
    public int getDefaultPermission()
    {
        return 1;
    }
}
