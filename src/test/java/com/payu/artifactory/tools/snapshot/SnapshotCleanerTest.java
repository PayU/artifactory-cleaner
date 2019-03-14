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

import com.payu.artifactory.tools.util.WrappedException;

@ExtendWith(MockitoExtension.class)
class SnapshotCleanerTest {

    private static final String SNAPSHOT_REPO = "snapshot-repo";
    private static final String RELEASE_REPO = "release-repo";

    @Mock
    private Artifactory artifactory;

    @Mock
    private RepositoryHandle repository;

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
        new SnapshotCleaner(artifactory, SNAPSHOT_REPO, RELEASE_REPO).execute();

        // then
        verify(repository).delete("/test-1.0-SNAPSHOT");
        verify(repository).delete("/test-test/test-1.4-SNAPSHOT");
        verifyNoMoreInteractions(repository);
    }

    @Test
    public void exceptionStopProcessing() throws IOException {

        when(artifactory.restCall(any(ArtifactoryRequest.class))).thenThrow(new IOException());

        //when
        Assertions.assertThrows(WrappedException.class,
                () -> new SnapshotCleaner(artifactory, SNAPSHOT_REPO, RELEASE_REPO).execute());

        verifyNoMoreInteractions(artifactory);
        verifyNoMoreInteractions(repository);
    }

    private ArtifactoryResponse aResponse(String apiUrl) throws IOException {
        switch (apiUrl) {
            case "api/storage/" + SNAPSHOT_REPO:
                return aStorageListResponse(this::aStorageListRootForSnapshot);

            case "api/storage/" + RELEASE_REPO + "/test-1.0":
                return aStorageListResponse(this::aStorageListRootForRelease);

            case "api/storage/" + RELEASE_REPO + "/test-1.1":
                return aStorageListResponse(StorageList::new);

            case "api/storage/" + SNAPSHOT_REPO + "/test-test":
                return aStorageListResponse(this::aStorageListRootForSnapshotSubItem);

            case "api/storage/" + RELEASE_REPO + "/test-test/test-1.4":
                return aStorageListResponse(this::aStorageListRootForRelease);

            default:
                throw new IllegalArgumentException("Unknown api call: " + apiUrl);
        }
    }


    private ArtifactoryResponse aStorageListResponse(Supplier<StorageList> action) throws IOException {
        ArtifactoryResponse response = mock(ArtifactoryResponse.class);
        when(response.parseBody(StorageList.class)).thenReturn(action.get());
        return response;
    }

    private StorageList aStorageListRootForSnapshot() {
        StorageList storageList = new StorageList();
        storageList.getChildren().add(new StorageChildren("/test-1.0-SNAPSHOT", true));
        storageList.getChildren().add(new StorageChildren("/test-1.1-SNAPSHOT", true));
        storageList.getChildren().add(new StorageChildren("/test-1.3-SNAPSHOT", false));
        storageList.getChildren().add(new StorageChildren("/test-1.2", false));
        storageList.getChildren().add(new StorageChildren("/test-test", true));
        storageList.getChildren().add(new StorageChildren());
        return storageList;
    }

    private StorageList aStorageListRootForSnapshotSubItem() {
        StorageList storageList = new StorageList();
        storageList.getChildren().add(new StorageChildren("/test-1.4-SNAPSHOT", true));
        return storageList;
    }

    private StorageList aStorageListRootForRelease() {
        StorageList storageList = new StorageList();
        storageList.getChildren().add(new StorageChildren("/something", false));
        return storageList;
    }

}
