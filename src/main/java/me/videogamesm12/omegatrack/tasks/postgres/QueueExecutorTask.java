package me.videogamesm12.omegatrack.tasks.postgres;

import me.videogamesm12.omegatrack.PostgreSQLStorage;
import me.videogamesm12.omegatrack.data.PositionDataset;

import java.util.Queue;
import java.util.TimerTask;

public class QueueExecutorTask extends TimerTask
{
    private final PostgreSQLStorage postgreSQLStorage;

    public QueueExecutorTask(final PostgreSQLStorage postgreSQLStorage) {
        this.postgreSQLStorage = postgreSQLStorage;
    }

    @Override
    public void run() {
        final Queue<PositionDataset> queue = this.postgreSQLStorage.getQueue();

        for (int i = 0; i < queue.size(); i++)
        {
            try
            {
                this.postgreSQLStorage.addSet(queue.poll());
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }

    }
}
