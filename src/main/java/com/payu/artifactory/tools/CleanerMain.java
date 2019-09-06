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

import com.payu.artifactory.tools.docker.DockerImagesCleaner;
import com.payu.artifactory.tools.snapshot.SnapshotCleaner;
import io.github.resilience4j.retry.Retry;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClientBuilder;
import org.jfrog.artifactory.client.model.Version;

import java.util.Arrays;

@Slf4j
public final class CleanerMain {

    private CleanerMain() {
    }

    private void execute() {
        Config config = new Config();

        Artifactory artifactory = ArtifactoryClientBuilder.create()
                .setUrl(config.getArtifactoryURL())
                .setUsername(config.getUser())
                .setPassword(config.getPassword())
                .build();

        Version v = artifactory.system().version();
        LOGGER.info("Artifactory version: {}, rev: {}, addons: {}", v.getVersion(), v.getRevision(), v.getAddons());

        Retry retry = config.getRetry();

        Try<Void> job1 = Try.run(
                () -> config.getSnapshotRepo().ifPresent(
                        snapshotRepo -> config.getReleaseRepo().ifPresent(
                                releaseRepo -> new SnapshotCleaner(
                                        artifactory,
                                        retry,
                                        snapshotRepo,
                                        releaseRepo
                                ).execute()
                        )
                )
        ).onFailure(e -> LOGGER.error("", e));

        Try<Void> job2 = Try.run(
                () -> config.getDockerRepository().ifPresent(
                        repo -> new DockerImagesCleaner(
                                artifactory,
                                retry,
                                repo,
                                config.getDockerTagsToKeep(),
                                config.getDockerFilterFile().orElse(null)
                        ).execute()
                )
        ).onFailure(e -> LOGGER.error("", e));

        Try.sequence(Arrays.asList(job1, job2)).get();
    }

    public static void main(String[] args) {
        new CleanerMain().execute();
    }
}
