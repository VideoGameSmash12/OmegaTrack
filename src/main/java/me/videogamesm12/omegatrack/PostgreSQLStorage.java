package me.videogamesm12.omegatrack;

import lombok.Getter;
import me.videogamesm12.omegatrack.data.PositionDataset;
import me.videogamesm12.omegatrack.storage.OTConfig;
import me.videogamesm12.omegatrack.tasks.postgres.QueueExecutorTask;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PostgreSQLStorage extends Thread
{
    @Getter
    private final Queue<PositionDataset> queue = new ConcurrentLinkedQueue<>();
    //--
    private Connection connection;
    private final Timer timer;
    private TimerTask queueExecutorTask;

    public PostgreSQLStorage(final Timer timer)
    {
        super("SQLStorage");
        super.start();
        this.timer = timer;
    }

    @Override
    public void run()
    {
        try
        {
            System.out.println("Connecting to " + String.format(
                    "jdbc:postgresql://%s:%s/%s",
                    OTConfig.INSTANCE.getSql().getIp(),
                    OTConfig.INSTANCE.getSql().getPort(),
                    OTConfig.INSTANCE.getSql().getDatabase()
            ));
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection(
                    String.format(
                            "jdbc:postgresql://%s:%s/%s",
                            OTConfig.INSTANCE.getSql().getIp(),
                            OTConfig.INSTANCE.getSql().getPort(),
                            OTConfig.INSTANCE.getSql().getDatabase()
                    ),
                    OTConfig.INSTANCE.getSql().getUsername(),
                    OTConfig.INSTANCE.getSql().getPassword());
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        // Sets up the queue now
        this.queueExecutorTask = new QueueExecutorTask(this);
        this.timer.scheduleAtFixedRate(this.queueExecutorTask, 0, OTConfig.INSTANCE.getSql().getQueueInterval());
    }

    @Override
    public void interrupt()
    {
        // Wait until what's in memory is written to disk.
        try
        {
            if (this.queueExecutorTask != null)
            {
                this.queueExecutorTask.cancel();
            }
            connection.close();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        super.interrupt();
    }

    public void queue(PositionDataset set)
    {
        queue.add(set);
    }

    public void addSet(@NotNull PositionDataset set) throws SQLException
    {
        PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO coordinates (uuid, world, time, x, z) VALUES (?, ?, ?, ?, ?)");
        //--
        statement.setString(1, set.getUuid().toString());
        statement.setString(2, set.getWorld());
        statement.setLong(3, set.getTime());
        statement.setDouble(4, set.getX());
        statement.setDouble(5, set.getZ());
        //--
        System.out.println("Executing update");
        statement.executeUpdate();
        System.out.println("Execute completed");
    }

    public void dropCoordinatesByUuid(UUID uuid) throws SQLException
    {
        PreparedStatement statement = connection.prepareStatement("DELETE FROM coordinates WHERE uuid = ?;");
        statement.setString(1, uuid.toString());
        statement.execute();
    }
}
