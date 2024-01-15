package com.example.federatedserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);

    public void scheduleTaskWithFixedRate() {
    }

    public void scheduleTaskWithFixedDelay() {
    }

    public void scheduleTaskWithInitialDelay() {
    }

    @Scheduled(cron = "0 0 0 1 * ?")
    public void scheduleTaskWithCronExpression() throws Exception {
        logger.info("AverageWeights task is running...");
        ClassifierNNAverageWeights.run();
    }
}
