package me.videogamesm12.omegatrack.exception;

public class StartFailureException extends Exception
{
    public StartFailureException(Throwable cause)
    {
        super("Failed to start OmegaTrack", cause);
    }
}
