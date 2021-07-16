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
package io.helidon.examples.dbclient.mapper.users;

import io.helidon.dbclient.DbColumn;
import io.helidon.dbclient.DbMapper;
import io.helidon.dbclient.DbRow;
import io.helidon.examples.dbclient.entity.Users;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsersMapper implements DbMapper<Users> {

    @Override
    public Users read(DbRow row) {
        DbColumn id = row.column("id");
        DbColumn username = row.column("username");
        DbColumn password = row.column("password");
        DbColumn role = row.column("role");
        return new Users(id.as(Integer.class), username.as(String.class), password.as(String.class), role.as(String.class));
    }

    @Override
    public Map<String, Object> toNamedParameters(Users value) {
        Map<String, Object> map = new HashMap<>(3);
        map.put("id", value.getId());
        map.put("username", value.getUsername());
        map.put("password", value.getPassword());
        map.put("role", value.getRole());
        return map;
    }

    @Override
    public List<Object> toIndexedParameters(Users value) {
        List<Object> list = new ArrayList<>(3);
        list.add(value.getId());
        list.add(value.getUsername());
        list.add(value.getPassword());
        list.add(value.getRole());
        return list;
    }

}
