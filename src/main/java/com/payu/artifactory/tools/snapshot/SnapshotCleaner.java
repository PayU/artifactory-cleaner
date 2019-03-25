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

package com.payu.artifactory.tools.snapshot;

import static org.jfrog.artifactory.client.ArtifactoryRequest.Method.GET;

import java.util.Objects;

import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.impl.ArtifactoryRequestImpl;

import io.github.resilience4j.retry.Retry;
import io.vavr.CheckedFunction0;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SnapshotCleaner {

    private final Artifactory artifactory;
    private final Retry retry;
    private final String snapshotRepo;
    private final String releaseRepo;

    public SnapshotCleaner(Artifactory artifactory, Retry retry, String snapshotRepo, String releaseRepo) {
        Objects.requireNonNull(artifactory, "artifactory must be set");
        Objects.requireNonNull(retry, "retry must be set");
        this.artifactory = artifactory;
        this.retry = retry;
        this.snapshotRepo = snapshotRepo;
        this.releaseRepo = releaseRepo;
    }


    public void execute() {
        walk("");
    }

    private StorageList getStorageList(String repo, String path) {

        ArtifactoryRequestImpl request = new ArtifactoryRequestImpl()
                .apiUrl("api/storage/" + repo + path)
                .method(GET);

        CheckedFunction0<StorageList> supplier = Retry.decorateCheckedSupplier(retry,
                () -> artifactory.restCall(request).parseBody(StorageList.class));

        return Try.of(supplier).get();
    }

    private void walk(String path) {

        LOGGER.info("Walk in: {}{}", snapshotRepo, path);

        StorageList storageList = getStorageList(snapshotRepo, path);

        storageList.getChildren().parallelStream()
                .filter(StorageChildren::isFolder)
                .filter(c -> c.getUri().endsWith("-SNAPSHOT"))
                .forEach(c -> checkSnapshot(path + c.getUri()));

        storageList.getChildren().stream()
                .filter(StorageChildren::isFolder)
                .filter(c -> !c.getUri().endsWith("-SNAPSHOT"))
                .forEach(c -> walk(path + c.getUri()));
    }

    private void checkSnapshot(String path) {
        StorageList storageList = getStorageList(releaseRepo, path.replace("-SNAPSHOT", ""));

        if (!storageList.getChildren().isEmpty()) {
            LOGGER.info("Delete: {}{}", snapshotRepo, path);
            artifactory.repository(snapshotRepo).delete(path);
        }
    }
}
