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

import java.util.Date;

import org.apache.maven.artifact.versioning.ComparableVersion;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"}) // false positives
public class AQLItemRootVersion implements Comparable<AQLItemRootVersion> {

    private String path;

    private String version;

    private Date created;

    public void setPath(String path) {
        int last = path.lastIndexOf('/');

        if (last == -1) {
            throw new IllegalArgumentException("no slash character in path " + path);
        }

        this.path = path.substring(0, last);
        this.version = path.substring(last + 1);
    }

    @Override
    public int compareTo(AQLItemRootVersion o) {
        return new ComparableVersion(version).compareTo(new ComparableVersion(o.version));
    }

    public String getParentPath() {
        int last = path.lastIndexOf('/');
        if (last == -1) {
            return path;
        }
        return path.substring(0, last);
    }
}
