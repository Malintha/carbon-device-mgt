/*
 * Copyright 2005-2015 WSO2, Inc. (http://wso2.com)
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
package org.wso2.carbon.device.mgt.jaxrs.beans.iot;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "IoTDeviceCredentials", description = "Credentials of IoT device.")
public class IOTDeviceCredentials {

    @ApiModelProperty(name = "deviceIdentifier", value = "Device ID", required = true)
    private String deviceIdentifier;

    @ApiModelProperty(name = "accessToken", value = "Access token", required = true)
    private String accessToken;

    @ApiModelProperty(name = "refreshToken", value = "Refresh token", required = true)
    private String refreshToken;

    @ApiModelProperty(name = "expiry", value = "Expiry date of the token", required = true)
    private long expiry;

    @ApiModelProperty(name = "consumerKey", value = "Application key", required = true)
    private String consumerKey;

    @ApiModelProperty(name = "consumerSecret", value = "Application secret", required = true)
    private String consumerSecret;

    public String getDeviceIdentifier() {
        return deviceIdentifier;
    }

    public void setDeviceIdentifier(String deviceIdentifier) {
        this.deviceIdentifier = deviceIdentifier;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public long getExpiry() {
        return expiry;
    }

    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }

    public void setConsumerSecret(String consumerSecret) {
        this.consumerSecret = consumerSecret;
    }
}
