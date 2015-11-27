package com.financialanalysis.workflow;

import com.google.inject.Inject;
import lombok.extern.log4j.Log4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log4j
public class FAService {
    private final ExecutorService executorService;
    private final ServiceMain serviceMain;

    @Inject
    public FAService(ServiceMain serviceMain) {
        this.serviceMain = serviceMain;
        executorService = Executors.newSingleThreadExecutor();
    }

    public void start() {
        log.info("Starting up FA");
        addShutdownHook();
        executorService.execute(serviceMain);
    }

    public void stop() {
        executorService.shutdown();
    }

    private void addShutdownHook() {
        Runnable shutdownHook = () -> {
            log.info("JVM shutdown hook fired");
            stop();
        };
        Runtime.getRuntime().addShutdownHook(new Thread(shutdownHook, "FA Shutdown Hook"));
    }
}
