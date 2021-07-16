package io.helidon.examples.dbclient.config;

import io.helidon.examples.dbclient.entity.Users;
import io.helidon.security.providers.httpauth.SecureUserStore;

import java.util.Arrays;
import java.util.Set;

public class AppUser implements SecureUserStore.User {
    private final String login;
    private final char[] password;
    private final Set<String> roles;

    public AppUser(Users login) {
        this.login = login.getUsername();
        this.password = login.getPassword().toCharArray();
        this.roles = Set.of(login.getRole());
    }

    private char[] password() {
        return password;
    }

    @Override
    public boolean isPasswordValid(char[] password) {
        return Arrays.equals(password(), password);
    }

    @Override
    public Set<String> roles() {
        return roles;
    }

    @Override
    public String login() {
        return login;
    }
}

