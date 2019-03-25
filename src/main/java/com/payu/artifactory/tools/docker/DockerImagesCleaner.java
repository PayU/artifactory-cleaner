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

import static org.jfrog.artifactory.client.ArtifactoryRequest.Method.GET;

import java.util.Objects;

import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.impl.ArtifactoryRequestImpl;

import io.github.resilience4j.retry.Retry;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DockerImagesCleaner {

    private final Artifactory artifactory;
    private final Retry retry;
    private final String repoKey;

    public DockerImagesCleaner(Artifactory artifactory, Retry retry, String repoKey) {
        this.retry = retry;
        Objects.requireNonNull(artifactory, "artifactory must be set");
        this.artifactory = artifactory;
        this.repoKey = repoKey;
    }

    public void execute() {

        ArtifactoryRequestImpl request = new ArtifactoryRequestImpl()
                .apiUrl("api/docker/" + repoKey + "/v2/_catalog")
                .method(GET);

        Try.of(Retry.decorateCheckedSupplier(retry,
                () -> artifactory.restCall(request).parseBody(DockerImageList.class)))
                .get()
                .forEach(this::cleanDockerTags);
    }


    private void cleanDockerTags(String imageName) {

        LOGGER.info("Clean tags in image: {}", imageName);

        ArtifactoryRequestImpl request = new ArtifactoryRequestImpl()
                .apiUrl("api/docker/" + repoKey + "/v2/" + imageName + "/tags/list")
                .method(GET);

        DockerImageTagList tagList = Try.of(Retry.decorateCheckedSupplier(retry,
                () -> artifactory.restCall(request).parseBody(DockerImageTagList.class)))
                .get();

        tagList.parallelStream()
                .filter(tag -> tag.endsWith("-SNAPSHOT"))
                .filter(tagList::containsReleaseForSnapshot)
                .forEach(tag -> deleteTag(imageName, tag));

    }

    private void deleteTag(String imageName, String tag) {
        LOGGER.info("Delete tag: {} from image: {}", tag, imageName);

        Try.of(Retry.decorateCheckedSupplier(retry,
                () -> artifactory.repository(repoKey).delete(imageName + "/" + tag)));
    }
}
