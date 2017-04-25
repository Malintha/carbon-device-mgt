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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.List;

@ApiModel(value = "DeviceType", description = "Contains information about device type.")
public class IOTDeviceType implements Serializable {

    private static final long serialVersionUID = 1998101711L;

    private int id;

    @ApiModelProperty(name = "name", value = "Device type name", required = true)
    private String name;

    @ApiModelProperty(name = "displayName", value = "Descriptive device type name", required = true)
    private String displayName;

    @ApiModelProperty(name = "properties", value = "Properties of the device type", required = true)
    private List<IOTDeviceProperty> properties;

    @ApiModelProperty(name = "methods", value = "Methods supported by the device type", required = true)
    private List<String> methods;

    private String config;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<IOTDeviceProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<IOTDeviceProperty> properties) {
        this.properties = properties;
    }

    public List<String> getMethods() {
        return methods;
    }

    public void setMethods(List<String> methods) {
        this.methods = methods;
    }

    public String getConfig() throws JSONException {
        if (config == null) {
            config = serializeToJSONString();
        }
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public String serializeToJSONString() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        jsonObject.put("displayName", displayName);
        if (properties != null) {
            jsonObject.put("properties", properties);
        }
        if (methods != null) {
            jsonObject.put("methods", methods);
        }
        return jsonObject.toString();
    }
}
