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
package org.wso2.carbon.device.mgt.common.device.details;

import org.json.JSONObject;
import org.wso2.carbon.device.mgt.common.iot.IOTDeviceProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestJSON {

    public static void main(String[] args) {

        JSONObject o = new JSONObject();
        o.put("name", "device1");
        List<IOTDeviceProperty> s = new ArrayList<>();
        s.add(new IOTDeviceProperty("test1", "string"));
        s.add(new IOTDeviceProperty("test2", "int"));
        Map<String, String> props = new HashMap<>();
        props.put("p1", "v1");
        props.put("p2", "v2");
        props.put("p3", "v3");
        o.put("properties", s);

        System.out.println(o.toString());
    }
}
