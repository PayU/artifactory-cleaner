/*
 * Copyright 2023 PayU
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

package com.payu.artifactory.tools.releases;

import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClientBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class ReleasesCleanerTest {

    @Test
    @Disabled("for manual testing")
    void manual() {

        Artifactory artifactory = ArtifactoryClientBuilder.create()
                .setUrl("https://artifactory....")
                .setUsername("uuu")
                .setPassword("ppp")
                .build();

        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(1)
                .build();

        Retry retry = Retry.of("id", retryConfig);

        retry.getEventPublisher()
                .onRetry(e -> LOGGER.warn("Retry attempt: #" + e.getNumberOfRetryAttempts(), e.getLastThrowable()));

        ReleasesCleaner releasesCleaner = new ReleasesCleaner(artifactory, retry,
                "releases-local:com/example/app1");
        releasesCleaner.execute();

    }
}
