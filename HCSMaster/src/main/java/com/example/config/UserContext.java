package com.example.config;

import com.example.pojo.User;

public class UserContext {

    private static ThreadLocal<User> userThreadLocal = new ThreadLocal<>();

    public static void setUser(User tUser) {
        userThreadLocal.set(tUser);
    }

    public static User getUser() {
        return userThreadLocal.get();
    }
}
