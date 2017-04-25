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
package org.wso2.carbon.device.mgt.core.service;

import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.device.mgt.common.DeviceManagementException;
import org.wso2.carbon.device.mgt.common.TransactionManagementException;
import org.wso2.carbon.device.mgt.common.iot.IOTDevice;
import org.wso2.carbon.device.mgt.common.iot.IOTDeviceType;
import org.wso2.carbon.device.mgt.core.dao.DeviceManagementDAOException;
import org.wso2.carbon.device.mgt.core.dao.DeviceManagementDAOFactory;
import org.wso2.carbon.device.mgt.core.dao.IOTDeviceDAO;

import java.util.List;
import java.util.UUID;

public class IOTDeviceManagementCoreServiceImpl implements IOTDeviceManagementCoreService {

    private IOTDeviceDAO iotDeviceDAO;

    public IOTDeviceManagementCoreServiceImpl() {
        iotDeviceDAO = DeviceManagementDAOFactory.getIOTDeviceDAO();
    }

    public List<String> getIoTDeviceTypeNames() throws DeviceManagementException {
        List<String> deviceTypeNames = null;
        try {
            DeviceManagementDAOFactory.beginTransaction();
            deviceTypeNames = iotDeviceDAO.getIoTDeviceTypeNames(getTenantId());
        } catch (DeviceManagementDAOException | TransactionManagementException e) {
            DeviceManagementDAOFactory.rollbackTransaction();
            String msg = "Failed to get IoT device types.";
            throw new DeviceManagementException(msg, e);
        } finally {
            DeviceManagementDAOFactory.commitTransaction();
        }
        return deviceTypeNames;
    }

    @Override
    public void addIoTDeviceType(IOTDeviceType deviceTypeInfo) throws DeviceManagementException {
        try {
            DeviceManagementDAOFactory.beginTransaction();
            iotDeviceDAO.addIoTDeviceType(deviceTypeInfo, getTenantId(), false);
        } catch (DeviceManagementDAOException | TransactionManagementException e) {
            DeviceManagementDAOFactory.rollbackTransaction();
            String msg = "Failed to add device type: " + deviceTypeInfo.getName();
            throw new DeviceManagementException(msg, e);
        } finally {
            DeviceManagementDAOFactory.commitTransaction();
        }
    }

    @Override
    public IOTDevice addIoTDevice(IOTDevice device) throws DeviceManagementException {
        try {
            DeviceManagementDAOFactory.beginTransaction();
            IOTDeviceType deviceType = iotDeviceDAO.getIoTDeviceType(device.getDeviceTypeName(), getTenantId());
            if (deviceType == null) {
                String msg = "Device type " + device.getDeviceTypeName() + " is not registered for tenant " + getTenantId();
                throw new DeviceManagementException(msg);
            }

            // device owners can specify an unique id for their devices. if it is not specified, an UUID will be added as the id.
            if (device.getDeviceId() == null || device.getDeviceId().isEmpty()) {
                device.setDeviceId(UUID.randomUUID().toString());
            }
            device.setDeviceTypeId(deviceType.getId());
            device = iotDeviceDAO.addIoTDevice(device, getTenantId());
            iotDeviceDAO.addIoTDeviceProperties(device);
            return device;

        } catch (DeviceManagementDAOException | TransactionManagementException e) {
            DeviceManagementDAOFactory.rollbackTransaction();
            String msg = "Failed to add device type: " + device.getName();
            throw new DeviceManagementException(msg, e);
        } finally {
            DeviceManagementDAOFactory.commitTransaction();
        }
    }

    @Override
    public List<IOTDevice> getIoTDevices() throws DeviceManagementException {
        try {
            DeviceManagementDAOFactory.beginTransaction();
            List<IOTDevice> devices = iotDeviceDAO.getIoTDevices(getTenantId());
            return devices;
        } catch (DeviceManagementDAOException | TransactionManagementException e) {
            DeviceManagementDAOFactory.rollbackTransaction();
            String msg = "Failed to get devices of tenant: " + getTenantId();
            throw new DeviceManagementException(msg, e);
        } finally {
            DeviceManagementDAOFactory.commitTransaction();
        }
    }

    @Override
    public IOTDevice getIoTDevice(String identifier) throws DeviceManagementException {
        try {
            DeviceManagementDAOFactory.beginTransaction();
            IOTDevice device = iotDeviceDAO.getIoTDevice(identifier, getTenantId());
            iotDeviceDAO.populateProperties(device);
            return device;
        } catch (DeviceManagementDAOException | TransactionManagementException e) {
            DeviceManagementDAOFactory.rollbackTransaction();
            String msg = "Failed to get device " + identifier + " of tenant: " + getTenantId();
            throw new DeviceManagementException(msg, e);
        } finally {
            DeviceManagementDAOFactory.commitTransaction();
        }
    }

    private int getTenantId() {
        return CarbonContext.getThreadLocalCarbonContext().getTenantId();
    }
}
