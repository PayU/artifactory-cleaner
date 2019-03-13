/*
 * Copyright 2019 PayU
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.payu.artifactory.tools;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.MultipleFailuresError;

class ConfigTest {

    @BeforeEach
    public void cleanSystemProperty() {
        System.getProperties().stringPropertyNames()
                .stream()
                .filter(n -> n.startsWith("artifactory"))
                .forEach(System::clearProperty);
    }

    @Test
    public void loadConfigFromFile() {

        System.setProperty("artifactory.properties.path", "artifactory-cleaner-test.properties");
        Config config = new Config();

        assertConfig(config);
    }

    @Test
    public void loadConfigFromFileShouldBeOkIfFileNotFound() {

        System.setProperty("artifactory.properties.path", "artifactory-cleaner-test-12345.properties");
        Config config = new Config();

        assertNotNull(config);
    }

    @Test
    public void loadConfigFromProperties() {

        System.setProperty("artifactory.url", "url");
        System.setProperty("artifactory.user", "user");
        System.setProperty("artifactory.password", "password");
        System.setProperty("artifactory.docker.repo.name", "docker");
        System.setProperty("artifactory.snapshot.repo.name", "snapshot");
        System.setProperty("artifactory.release.repo.name", "release");
        Config config = new Config();

        assertConfig(config);
    }

    @Test
    public void missingProperty() {

        Config config = new Config();

        MultipleFailuresError failuresError = assertThrows(MultipleFailuresError.class,
                () -> assertConfig(config));

        assertEquals(6, failuresError.getFailures().size());
    }

    private void assertConfig(Config config) {
        assertAll(
                () -> assertNotNull(config.getArtifactoryURL()),
                () -> assertNotNull(config.getUser()),
                () -> assertNotNull(config.getPassword()),
                () -> assertTrue(config.getDockerRepository().isPresent()),
                () -> assertTrue(config.getSnapshotRepo().isPresent()),
                () -> assertTrue(config.getReleaseRepo().isPresent())
        );
    }


}
