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
package org.wso2.carbon.device.mgt.core.dao.impl;

import org.json.JSONException;
import org.wso2.carbon.device.mgt.common.iot.IOTDevice;
import org.wso2.carbon.device.mgt.common.iot.IOTDeviceType;
import org.wso2.carbon.device.mgt.core.dao.DeviceManagementDAOException;
import org.wso2.carbon.device.mgt.core.dao.DeviceManagementDAOFactory;
import org.wso2.carbon.device.mgt.core.dao.IOTDeviceDAO;
import org.wso2.carbon.device.mgt.core.dao.util.DeviceManagementDAOUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IOTDeviceDAOImpl implements IOTDeviceDAO {

    @Override
    public void addIoTDeviceType(IOTDeviceType deviceType, int providerTenantId, boolean isSharedWithAllTenants)
            throws DeviceManagementDAOException {
        Connection conn;
        PreparedStatement stmt = null;
        try {
            conn = this.getConnection();
            stmt = conn.prepareStatement(
                    "INSERT INTO DM_IOT_DEVICE_TYPE (NAME,PROVIDER_TENANT_ID,SHARED_WITH_ALL_TENANTS,CONFIG) VALUES (?,?,?,?)");
            stmt.setString(1, deviceType.getName());
            stmt.setInt(2, providerTenantId);
            stmt.setBoolean(3, isSharedWithAllTenants);
            stmt.setString(4, deviceType.getConfig());
            stmt.execute();
        } catch (SQLException | JSONException e) {
            throw new DeviceManagementDAOException(
                    "Error occurred while registering the IoT device type '" + deviceType.getName() + "'", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, null);
        }
    }

    @Override
    public List<String> getIoTDeviceTypeNames(int tenantId) throws DeviceManagementDAOException {
        Connection conn;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<String> deviceTypes = new ArrayList<>();
        try {
            conn = this.getConnection();
            String sql =
                    "SELECT NAME FROM DM_IOT_DEVICE_TYPE where PROVIDER_TENANT_ID = ? OR SHARED_WITH_ALL_TENANTS = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, tenantId);
            stmt.setBoolean(2, true);
            rs = stmt.executeQuery();
            while (rs.next()) {
                deviceTypes.add(rs.getString("NAME"));
            }
            return deviceTypes;
        } catch (SQLException e) {
            throw new DeviceManagementDAOException("Error occurred while fetching the registered device types", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, rs);
        }
    }

    @Override
    public IOTDeviceType getIoTDeviceType(String deviceTypeName, int tenantId) throws DeviceManagementDAOException {
        Connection conn;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            IOTDeviceType deviceTypeInfo = null;
            conn = this.getConnection();
            String sql =
                    "SELECT ID, CONFIG FROM DM_IOT_DEVICE_TYPE where NAME = ? AND (PROVIDER_TENANT_ID = ? OR SHARED_WITH_ALL_TENANTS = ?)";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, deviceTypeName);
            stmt.setInt(2, tenantId);
            stmt.setBoolean(3, true);
            rs = stmt.executeQuery();
            if (rs.next()) {
                deviceTypeInfo = new IOTDeviceType();
                deviceTypeInfo.setId(rs.getInt("ID"));
                deviceTypeInfo.setName(deviceTypeName);
                deviceTypeInfo.setConfig(rs.getString("CONFIG"));
            }
            return deviceTypeInfo;
        } catch (SQLException e) {
            throw new DeviceManagementDAOException("Error occurred while fetching the registered device types", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, rs);
        }
    }

    @Override
    public IOTDevice addIoTDevice(IOTDevice device, int tenantId) throws DeviceManagementDAOException {
        Connection conn;
        PreparedStatement stmt = null;
        PreparedStatement propStmt = null;
        ResultSet rs = null;
        try {
            conn = this.getConnection();
            stmt = conn.prepareStatement(
                    "INSERT INTO DM_IOT_DEVICE (DEVICE_ID, NAME, DEVICE_TYPE_ID, DESCRIPTION, TENANT_ID, LAST_UPDATED) VALUES (?,?,?,?,?,?)");
            stmt.setString(1, device.getDeviceId());
            stmt.setString(2, device.getName());
            stmt.setInt(3, device.getDeviceTypeId());
            stmt.setString(4, device.getDescription());
            stmt.setInt(5, tenantId);
            stmt.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
            stmt.execute();

            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                device.setId(rs.getInt(1));
            }
            return device;

        } catch (SQLException | JSONException e) {
            throw new DeviceManagementDAOException(
                    "Error occurred while registering the IoT device '" + device.getName() + "'", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, null);
        }
    }

    @Override
    public IOTDevice addIoTDeviceProperties(IOTDevice device) throws DeviceManagementDAOException {
        Connection conn;
        PreparedStatement stmt = null;
        try {
            if (device.getProperties() != null) {
                conn = this.getConnection();
                stmt = conn.prepareStatement("INSERT INTO DM_IOT_DEVICE_PROPERTY (DEVICE_ID, PROP_NAME, PROP_VALUE) VALUES (?,?,?)");
                Map<String, String> props = device.getProperties();
                for (String propName : props.keySet()) {
                    String propValue = props.get(propName);
                    stmt.setInt(1, device.getId());
                    stmt.setString(2, propName);
                    stmt.setString(3, propValue);
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
            return device;
        } catch (SQLException | JSONException e) {
            throw new DeviceManagementDAOException(
                    "Error occurred while persisting properties of IoT device '" + device.getName() + "'", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, null);
        }
    }

    @Override
    public List<IOTDevice> getIoTDevices(int tenantId) throws DeviceManagementDAOException {
        List<IOTDevice> devices = new ArrayList<>();
        Connection conn;
        PreparedStatement stmt = null;
        try {
            conn = this.getConnection();
            stmt = conn.prepareStatement(
                    "SELECT D.ID AS ID, D.DEVICE_ID AS DEVICE_ID, D.NAME AS NAME, D.DEVICE_TYPE_ID AS DEVICE_TYPE_ID, D.DESCRIPTION AS DESCRIPTION, DT.NAME AS DEVICE_TYPE_NAME " +
                            "FROM DM_IOT_DEVICE D, DM_IOT_DEVICE_TYPE DT " +
                            "WHERE D.DEVICE_TYPE_ID = DT.ID AND D.TENANT_ID = ?");
            stmt.setInt(1, tenantId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                IOTDevice device = new IOTDevice();
                device.setId(rs.getInt("ID"));
                device.setDeviceId(rs.getString("DEVICE_ID"));
                device.setName(rs.getString("NAME"));
                device.setDeviceTypeId(rs.getInt("DEVICE_TYPE_ID"));
                device.setDeviceTypeName(rs.getString("DEVICE_TYPE_NAME"));
                device.setDescription(rs.getString("DESCRIPTION"));
                devices.add(device);
            }
        } catch (SQLException | JSONException e) {
            throw new DeviceManagementDAOException(
                    "Error occurred while fetching devices of tenant '" + tenantId + "'", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, null);
        }
        return devices;
    }

    @Override
    public IOTDevice getIoTDevice(String deviceIdentifier, int tenantId) throws DeviceManagementDAOException {
        IOTDevice device = null;
        Connection conn;
        PreparedStatement stmt = null;
        try {
            conn = this.getConnection();
            stmt = conn.prepareStatement(
                    "SELECT D.ID AS ID, D.DEVICE_ID AS DEVICE_ID, D.NAME AS NAME, D.DEVICE_TYPE_ID AS DEVICE_TYPE_ID, D.DESCRIPTION AS DESCRIPTION, DT.NAME AS DEVICE_TYPE_NAME " +
                            "FROM DM_IOT_DEVICE D, DM_IOT_DEVICE_TYPE DT " +
                            "WHERE D.DEVICE_TYPE_ID = DT.ID AND D.DEVICE_ID = ? AND D.TENANT_ID = ?");
            stmt.setString(1, deviceIdentifier);
            stmt.setInt(2, tenantId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                device = new IOTDevice();
                device.setId(rs.getInt("ID"));
                device.setDeviceId(rs.getString("DEVICE_ID"));
                device.setName(rs.getString("NAME"));
                device.setDeviceTypeId(rs.getInt("DEVICE_TYPE_ID"));
                device.setDeviceTypeName(rs.getString("DEVICE_TYPE_NAME"));
                device.setDescription(rs.getString("DESCRIPTION"));
            }
        } catch (SQLException | JSONException e) {
            throw new DeviceManagementDAOException(
                    "Error occurred while fetching devices of tenant '" + tenantId + "'", e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, null);
        }
        return device;
    }

    public void populateProperties(IOTDevice device) throws DeviceManagementDAOException {
        Connection conn;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = this.getConnection();
            String sql =
                    "SELECT PROP_NAME, PROP_VALUE FROM DM_IOT_DEVICE_PROPERTY WHERE DEVICE_ID = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, device.getId());
            rs = stmt.executeQuery();
            while (rs.next()) {
                String propName = rs.getString("PROP_NAME");
                String propValue = rs.getString("PROP_VALUE");
                device.getProperties().put(propName, propValue);
            }
        } catch (SQLException e) {
            throw new DeviceManagementDAOException("Error occurred while fetching properties of device: " + device.getId(), e);
        } finally {
            DeviceManagementDAOUtil.cleanupResources(stmt, rs);
        }
    }

    private Connection getConnection() throws SQLException {
        return DeviceManagementDAOFactory.getConnection();
    }
}
