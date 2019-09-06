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

package com.payu.artifactory.tools.docker;

import io.github.resilience4j.retry.Retry;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryRequest;
import org.jfrog.artifactory.client.impl.ArtifactoryRequestImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class DockerImagesCleaner {

    private final Artifactory artifactory;
    private final Retry retry;
    private final String repoKey;
    private final int tagsToKeep;
    private final List<Pattern> filters = new ArrayList<>();

    public DockerImagesCleaner(
            Artifactory artifactory, Retry retry, String repoKey, int tagsToKeep, String filterFile
    ) {
        this.retry = retry;
        Objects.requireNonNull(artifactory, "artifactory must be set");
        this.artifactory = artifactory;
        this.repoKey = repoKey;
        this.tagsToKeep = tagsToKeep;

        LOGGER.info("Acting upon {} repo and keeping {} newest tags", repoKey, tagsToKeep);

        if (filterFile != null) {
            LOGGER.info("Using filter file {}", filterFile);

            try (Stream<String> stream = Files.lines(Paths.get(filterFile))) {
                stream
                        .filter(l -> !l.isEmpty())
                        .filter(l -> l.charAt(0) != '#')
                        .map(l -> Pattern.compile(l))
                        .forEach(filters::add);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }

            LOGGER.info("Loaded {} filters", filters.size());
        }
    }

    @SuppressWarnings("PMD.GuardLogStatementJavaUtil") // false positive
    public void execute() {
        String itemsQuery = "items.find({\"$and\": [{\"repo\": \""
                + repoKey
                + "\"},{\"name\": \"manifest.json\"}]}).include(\"repo\",\"path\",\"name\",\"modified\")";

        LOGGER.info("Finding docker items with query: {}", itemsQuery);

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

        Map<String, List<AQLItem>> pv = items.getResults().stream().collect(
                Collectors.groupingBy(AQLItem::getPath, Collectors.toList())
        );

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);

        pv.forEach(
                (image, versions) -> {
                    versions.sort(new Comparator<AQLItem>() {
                        @Override
                        public int compare(AQLItem i1, AQLItem i2) {
                            try {
                                return format.parse(i2.getModified()).compareTo(format.parse(i1.getModified()));
                            } catch (ParseException e) {
                                throw new IllegalArgumentException(e);
                            }
                        }
                    });

                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("Processing image {}", image);
                        LOGGER.info(
                                "Newest tags:{}",
                                versions.stream()
                                        .limit(tagsToKeep)
                                        .map(i -> i.getVersion()).reduce("", (s, e) -> s + " " + e)
                        );
                    }

                    versions.stream().skip(tagsToKeep).forEach(
                            item -> {
                                if (isFiltered(item.getPath() + "/" + item.getVersion())) {
                                    LOGGER.info("Filtered {}", item.getVersion());
                                } else {
                                    deleteTag(image, item.getVersion());
                                }
                            }
                    );
                }
        );
    }

    private boolean isFiltered(String path) {
        boolean filtered = false;

        for (Pattern p : filters) {
            if (p.matcher(path).matches()) {
                filtered = true;
                break;
            }
        }

        return filtered;
    }

    private void deleteTag(String imageName, String tag) {
        LOGGER.info("Delete tag {}", tag);

        Try.of(Retry.decorateCheckedSupplier(retry,
                () -> artifactory
                        .repository(repoKey)
                        .delete(imageName + "/" + tag)));
    }
}
