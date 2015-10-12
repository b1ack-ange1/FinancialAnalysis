package com.financialanalysis.workflow;

import com.financialanalysis.guice.FAModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.SneakyThrows;

public class Main {
    @SneakyThrows
    public static void main(String[] args) {
        Injector injector = Guice.createInjector(new FAModule());
        FAService faService = injector.getInstance(FAService.class);
        faService.start();

//        Thread.sleep(1000);
        faService.stop();
    }
}
