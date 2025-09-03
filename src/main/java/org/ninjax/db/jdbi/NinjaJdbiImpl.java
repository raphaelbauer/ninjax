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

package org.ninjax.db.jdbi;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import org.ninjax.db.jdbc.NinjaDatasource;
import org.ninjax.db.jdbc.NinjaDatasources;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

public class NinjaJdbiImpl implements NinjaJdbi {

    final Map<String, Jdbi> nameToDBIMap;

    public NinjaJdbiImpl(NinjaDatasources ninjaDatasources) {

        Map<String, Jdbi> map = new HashMap<>();

        for (NinjaDatasource ninjaDatasource : ninjaDatasources.getDatasources()) {
            Jdbi jdbi = Jdbi.create(ninjaDatasource.getDataSource());
            jdbi.installPlugin(new SqlObjectPlugin());
            map.put(ninjaDatasource.getName(), jdbi);
            
        }

        this.nameToDBIMap = ImmutableMap.copyOf(map);
    }

    @Override
    public Jdbi getJdbi(String datasourceName) {
        return nameToDBIMap.get(datasourceName);
    }

}
