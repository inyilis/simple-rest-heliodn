/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates.
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
package io.helidon.examples.dbclient.config;

import com.google.gson.Gson;
import io.helidon.dbclient.DbClient;
import io.helidon.examples.dbclient.entity.Users;

import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.List;

public class InitializeDb {

    public static void init(DbClient dbClient) {
        try {
            dbClient.execute(exec -> exec.namedDml("create-users"))
                    .await();
        } catch (Exception ex) {
            System.out.printf("Could not create tables: %s", ex.getMessage());
        }
    }

    public static List<Users> findAllUsers(DbClient dbClient){
        List<Users> userList = new ArrayList<>();

        dbClient.execute(exec -> exec.namedQuery("select-all-users"))
                .map(it -> it.as(JsonObject.class)).forEach(it -> {
            Users user = new Gson().fromJson(it.toString(), Users.class);
            userList.add(user);
        }).await();

        return userList;
    }

    private InitializeDb() {
        throw new UnsupportedOperationException("Instances of InitializeDb utility class are not allowed");
    }

}
