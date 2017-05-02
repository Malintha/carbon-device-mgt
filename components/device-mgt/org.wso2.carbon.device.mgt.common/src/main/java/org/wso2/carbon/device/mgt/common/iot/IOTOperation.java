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
package org.wso2.carbon.device.mgt.common.iot;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.json.JSONObject;

@ApiModel(value = "IOTOperation", description = "Contains information about device type.")
public class IOTOperation {

    private int id;

    private int deviceId;

    @ApiModelProperty(name = "deviceIdentifier", value = "Device identifier", required = true)
    private String deviceIdentifier;

    @ApiModelProperty(name = "operationName", value = "Operation name", required = true)
    private String operationName;

    @ApiModelProperty(name = "payload", value = "Payload of the operation containing the method name and parameters.", required = true)
    private String payload;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceIdentifier() {
        return deviceIdentifier;
    }

    public void setDeviceIdentifier(String deviceIdentifier) {
        this.deviceIdentifier = deviceIdentifier;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("deviceId", deviceId);
        o.put("deviceIdentifier", deviceIdentifier);
        o.put("operationName", operationName);
        o.put("payload", payload);
        return o.toString();
    }
}
