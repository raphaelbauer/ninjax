/**
 * Copyright (C) the original author or authors.
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

package models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;


import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class Guestbook {

    public Integer id;

    public String email;

    public String content;

    public Integer getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getContent() {
        return content;
    }


    public Guestbook(Integer id, String email, String content) {
        this.id = id;
        this.email = email;
        this.content = content;
    }
    
    public Guestbook(String email, String content) {
        this.email = email;
        this.content = content;
    }

    public static class GuestbookMapper implements RowMapper<Guestbook> {

        @Override
        public Guestbook map(ResultSet r, StatementContext ctx) throws SQLException {
            return new Guestbook(r.getInt("id"), r.getString("email"), r.getString("content"));
        }
    }

}
