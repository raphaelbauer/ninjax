/*
 * Copyright 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ninjaxframework.core;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static com.google.common.truth.Truth.assertThat;

class NinjaSessionTest {

    @Test
    void testConstructionAndGet() {
        NinjaSession session = new NinjaSession(Map.of("foo", "bar"));
        assertThat(session.get("foo")).isEqualTo(Optional.of("bar"));
        assertThat(session.get("baz")).isEmpty();
    }

    @Test
    void testOverwriteValue() {
        NinjaSession session = new NinjaSession(Map.of("key", "oldValue"));
        NinjaSession updated = session.withValue("key", "newValue");
        assertThat(updated.get("key")).isEqualTo(Optional.of("newValue"));
        assertThat(session.get("key")).isEqualTo(Optional.of("oldValue")); // original unchanged
    }

    @Test
    void testAddValue() {
        NinjaSession session = new NinjaSession();
        NinjaSession updated = session.withValue("a", "1");
        assertThat(updated.get("a")).isEqualTo(Optional.of("1"));
        assertThat(session.get("a")).isEmpty();
    }

    @Test
    void testRemoveValue() {
        NinjaSession session = new NinjaSession(Map.of("x", "y", "a", "b"));
        NinjaSession removed = session.removeValue("x");
        assertThat(removed.get("x")).isEmpty();
        assertThat(session.get("x")).isEqualTo(Optional.of("y"));
        assertThat(removed.get("a")).isEqualTo(Optional.of("b"));
    }

    @Test
    void testImmutability() {
        NinjaSession session = new NinjaSession(Map.of("foo", "bar"));
        NinjaSession updated = session.withValue("foo", "baz");
        assertThat(session).isNotSameInstanceAs(updated);
        assertThat(session.get("foo")).isEqualTo(Optional.of("bar"));
        assertThat(updated.get("foo")).isEqualTo(Optional.of("baz"));
    }

    @Test
    void testRemoveNonExistingKey() {
        NinjaSession session = new NinjaSession(Map.of("a", "b"));
        NinjaSession removed = session.removeValue("not-present");
        assertThat(removed).isEqualTo(session); // Should be equal, nothing to remove
    }
}