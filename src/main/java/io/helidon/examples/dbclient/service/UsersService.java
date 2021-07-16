/*
 * Copyright (c) 2019, 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.examples.dbclient.service;

import com.google.gson.Gson;
import io.helidon.common.http.Http;
import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbRow;
import io.helidon.examples.dbclient.entity.Users;
import io.helidon.security.integration.webserver.WebSecurity;
import io.helidon.webserver.*;

import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Example service using a database.
 */
public class UsersService implements Service {

    private static final Logger LOGGER = Logger.getLogger(UsersService.class.getName());

    private final DbClient dbClient;

    public UsersService(DbClient dbClient) {
        this.dbClient = dbClient;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules
                .get("/users", WebSecurity.rolesAllowed("admin", "customer"), this::listUsers)
                .post("/login", this::loginUsers)
                .get("/users/{id}", WebSecurity.rolesAllowed("admin", "customer"), this::getUsersById)
                .post("/users", WebSecurity.rolesAllowed("admin"), Handler.create(Users.class, this::insertUser))
                .put("/users", WebSecurity.rolesAllowed("admin", "customer"), Handler.create(Users.class, this::updateUser))
                .delete("/users/{id}", WebSecurity.rolesAllowed("admin", "customer"), this::deleteUserById);
    }

    private void listUsers(ServerRequest request, ServerResponse response) {
        List<Users> users = new ArrayList<>();
        dbClient.execute(exec -> exec.namedQuery("select-all-users"))
                .map(it -> it.as(JsonObject.class)).forEach(it -> {
            users.add(new Gson().fromJson(it.toString(), Users.class));
        }).await();
        response.send(new Gson().toJson(users));
    }

    private void getUsersById(ServerRequest request, ServerResponse response) {
        try {
            int userId = Integer.parseInt(request.path().param("id"));
            String username = request.path().param("username");
            dbClient.execute(exec -> exec
                    .createNamedGet("select-users-by-id")
                    .addParam("id", userId)
                    .addParam("username", username)
                    .execute())
                    .thenAccept(maybeRow -> maybeRow
                    .ifPresentOrElse(
                            row -> sendRow(row, response),
                            () -> sendNotFound(response, "Users " + userId + " not found")))
                    .exceptionally(throwable -> sendError(throwable, response));
        } catch (NumberFormatException ex) {
            sendError(ex, response);
        }
    }

    private void loginUsers(ServerRequest request, ServerResponse response) {
        try {
            String username = request.queryParams().put("username").get(0);
            String password = request.queryParams().put("password").get(0);
            dbClient.execute(exec -> exec
                    .createNamedGet("login-users")
                    .addParam("username", username)
                    .addParam("password", password)
                    .execute())
                    .thenAccept(maybeRow -> maybeRow
                            .ifPresentOrElse(
                                    row -> sendRow(row, response),
                                    () -> sendNotFound(response, "Username or Password is wrong")))
                    .exceptionally(throwable -> sendError(throwable, response));
        } catch (NumberFormatException ex) {
            sendError(ex, response);
        }
    }

    private void insertUser(ServerRequest request, ServerResponse response, Users user) {
        dbClient.execute(exec -> exec
                .createNamedInsert("insert-users")
                .indexedParam(user)
                .execute())
                .thenAccept(count -> response.send("Inserted: " + count + " values\n"))
                .exceptionally(throwable -> sendError(throwable, response));
    }

    private void updateUser(ServerRequest request, ServerResponse response, Users user) {
        dbClient.execute(exec -> exec
                .createNamedUpdate("update-users-by-id")
                .namedParam(user)
                .execute())
                .thenAccept(count -> response.send("Updated: " + count + " values\n"))
                .exceptionally(throwable -> sendError(throwable, response));
    }

    private void deleteUserById(ServerRequest request, ServerResponse response) {
        try {
            int id = Integer.parseInt(request.path().param("id"));
            dbClient.execute(exec -> exec
                    .createNamedDelete("delete-users-by-id")
                    .addParam("id", id)
                    .execute())
                    .thenAccept(count -> response.send("Deleted: " + count + " values\n"))
                    .exceptionally(throwable -> sendError(throwable, response));
        } catch (NumberFormatException ex) {
            sendError(ex, response);
        }
    }

    private void sendNotFound(ServerResponse response, String message) {
        response.status(Http.Status.NOT_FOUND_404);
        response.send(message);
    }

    private void sendRow(DbRow row, ServerResponse response) {
        response.send(row.as(JsonObject.class));
    }

    private <T> T sendError(Throwable throwable, ServerResponse response) {
        Throwable realCause = throwable;
        if (throwable instanceof CompletionException) {
            realCause = throwable.getCause();
        }
        response.status(Http.Status.INTERNAL_SERVER_ERROR_500);
        response.send("Failed to process request: " + realCause.getClass().getName() + "(" + realCause.getMessage() + ")");
        LOGGER.log(Level.WARNING, "Failed to process request", throwable);
        return null;
    }

}
