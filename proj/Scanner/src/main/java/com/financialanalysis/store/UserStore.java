package com.financialanalysis.store;

import com.financialanalysis.data.User;

import java.util.Arrays;
import java.util.List;

public class UserStore {
    private List<User> userList = Arrays.asList(
            new User(""),
            new User("")
    );

    public List<User> load() {
        return userList;
    }
}
