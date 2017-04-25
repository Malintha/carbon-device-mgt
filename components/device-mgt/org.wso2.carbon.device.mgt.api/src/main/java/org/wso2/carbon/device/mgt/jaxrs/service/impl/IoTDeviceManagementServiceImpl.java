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
package org.wso2.carbon.device.mgt.jaxrs.service.impl;

import io.swagger.annotations.ApiParam;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.application.extension.APIManagementProviderService;
import org.wso2.carbon.apimgt.application.extension.dto.ApiApplicationKey;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.device.mgt.common.DeviceManagementException;
import org.wso2.carbon.device.mgt.common.iot.IOTDevice;
import org.wso2.carbon.device.mgt.common.iot.IOTDeviceType;
import org.wso2.carbon.device.mgt.core.service.IOTDeviceManagementCoreService;
import org.wso2.carbon.device.mgt.jaxrs.beans.DeviceTypeList;
import org.wso2.carbon.device.mgt.jaxrs.beans.ErrorResponse;
import org.wso2.carbon.device.mgt.jaxrs.beans.iot.IOTDeviceCredentials;
import org.wso2.carbon.device.mgt.jaxrs.beans.iot.IOTDeviceList;
import org.wso2.carbon.device.mgt.jaxrs.service.api.IoTDeviceManagementService;
import org.wso2.carbon.device.mgt.jaxrs.util.APIMgtUtils;
import org.wso2.carbon.device.mgt.jaxrs.util.DeviceMgtAPIUtils;
import org.wso2.carbon.identity.jwt.client.extension.JWTClient;
import org.wso2.carbon.identity.jwt.client.extension.dto.AccessTokenInfo;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/iot")
public class IoTDeviceManagementServiceImpl implements IoTDeviceManagementService {

    private static Log log = LogFactory.getLog(IoTDeviceManagementServiceImpl.class);

    private ApiApplicationKey apiApplicationKey;

    @POST
    @Path("/device-types")
    @Override
    public Response addDeviceType(@ApiParam(name = "deviceType", value = "Device type information.", required = true) @Valid IOTDeviceType deviceType) {
        try {
            IOTDeviceManagementCoreService dms = DeviceMgtAPIUtils.getIOTDeviceManagementCoreService();
            dms.addIoTDeviceType(deviceType);
        } catch (DeviceManagementException e) {
            String msg = "Failed to add device type: " + deviceType.getName();
            log.error(msg, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
        }
        return Response.status(Response.Status.OK).entity("Device type added.").build();
    }

    @GET
    @Path("/device-types")
    @Override
    public Response getDeviceTypes() {
        List<String> deviceTypes;
        try {
            IOTDeviceManagementCoreService dms = DeviceMgtAPIUtils.getIOTDeviceManagementCoreService();
            deviceTypes = dms.getIoTDeviceTypeNames();
            DeviceTypeList deviceTypeList = new DeviceTypeList();
            deviceTypeList.setCount(deviceTypes.size());
            deviceTypeList.setList(deviceTypes);
            return Response.status(Response.Status.OK).entity(deviceTypeList).build();
        } catch (DeviceManagementException e) {
            String msg = "Error occurred while fetching the list of device types.";
            log.error(msg, e);
            return Response.serverError().entity(
                    new ErrorResponse.ErrorResponseBuilder().setMessage(msg).build()).build();
        }
    }

    @POST
    @Path("/devices")
    @Override
    public Response addDevice(@ApiParam(name = "device", value = "Device Information.", required = true) @Valid IOTDevice device) {
        try {
            IOTDeviceManagementCoreService dms = DeviceMgtAPIUtils.getIOTDeviceManagementCoreService();
            String authorizedUser = MultitenantUtils.getTenantAwareUsername(CarbonContext.getThreadLocalCarbonContext().getUsername());
            IOTDevice addedDevice = dms.addIoTDevice(device);
            AccessTokenInfo accessTokenInfo = getAccessToken(addedDevice.getDeviceId(), authorizedUser);
            IOTDeviceCredentials deviceCredentials = new IOTDeviceCredentials();
            deviceCredentials.setDeviceIdentifier(addedDevice.getDeviceId());
            deviceCredentials.setAccessToken(accessTokenInfo.getAccessToken());
            deviceCredentials.setRefreshToken(accessTokenInfo.getRefreshToken());
            deviceCredentials.setExpiry(accessTokenInfo.getExpiresIn());
            if (apiApplicationKey != null) {
                deviceCredentials.setConsumerKey(apiApplicationKey.getConsumerKey());
                deviceCredentials.setConsumerSecret(apiApplicationKey.getConsumerSecret());
            }
            return Response.status(Response.Status.OK).entity(deviceCredentials).build();
        } catch (DeviceManagementException e) {
            String msg = "Failed to add device: " + device.getName();
            log.error(msg, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
        }
    }

    @GET
    @Path("/devices")
    @Override
    public Response getDevices() {
        try {
            IOTDeviceManagementCoreService dms = DeviceMgtAPIUtils.getIOTDeviceManagementCoreService();
            List<IOTDevice> devices = dms.getIoTDevices();
            IOTDeviceList deviceList = new IOTDeviceList();
            deviceList.setDevices(devices);
            return Response.status(Response.Status.OK).entity(deviceList).build();
        } catch (DeviceManagementException e) {
            String msg = "Error occurred while fetching the list of devices.";
            log.error(msg, e);
            return Response.serverError().entity(
                    new ErrorResponse.ErrorResponseBuilder().setMessage(msg).build()).build();
        }
    }

    @GET
    @Path("/devices/{device-identifier}")
    @Override
    public Response getDevice(@PathParam("device-identifier") String identifier) {
        try {
            IOTDeviceManagementCoreService dms = DeviceMgtAPIUtils.getIOTDeviceManagementCoreService();
            IOTDevice device = dms.getIoTDevice(identifier);
            return Response.status(Response.Status.OK).entity(device).build();
        } catch (DeviceManagementException e) {
            String msg = "Error occurred while fetching devices: " + identifier;
            log.error(msg, e);
            return Response.serverError().entity(
                    new ErrorResponse.ErrorResponseBuilder().setMessage(msg).build()).build();
        }
    }

    public Response addOperation() {
        return null;
    }

    private AccessTokenInfo getAccessToken(String deviceId, String user) throws DeviceManagementException {
        try {
            if (apiApplicationKey == null) {
                String applicationUsername =
                        PrivilegedCarbonContext.getThreadLocalCarbonContext().getUserRealm().getRealmConfiguration()
                                .getAdminUserName() + "@" + PrivilegedCarbonContext.getThreadLocalCarbonContext()
                                .getTenantDomain();
                APIManagementProviderService apiManagementProviderService = APIMgtUtils.getAPIManagementProviderService();
                String[] tags = {"iot"};
                apiApplicationKey = apiManagementProviderService.generateAndRetrieveApplicationKeys(
                        "iot", tags, "PRODUCTION", applicationUsername, true,
                        "3600000");
            }
            JWTClient jwtClient = APIMgtUtils.getJWTClientManagerService().getJWTClient();
            String scopes = " device_" + deviceId;
            AccessTokenInfo accessTokenInfo = jwtClient.getAccessToken(apiApplicationKey.getConsumerKey(),
                    apiApplicationKey.getConsumerSecret(), user, scopes);
            return accessTokenInfo;
        } catch (Exception e) {
            String msg = "Failed to get access token for the device " + deviceId + " owned by " + user;
            throw new DeviceManagementException(msg, e);
        }
    }
}
