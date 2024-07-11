package arbor.astralis.vip;

import discord4j.core.GatewayDiscordClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public final class PerkRefresher {

    private static final Logger LOGGER = LogManager.getLogger();
    private static volatile PerkRefresher INSTANCE = null;

    private final GatewayDiscordClient client;
    private Timer timer;


    private PerkRefresher(GatewayDiscordClient client) {
        this.client = client;
        respawnTimer();
    }

    private void respawnTimer() {
        if (timer != null) {
            timer.cancel();
        }

        timer = new Timer("vip-refresh-dispatcher");

        long msUntilTomorrow = getMsUntilNextDayUtc();
        long msUntilTomorrowMins = TimeUnit.MILLISECONDS.toMinutes(msUntilTomorrow);
        LOGGER.info("New VIP refresh timer scheduled with delay: " + msUntilTomorrow + "ms (" + msUntilTomorrowMins + " mins)");

        timer.scheduleAtFixedRate(
            new VipPerkRefreshTask(client, this::respawnTimer),
            msUntilTomorrow,
            TimeUnit.DAYS.toMillis(1)
        );
    }

    public static void initialize(GatewayDiscordClient client) {
        if (INSTANCE == null) {
            synchronized (PerkRefresher.class) {
                if (INSTANCE == null) {
                    INSTANCE = new PerkRefresher(client);
                }
            }
        }
    }

    private long getMsUntilNextDayUtc() {
        long msNow = System.currentTimeMillis();
        long msElapsedSinceToday = msNow % TimeUnit.DAYS.toMillis(1);
        long msTomorrow = msNow - msElapsedSinceToday + TimeUnit.DAYS.toMillis(1);

        return msTomorrow - msNow;
    }

    private static final class VipPerkRefreshTask extends TimerTask {

        private final GatewayDiscordClient client;
        private final Runnable respawnTimerTask;

        private VipPerkRefreshTask(GatewayDiscordClient client, Runnable respawnTimerTask) {
            this.client = client;
            this.respawnTimerTask = respawnTimerTask;
        }

        @Override
        public void run() {
            LOGGER.info("Begin refreshing VIP perks now...");
            
            long start = System.nanoTime();
            PerkManager.refreshPerksForAllGuilds(client);
            long end = System.nanoTime();

            long duration = TimeUnit.NANOSECONDS.toMillis(end - start);
            LOGGER.info("Finished refreshing VIP perks now. Took: " + duration + "ms");
            
            respawnTimerTask.run();
        }
    }
}
