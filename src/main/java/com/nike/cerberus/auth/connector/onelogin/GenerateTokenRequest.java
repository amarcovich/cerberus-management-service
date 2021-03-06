/*
 * Copyright (c) 2016 Nike, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.auth.connector.onelogin;

/**
 * POJO representing a generate token request.
 */
class GenerateTokenRequest {

    private String grantType = "client_credentials";

    public String getGrantType() {
        return grantType;
    }

    public GenerateTokenRequest setGrantType(String grantType) {
        this.grantType = grantType;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GenerateTokenRequest that = (GenerateTokenRequest) o;

        return grantType != null ? grantType.equals(that.grantType) : that.grantType == null;

    }

    @Override
    public int hashCode() {
        return grantType != null ? grantType.hashCode() : 0;
    }
}
