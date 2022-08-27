package com.example.community.util;

import com.example.community.entity.User;
import org.springframework.stereotype.Component;

@Component
public class HostHodler {

    private final ThreadLocal<User> threadLocal = new ThreadLocal<>();

    public void setUser(User user) {
        threadLocal.set(user);
    }

    public User getUser() {
        return threadLocal.get();
    }

    public void clearUser() {
        threadLocal.remove();
    }
}
