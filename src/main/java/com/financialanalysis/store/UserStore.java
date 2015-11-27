package com.financialanalysis.store;

import com.financialanalysis.data.User;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class UserStore {
    private final String fileName = "emails";
    static {
        createUserStore();
    }

    @SneakyThrows
    private static void createUserStore() {
        Path path = Paths.get(getUseStoreDir());
        Files.createDirectories(path);
    }

    private static String getUseStoreDir() {
        return "var/users/";
    }

    @SneakyThrows
    public List<User> load() {
        File file = new File(getUseStoreDir() + "/" + fileName);
        List<String> emails = FileUtils.readLines(file);

        List<User> users = emails.stream().map(e -> new User(e)).collect(Collectors.toList());
        return users;
    }
}
