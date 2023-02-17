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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClientBuilder;
import org.jfrog.artifactory.client.model.Version;

import com.payu.artifactory.tools.docker.DockerImagesCleaner;
import com.payu.artifactory.tools.releases.ReleasesCleaner;
import com.payu.artifactory.tools.snapshot.SnapshotCleaner;

import io.github.resilience4j.retry.Retry;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;

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

        List<Try<Void>> jobs = new ArrayList<>();

        jobs.add(Try.run(
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
        ).onFailure(e -> LOGGER.error("", e)));

        jobs.add(Try.run(
                () -> config.getDockerRepository().ifPresent(
                        repo -> new DockerImagesCleaner(
                                artifactory,
                                retry,
                                repo,
                                config.getDockerTagsToKeep(),
                                config.getDockerFilterFile().orElse(null)
                        ).execute()
                )
        ).onFailure(e -> LOGGER.error("", e)));

        config.getReleaseCleanConfigs()
                .orElseGet(Collections::emptyList).stream()
                .map(relConfig -> new ReleasesCleaner(artifactory, retry, relConfig))
                .map(cleaner -> Try.run(cleaner::execute).onFailure(e -> LOGGER.error("", e)))
                .forEach(jobs::add);

        Try.sequence(jobs).get();
    }

    public static void main(String[] args) {
        new CleanerMain().execute();
    }
}
