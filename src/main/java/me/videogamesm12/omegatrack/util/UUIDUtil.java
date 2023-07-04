package me.videogamesm12.omegatrack.util;

import java.util.UUID;

public class UUIDUtil
{
    public static final UUID SYSTEM_UUID = new UUID(0, 0);

    /**
     * Creates UUIDs using data gathered from integer arrays.
     * @param uuid  int[]
     * @return      UUID
     */
    public static UUID fromIntArray(int[] uuid)
    {
        if (uuid.length != 4)
        {
            throw new IllegalArgumentException("An array of 4 integers is required for this to work");
        }

        // Shift every portion around
        return new UUID(
                (long) uuid[0] << 32 | uuid[1] & 4294967295L,    // Portions 1 and 2, shifted
                (long) uuid[2] << 32 | uuid[3] & 4294967295L);   // Portions 3 and 4, shifted
    }
}
