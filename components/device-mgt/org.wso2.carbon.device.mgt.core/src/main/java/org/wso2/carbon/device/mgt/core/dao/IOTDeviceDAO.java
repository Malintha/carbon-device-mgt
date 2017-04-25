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
package org.wso2.carbon.device.mgt.core.dao;

import org.wso2.carbon.device.mgt.common.iot.IOTDevice;
import org.wso2.carbon.device.mgt.common.iot.IOTDeviceType;

import java.util.List;

public interface IOTDeviceDAO {

    void addIoTDeviceType(IOTDeviceType deviceType, int providerTenantId, boolean isSharedWithAllTenants)
            throws DeviceManagementDAOException;

    List<String> getIoTDeviceTypeNames(int tenantId) throws DeviceManagementDAOException;

    IOTDeviceType getIoTDeviceType(String deviceTypeName, int tenantId) throws DeviceManagementDAOException;

    IOTDevice addIoTDevice(IOTDevice device, int tenantId) throws DeviceManagementDAOException;

    IOTDevice addIoTDeviceProperties(IOTDevice device) throws DeviceManagementDAOException;

    List<IOTDevice> getIoTDevices(int tenantId) throws DeviceManagementDAOException;

    IOTDevice getIoTDevice(String deviceIdentifier, int tenantId) throws DeviceManagementDAOException;

    void populateProperties(IOTDevice device) throws DeviceManagementDAOException;
}
