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
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryRequest;
import org.jfrog.artifactory.client.ArtifactoryResponse;
import org.jfrog.artifactory.client.RepositoryHandle;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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

    private Retry retry = Retry.ofDefaults("id");

    @Test
    public void snapshotShouldBeDeletedForExistingReleaseVersion() throws IOException {

        // given
        doReturn(aqlItemsSupplier()).when(response).parseBody(AQLItems.class);

        when(artifactory.restCall(any(ArtifactoryRequest.class))).thenReturn(response);

        when(artifactory.repository(TEST_REPO)).thenReturn(repository);

        // when
        new DockerImagesCleaner(artifactory, retry, TEST_REPO, 2, null).execute();

        // then
        verify(repository).delete(PAYU_TEST_IMAGE + "/1.1");
        verify(repository).delete(PAYU_TEST_IMAGE + "/1.2");
        verifyNoMoreInteractions(artifactory);
        verifyNoMoreInteractions(response);
        verifyNoMoreInteractions(repository);
    }

    @Test
    public void exceptionStopProcessing() throws IOException {

        // givem
        when(artifactory.restCall(any(ArtifactoryRequest.class))).thenThrow(new IOException());

        //when
        Assertions.assertThrows(IOException.class,
                () -> new DockerImagesCleaner(artifactory, retry, TEST_REPO, 4, null).execute());

        // then
        verify(artifactory, times(3)).restCall(any(ArtifactoryRequest.class));
        verifyNoMoreInteractions(artifactory);
        verifyNoMoreInteractions(response);
        verifyNoMoreInteractions(repository);
    }

    private AQLItems aqlItemsSupplier() {
        AQLItems items = new AQLItems();
        items.getResults().add(getItem(PAYU_TEST_IMAGE + "/1.1", "2000-05-05T16:44:30.629+02:00"));
        items.getResults().add(getItem(PAYU_TEST_IMAGE + "/1.2", "2001-05-05T16:44:30.629+02:00"));
        items.getResults().add(getItem(PAYU_TEST_IMAGE + "/1.3", "2002-05-05T16:44:30.629+02:00"));
        items.getResults().add(getItem(PAYU_TEST_IMAGE + "/1.4", "2003-05-05T16:44:30.629+02:00"));
        items.getResults().add(getItem("abcd/1.1", "1998-05-05T16:44:30.629+02:00"));
        items.getResults().add(getItem("abcd/1.2", "1999-05-05T16:44:30.629+02:00"));
        return items;
    }

    private AQLItem getItem(String path, String modified) {
        AQLItem item = new AQLItem();
        item.setPath(path);
        item.setModified(modified);
        return item;
    }
}
