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

package io.helidon.examples.dbclient;

import io.helidon.common.LogConfig;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Single;
import io.helidon.config.Config;
import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.health.DbClientHealthCheck;
import io.helidon.examples.dbclient.config.AppUser;
import io.helidon.examples.dbclient.config.InitializeDb;
import io.helidon.examples.dbclient.service.UsersService;
import io.helidon.health.HealthSupport;
import io.helidon.media.jsonb.JsonbSupport;
import io.helidon.media.jsonp.JsonpSupport;
import io.helidon.metrics.MetricsSupport;
import io.helidon.security.Security;
import io.helidon.security.SecurityContext;
import io.helidon.security.integration.webserver.WebSecurity;
import io.helidon.security.providers.httpauth.HttpBasicAuthProvider;
import io.helidon.security.providers.httpauth.SecureUserStore;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class Main {

    private Main() {
    }

    public static void main(final String[] args) {
        startServer();
    }

    static Single<WebServer> startServer() {

        // load logging configuration
        LogConfig.configureRuntime();

        // By default this will pick up application.yaml from the classpath
        Config config = Config.create();

        // Prepare routing for the server
        Routing routing = createRouting(config);

        WebServer server = WebServer.builder(routing)
                .addMediaSupport(JsonpSupport.create())
                .addMediaSupport(JsonbSupport.create())
                .config(config.get("server"))
//                .tracer(TracerBuilder.create(config.get("tracing")).build())
                .build();

        // Start the server and print some info.
        Single<WebServer> webserver = server.start();

        webserver.thenAccept(ws -> {
            System.out.println( "WEB server is up! http://localhost:" + ws.port() );
            ws.whenShutdown().thenRun(()
                    -> System.out.println("WEB server is DOWN. Good bye!"));
        }).exceptionallyAccept(t -> {
            System.err.println("Startup failed: " + t.getMessage());
            t.printStackTrace(System.err);
        });

        return webserver;
    }

    private static Routing createRouting(Config config) {
        Config dbConfig = config.get("db");

        // Client services are added through a service loader - see mongoDB example for explicit services
        DbClient dbClient = DbClient.builder(dbConfig)
                .build();
        // Some relational databases do not support DML statement as ping so using query which works for all of them
        HealthSupport health = HealthSupport.builder()
                .addLiveness(
                        DbClientHealthCheck.create(dbClient, dbConfig.get("health-check")))
                .build();

        // Initialize database schema
        InitializeDb.init(dbClient);

        return Routing.builder()
                .register(buildWebSecurity(dbClient).securityDefaults(WebSecurity.authenticate()))
                .register(health)                   // Health at "/health"
                .register(MetricsSupport.create())  // Metrics at "/metrics"
                .register("/api", new UsersService(dbClient))
                .build();
    }

    private static WebSecurity buildWebSecurity(DbClient dbClient) {
        Security security = Security.builder()
                .addAuthenticationProvider(
                        HttpBasicAuthProvider.builder()
                                .realm("helidon")
                                .userStore(buildUserStore(dbClient)),
                        "http-basic-auth")
                .build();
        return WebSecurity.create(security);
    }

    private static SecureUserStore buildUserStore(DbClient dbClient) {
        Map<String, AppUser> users = new HashMap<>();

        InitializeDb.findAllUsers(dbClient).
                forEach(it -> users.put(it.getUsername(), new AppUser(it)));

        return login -> Optional.ofNullable(users.get(login));
    }
}
