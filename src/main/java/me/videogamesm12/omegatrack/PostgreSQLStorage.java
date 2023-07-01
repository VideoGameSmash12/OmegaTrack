package me.videogamesm12.omegatrack;

import me.videogamesm12.omegatrack.data.PositionDataset;
import me.videogamesm12.omegatrack.storage.OTConfig;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PostgreSQLStorage extends Thread
{
    private Queue<PositionDataset> queue = new ConcurrentLinkedQueue<>();
    private ScheduledExecutorService queueManager = new ScheduledThreadPoolExecutor(2);
    //--
    private Connection connection;

    public PostgreSQLStorage()
    {
        super("SQLStorage");
        super.start();
    }

    @Override
    public void run()
    {
        try
        {
            System.out.println("Connecting to " + String.format(
                    "jdbc:postgresql://%s:%s/omegatrack",
                    OTConfig.INSTANCE.getSqlIp(),
                    OTConfig.INSTANCE.getSqlPort()
            ));
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection(
                    String.format(
                            "jdbc:postgresql://%s:%s/omegatrack",
                            OTConfig.INSTANCE.getSqlIp(),
                            OTConfig.INSTANCE.getSqlPort()
                    ),
                    OTConfig.INSTANCE.getSqlUser(),
                    OTConfig.INSTANCE.getSqlPassword());
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        // Sets up the queue now
        queueManager.scheduleAtFixedRate(() -> {
            for (int i = 0; i < queue.size(); i++)
            {
                try
                {
                    addSet(queue.poll());
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    @Override
    public void interrupt()
    {
        // Wait until what's in memory is written to disk.
        try
        {
            queueManager.shutdown();
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

    public void sendStatement(String statement) throws SQLException
    {
        connection.prepareStatement(statement).execute();
    }

    private void addSet(@NotNull PositionDataset set) throws SQLException
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
        PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM coordinates where uuid = '" + uuid.toString() + "'");
        statement.execute();
    }
}
