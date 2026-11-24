package me.videogamesm12.omegatrack.tasks.postgres;

import com.github.hhhzzzsss.epsilonbot.EpsilonBot;
import me.videogamesm12.omegatrack.PostgreSQLStorage;
import me.videogamesm12.omegatrack.data.PositionDataset;

import java.util.Queue;
import java.util.TimerTask;

public class QueueExecutorTask extends TimerTask
{
    private final PostgreSQLStorage postgreSQLStorage;

    private long lastLog;
    private int count;

    public QueueExecutorTask(final PostgreSQLStorage postgreSQLStorage)
    {
        this.postgreSQLStorage = postgreSQLStorage;
    }

    @Override
    public void run()
    {
        final Queue<PositionDataset> queue = this.postgreSQLStorage.getQueue();

        try
        {
            int size = queue.size();

            for (int i = 0; i < queue.size(); i++)
            {
                try
                {
                    this.postgreSQLStorage.addSet(queue.poll());
                }
                catch (Exception ex)
                {
                    EpsilonBot.getLogger().error("Failed to queue update", ex);
                }
            }

            if (System.currentTimeMillis() - lastLog > 5000)
            {
                EpsilonBot.getLogger().info("Queued {} entries since {}", count, lastLog);
                lastLog = System.currentTimeMillis();
                count = 0;
            }
            else
            {
                count = count + size;
            }
        }
        catch (Exception ex)
        {
            EpsilonBot.getLogger().error("We didn't even have a chance, lol", ex);
        }
    }
}
