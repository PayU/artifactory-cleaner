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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;

import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryRequest;
import org.jfrog.artifactory.client.ArtifactoryResponse;
import org.jfrog.artifactory.client.RepositoryHandle;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.payu.artifactory.tools.util.WrappedException;

@ExtendWith(MockitoExtension.class)
class DockerImagesCleanerTest {

    private static final String TEST_REPO = "test-repo";
    private static final String PAYU_TEST_IMAGE = "payu/test-image";

    @Mock
    private Artifactory artifactory;

    @Mock
    private ArtifactoryResponse response;

    @Mock
    private RepositoryHandle repository;

    @Test
    public void snapshotShouldBeDeletedForExistingReleaseVersion() throws IOException {


        // given

        doReturn(aDockerImageList()).when(response).parseBody(DockerImageList.class);
        doReturn(aDockerImageTagList()).when(response).parseBody(DockerImageTagList.class);

        when(artifactory.restCall(any(ArtifactoryRequest.class))).thenReturn(response);

        when(artifactory.repository(TEST_REPO)).thenReturn(repository);

        // when
        new DockerImagesCleaner(artifactory, TEST_REPO).execute();

        // then
        verify(repository).delete(PAYU_TEST_IMAGE + "/1.0-SNAPSHOT");
        verify(repository).delete(PAYU_TEST_IMAGE + "/1.2-SNAPSHOT");
        verifyNoMoreInteractions(artifactory);
        verifyNoMoreInteractions(response);
        verifyNoMoreInteractions(repository);
    }

    @Test
    public void exceptionStopProcessing() throws IOException {

        // givem
        when(artifactory.restCall(any(ArtifactoryRequest.class))).thenThrow(new IOException());

        //when
        Assertions.assertThrows(WrappedException.class, () -> new DockerImagesCleaner(artifactory, TEST_REPO).execute());

        // then
        verifyNoMoreInteractions(artifactory);
        verifyNoMoreInteractions(response);
        verifyNoMoreInteractions(repository);
    }

    private DockerImageTagList aDockerImageTagList() {
        DockerImageTagList dockerImageTagList = new DockerImageTagList();
        dockerImageTagList.getTags().addAll(
                Arrays.asList("1.0", "1.0-SNAPSHOT", "1.1", "1.2-SNAPSHOT", "1.3-SNAPSHOT", "1.2"));
        return dockerImageTagList;

    }

    private DockerImageList aDockerImageList() {
        DockerImageList dockerImageList = new DockerImageList();
        dockerImageList.getRepositories().add(PAYU_TEST_IMAGE);
        return dockerImageList;
    }
}
