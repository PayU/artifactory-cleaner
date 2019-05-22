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

import io.github.resilience4j.retry.Retry;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryRequest;
import org.jfrog.artifactory.client.impl.ArtifactoryRequestImpl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class SnapshotCleaner {

    private static final String SNAPSHOT = "-SNAPSHOT";

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
        String itemsQuery = getItemsQuery();

        LOGGER.info("Finding maven items with query: {}", itemsQuery);

        ArtifactoryRequest request = new ArtifactoryRequestImpl()
                .method(ArtifactoryRequest.Method.POST)
                .apiUrl("api/search/aql")
                .requestType(ArtifactoryRequest.ContentType.TEXT)
                .responseType(ArtifactoryRequest.ContentType.JSON)
                .requestBody(itemsQuery);

        AQLItems items = Try.of(
            Retry.decorateCheckedSupplier(
                retry,
                () -> artifactory
                    .restCall(request)
                    .parseBody(AQLItems.class)
            )
        ).get();

        Map<String, List<String>> pv = items.getResults().stream().collect(
            Collectors.groupingBy(AQLItem::getPath, Collectors.mapping(AQLItem::getVersion, Collectors.toList()))
        );
        pv.replaceAll((k, v) -> getSnapshotsToDelete(v));
        pv.entrySet().stream().forEach(e -> deleteSnapshots(e.getKey(), e.getValue()));
    }

    private static List<String> getSnapshotsToDelete(List<String> input) {
        List<String> result = null;

        Optional<ComparableVersion> newestRelease = input.stream()
                .filter(c -> !c.endsWith(SNAPSHOT))
                .max((v1, v2) -> new ComparableVersion(v1).compareTo(new ComparableVersion(v2)))
                .map(s -> new ComparableVersion(s));

        if (newestRelease.isPresent()) {
            ComparableVersion newest = newestRelease.get();
            result = input.stream()
                        .filter(c -> c.endsWith(SNAPSHOT) && newest.compareTo(new ComparableVersion(c)) > 0)
                        .collect(Collectors.toList());
        } else {
            result = Collections.emptyList();
        }

        return result;
    }

    private String getItemsQuery() {
        StringBuilder result = new StringBuilder(100);
        result.append("items.find({");

        if (snapshotRepo.equals(releaseRepo)) {
            result.append("\"repo\":\"").append(snapshotRepo).append('"');
        } else {
            result.append("\"$or\":[{\"repo\":\"").append(snapshotRepo);
            result.append("\",\"repo\":\"").append(releaseRepo);
            result.append("\"}]");
        }

        result.append(",\"name\":{\"$match\":\"*.pom\"}}).include(\"repo\",\"path\",\"name\")");
        return result.toString();
    }

    private void deleteSnapshots(String path, List<String> versions) {
        for (String version: versions) {
            String fp = path + "/" + version;
            LOGGER.info("Delete: {}/{}", snapshotRepo, fp);
            artifactory.repository(snapshotRepo).delete(fp);
        }
    }
}
