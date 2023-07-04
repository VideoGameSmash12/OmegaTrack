package me.videogamesm12.omegatrack.data;

import lombok.Getter;
import me.videogamesm12.omegatrack.util.UUIDUtil;

import java.sql.PreparedStatement;
import java.util.Date;
import java.util.UUID;

@Getter
public class PositionDataset
{
    // IDENTIFICATION
    private final UUID uuid;
    // TIME
    private final long time;
    // POSITION
    private final String world;
    private final double x;
    private final double z;

    public PositionDataset(UUID uuid, String world, long time, double x, double z)
    {
        this.uuid = uuid;
        this.time = time;
        this.world = world;
        this.x = x;
        this.z = z;
    }

    public PositionDataset(int[] uuid, String world, long time, double x, double z)
    {
        this(UUIDUtil.fromIntArray(uuid), world, time, x, z);
    }

    public PositionDataset(int[] uuid, String world, double x, double z)
    {
        this(UUIDUtil.fromIntArray(uuid), world, new Date().getTime(), x, z);
    }

    public PositionDataset(UUID uuid, String world, double x, double z)
    {
        this(uuid, world, new Date().getTime(), x, z);
    }
}
