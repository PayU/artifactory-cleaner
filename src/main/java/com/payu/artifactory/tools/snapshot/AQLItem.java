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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@EqualsAndHashCode
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"}) // false positives
public class AQLItem {

    private String path;

    private String version;

    public void setPath(String path) {
        int last = path.lastIndexOf('/');

        if (last == -1) {
            throw new IllegalArgumentException("no slash character in path " + path);
        }

        this.path = path.substring(0, last);
        this.version = path.substring(last + 1);
    }
}
