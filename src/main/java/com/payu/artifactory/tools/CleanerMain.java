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

import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClientBuilder;
import org.jfrog.artifactory.client.model.Version;

import com.payu.artifactory.tools.docker.DockerImagesCleaner;
import com.payu.artifactory.tools.snapshot.SnapshotCleaner;

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

        Version version = artifactory.system().version();
        LOGGER.info("Artifactory version: {}, rev: {}, addons: {}", version.getVersion(), version.getRevision(),
                version.getAddons());

        config.getSnapshotRepo().ifPresent(snapshotRepo -> config.getReleaseRepo().ifPresent(
                releaseRepo -> new SnapshotCleaner(artifactory, snapshotRepo, releaseRepo).execute()));

        config.getDockerRepository().ifPresent(
                dockerRepository -> new DockerImagesCleaner(artifactory, dockerRepository).execute());
    }


    public static void main(String[] args) {
        new CleanerMain().execute();
    }
}
