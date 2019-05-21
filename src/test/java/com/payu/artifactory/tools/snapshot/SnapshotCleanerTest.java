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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.function.Supplier;

import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryRequest;
import org.jfrog.artifactory.client.ArtifactoryResponse;
import org.jfrog.artifactory.client.RepositoryHandle;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.resilience4j.retry.Retry;

@ExtendWith(MockitoExtension.class)
class SnapshotCleanerTest {

    private static final String SNAPSHOT_REPO = "snapshot-repo";
    private static final String RELEASE_REPO = "release-repo";

    @Mock
    private Artifactory artifactory;

    @Mock
    private RepositoryHandle repository;

    private Retry retry = Retry.ofDefaults("test");

    @Test
    public void shouldDeleteSnapshotForExistingRelease() throws IOException {

        // given
        when(artifactory.restCall(any(ArtifactoryRequest.class)))
                .then(i -> {
                    ArtifactoryRequest request = i.getArgument(0);
                    return aResponse(request.getApiUrl());
                });

        when(artifactory.repository(SNAPSHOT_REPO)).thenReturn(repository);

        //when
        new SnapshotCleaner(artifactory, retry, SNAPSHOT_REPO, RELEASE_REPO).execute();

        // then
        verify(repository).delete("/a/b/c/8-SNAPSHOT");
        verify(repository).delete("/test/1.0-SNAPSHOT");
        verify(repository).delete("/test/1.1-SNAPSHOT");
        verifyNoMoreInteractions(repository);
    }

    @Test
    public void exceptionStopProcessing() throws IOException {

        when(artifactory.restCall(any(ArtifactoryRequest.class))).thenThrow(new IOException());

        //when
        Assertions.assertThrows(IOException.class,
                () -> new SnapshotCleaner(artifactory, retry, SNAPSHOT_REPO, RELEASE_REPO).execute());

        verify(artifactory, times(3)).restCall(any(ArtifactoryRequest.class));
        verifyNoMoreInteractions(artifactory);
        verifyNoMoreInteractions(repository);
    }

    private ArtifactoryResponse aResponse(String apiUrl) throws IOException {
        switch (apiUrl) {
            case "api/search/aql":
                return aqlItemsResponse(this::aqlItemsSupplier);
            default:
                throw new IllegalArgumentException("Unknown api call: " + apiUrl);
        }
    }


    private ArtifactoryResponse aqlItemsResponse(Supplier<AQLItems> action) throws IOException {
        ArtifactoryResponse response = mock(ArtifactoryResponse.class);
        when(response.parseBody(AQLItems.class)).thenReturn(action.get());
        return response;
    }

    private AQLItems aqlItemsSupplier() {
        AQLItems items = new AQLItems();
        items.getResults().add(getItem("/test/1.0-SNAPSHOT"));
        items.getResults().add(getItem("/test/1.1-SNAPSHOT"));
        items.getResults().add(getItem("/test/1.2"));
        items.getResults().add(getItem("/test/1.3-SNAPSHOT"));
        items.getResults().add(getItem("/a/b/c/8-SNAPSHOT"));
        items.getResults().add(getItem("/a/b/c/10"));
        return items;
    }

    private AQLItem getItem(String path) {
        AQLItem item = new AQLItem();
        item.setPath(path);
        return item;
    }
}
