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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryRequest;
import org.jfrog.artifactory.client.impl.ArtifactoryRequestImpl;

import io.github.resilience4j.retry.Retry;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;

/**
 * Clean old released artifacts for given root from Artifactory.
 */
@Slf4j
public class ReleasesCleaner {

    private final Artifactory artifactory;
    private final Retry retry;
    private final String repo;

    private final String root;

    private final int minDays;
    private final int minRemain;
    private final int limit;

    public ReleasesCleaner(Artifactory artifactory, Retry retry, String config) {
        this.artifactory = artifactory;
        this.retry = retry;

        String[] configItems = config.split(":");
        this.repo = configItems[0];
        this.root = configItems[1];

        this.minDays = Integer.parseInt(safeGet(configItems, 2, "365"));
        this.minRemain = Integer.parseInt(safeGet(configItems, 3, "3"));
        this.limit = Integer.parseInt(safeGet(configItems, 4, "128"));
    }

    private String safeGet(String[] array, int index, String defaultValue) {
        return index < array.length ? array[index] : defaultValue;
    }

    public void execute() {
        List<AQLItemRootVersion> items = getAllVersions();

        // remove last remaining elements
        for (int i = 0; i < minRemain; i++) {
            items.remove(items.size() - 1);
        }

        // remove newer items
        Date minData = Date.from(LocalDateTime.now().minusDays(minDays).atZone(ZoneId.systemDefault()).toInstant());
        items.removeIf(item -> minData.before(item.getCreated()));

        if (items.isEmpty()) {
            LOGGER.info("There are no matching versions to remove for {}/{}", repo, root);
            return;
        }

        LOGGER.info("{} versions for deleting for: {},{}", items.size(), repo, root);

        items.stream().limit(limit).forEach(this::deleteVersion);
    }

    private <T> T executeQuery(String query, Class<? extends T> returnClass) {

        ArtifactoryRequest request = new ArtifactoryRequestImpl()
                .method(ArtifactoryRequest.Method.POST)
                .apiUrl("api/search/aql")
                .requestType(ArtifactoryRequest.ContentType.TEXT)
                .responseType(ArtifactoryRequest.ContentType.JSON)
                .requestBody(query);

        return Try.of(
                Retry.decorateCheckedSupplier(
                        retry,
                        () -> artifactory
                                .restCall(request)
                                .parseBody(returnClass)
                )
        ).get();
    }

    private List<AQLItemRootVersion> getAllVersions() {
        String itemsQueryForAllVersion = getItemsQueryForAllVersion();
        LOGGER.info("Finding versions items with query: {}", itemsQueryForAllVersion);

        AQLItemsRootVersion items = executeQuery(itemsQueryForAllVersion, AQLItemsRootVersion.class);
        return items.getResults().stream().sorted().collect(Collectors.toList());
    }

    @SuppressWarnings("PMD")
    private String getItemsQueryForAllVersion() {
        StringBuilder result = new StringBuilder();
        result.append("items.find(");

        result.append("{\"repo\":\"").append(repo).append("\"},");
        result.append("{\"path\": {\"$match\":\"").append(root).append("/*\"}},");
        result.append("{\"name\" : {\"$match\":\"*.pom\"}}");

        result.append(")");
        result.append(".include(\"repo\", \"path\", \"name\", \"created\")");

        return result.toString();
    }

    @SuppressWarnings("PMD")
    private String getItemsQueryForVersion(AQLItemRootVersion version) {
        StringBuilder result = new StringBuilder();
        result.append("items.find(");

        result.append("{\"repo\":\"").append(repo).append("\"},");
        result.append("{\"path\": {\"$match\":\"").append(version.getParentPath()).append("/*\"}},");
        result.append("{\"name\" : {\"$match\":\"*-").append(version.getVersion()).append(".pom\"}}");

        result.append(")");
        result.append(".include(\"repo\", \"path\", \"name\", \"created\")");

        return result.toString();
    }

    private void deleteVersion(AQLItemRootVersion version) {

        LOGGER.info("");
        LOGGER.info("*****");
        LOGGER.info("Delete items from {}/{} for version {} created at {}",
                repo, version.getParentPath(), version.getVersion(), version.getCreated());

        List<AQLItemPath> itemPaths = getItemsForVersion(version);
        itemPaths.forEach(this::deletePath);

        LOGGER.info("*****");
    }

    private List<AQLItemPath> getItemsForVersion(AQLItemRootVersion version) {
        String itemsQueryForVersion = getItemsQueryForVersion(version);
        LOGGER.info("Finding items with query: {}", itemsQueryForVersion);

        AQLItemsPath itemsPath = executeQuery(itemsQueryForVersion, AQLItemsPath.class);
        return itemsPath.getResults().stream().sorted().collect(Collectors.toList());
    }

    private void deletePath(AQLItemPath path) {
        LOGGER.info("Delete {}/{}", repo, path.getPath());
        artifactory.repository(repo).delete(path.getPath());
    }
}
