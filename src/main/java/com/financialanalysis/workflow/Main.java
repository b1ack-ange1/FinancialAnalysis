package com.financialanalysis.workflow;

import com.financialanalysis.guice.FAModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;

@Log4j
public class Main {
    @SneakyThrows
    public static void main(String[] args) {
//        for(String s : args) {
//            log.info(s);
//        }

        // Use jcommander to parse command line options

        Injector injector = Guice.createInjector(new FAModule());
        FAService faService = injector.getInstance(FAService.class);
        faService.start();
        faService.stop();
    }
}
