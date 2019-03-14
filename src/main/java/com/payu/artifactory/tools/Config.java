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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Config {

    private static final String DEFAULT_PROPERTIES_FILE_PATH = "artifactory-cleaner.properties";

    private final Properties properties;

    public Config() {

        properties = new Properties();

        String path = System.getProperty("artifactory.properties.path", DEFAULT_PROPERTIES_FILE_PATH);
        InputStream inputStream = getClass().getResourceAsStream("/" + path);
        if (inputStream == null) {
            try {
                inputStream = new FileInputStream(path);
            } catch (FileNotFoundException e) {
                LOGGER.warn(e.getMessage());
                inputStream = null;
            }
        }
        if (inputStream != null) {
            try {
                properties.load(inputStream);
            } catch (IOException e) {
                LOGGER.warn(e.getMessage());
            }
        } else {
            LOGGER.warn("File {} not found, configuration from system properties", path);
        }
        LOGGER.info("Config loaded");
    }


    private Optional<String> getProperty(String key) {
        return Optional.ofNullable(System.getProperty(key, properties.getProperty(key)));
    }

    private String getRequiredProperty(String key) {
        return getProperty(key).orElseThrow(
                () -> new IllegalArgumentException("configuration property: " + key + " must be defined"));
    }

    public String getArtifactoryURL() {
        return getRequiredProperty("artifactory.url");
    }

    public String getUser() {
        return getRequiredProperty("artifactory.user");
    }

    public String getPassword() {
        return getRequiredProperty("artifactory.password");
    }

    public Optional<String> getDockerRepository() {
        return getProperty("artifactory.docker.repo.name");
    }

    public Optional<String> getSnapshotRepo() {
        return getProperty("artifactory.snapshot.repo.name");
    }

    public Optional<String> getReleaseRepo() {
        return getProperty("artifactory.release.repo.name");
    }
}
