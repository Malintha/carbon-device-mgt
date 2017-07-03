package org.wso2.carbon.device.mgt.jaxrs.service.impl;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Stub;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.analytics.api.AnalyticsDataAPI;
import org.wso2.carbon.analytics.api.AnalyticsDataAPIUtil;
import org.wso2.carbon.analytics.dataservice.commons.AnalyticsDataResponse;
import org.wso2.carbon.analytics.dataservice.commons.SearchResultEntry;
import org.wso2.carbon.analytics.dataservice.commons.SortByField;
import org.wso2.carbon.analytics.dataservice.commons.SortType;
import org.wso2.carbon.analytics.stream.persistence.stub
        .EventStreamPersistenceAdminServiceEventStreamPersistenceAdminServiceExceptionException;
import org.wso2.carbon.analytics.stream.persistence.stub.EventStreamPersistenceAdminServiceStub;
import org.wso2.carbon.analytics.stream.persistence.stub.dto.AnalyticsTable;
import org.wso2.carbon.analytics.stream.persistence.stub.dto.AnalyticsTableRecord;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.util.Utils;
import org.wso2.carbon.device.mgt.common.DeviceIdentifier;
import org.wso2.carbon.device.mgt.common.DeviceManagementException;
import org.wso2.carbon.device.mgt.common.authorization.DeviceAccessAuthorizationException;
import org.wso2.carbon.device.mgt.jaxrs.beans.analytics.DeviceTypeEvent;
import org.wso2.carbon.device.mgt.jaxrs.beans.analytics.EventRecords;
import org.wso2.carbon.device.mgt.jaxrs.beans.analytics.Attribute;
import org.wso2.carbon.device.mgt.jaxrs.beans.analytics.AttributeType;
import org.wso2.carbon.device.mgt.jaxrs.beans.analytics.EventAttributeList;
import org.wso2.carbon.device.mgt.jaxrs.beans.analytics.TransportType;
import org.wso2.carbon.device.mgt.jaxrs.service.api.DeviceEventManagementService;
import org.wso2.carbon.device.mgt.jaxrs.util.Constants;
import org.wso2.carbon.device.mgt.jaxrs.util.DeviceMgtAPIUtils;
import org.wso2.carbon.event.publisher.stub.EventPublisherAdminServiceCallbackHandler;
import org.wso2.carbon.event.publisher.stub.EventPublisherAdminServiceStub;
import org.wso2.carbon.event.publisher.stub.types.EventMappingPropertyDto;
import org.wso2.carbon.event.receiver.stub.EventReceiverAdminServiceCallbackHandler;
import org.wso2.carbon.event.receiver.stub.EventReceiverAdminServiceStub;
import org.wso2.carbon.event.receiver.stub.types.BasicInputAdapterPropertyDto;
import org.wso2.carbon.event.receiver.stub.types.EventReceiverConfigurationDto;
import org.wso2.carbon.event.stream.stub.EventStreamAdminServiceStub;
import org.wso2.carbon.event.stream.stub.types.EventStreamAttributeDto;
import org.wso2.carbon.event.stream.stub.types.EventStreamDefinitionDto;
import org.wso2.carbon.identity.jwt.client.extension.JWTClient;
import org.wso2.carbon.identity.jwt.client.extension.exception.JWTClientException;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.analytics.datasource.commons.exception.AnalyticsException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import javax.validation.Valid;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This is used for simple analytics purpose, to create streams and receiver dynamically and a common endpoint
 * to retrieve data.
 */
@Path("/events")
public class DeviceEventManagementServiceImpl implements DeviceEventManagementService {

    private static final Log log = LogFactory.getLog(DeviceEventManagementServiceImpl.class);

    private static final String DEFAULT_EVENT_STORE_NAME = "EVENT_STORE";
    private static final String DEFAULT_WEBSOCKET_PUBLISHER_ADAPTER_TYPE = "secured-websocket";
    private static final String OAUTH_MQTT_ADAPTER_TYPE = "oauth-mqtt";
    private static final String THRIFT_ADAPTER_TYPE = "iot-event";
    private static final String DEFAULT_DEVICE_ID_ATTRIBUTE = "deviceId";
    private static final String DEFAULT_META_DEVICE_ID_ATTRIBUTE = "meta_deviceId";



    @GET
    @Path("/{type}")
    @Override
    public Response getDeviceTypeEventDefinition(@PathParam("type") String deviceType) {
        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        EventStreamAdminServiceStub eventStreamAdminServiceStub = null;
        EventReceiverAdminServiceStub eventReceiverAdminServiceStub = null;
        try {
            if (deviceType == null ||
                    !DeviceMgtAPIUtils.getDeviceManagementService().getAvailableDeviceTypes().contains(deviceType)) {
                String errorMessage = "Invalid device type";
                log.error(errorMessage);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            String streamName = DeviceMgtAPIUtils.getStreamDefinition(deviceType, tenantDomain);
            eventStreamAdminServiceStub = DeviceMgtAPIUtils.getEventStreamAdminServiceStub();
            EventStreamDefinitionDto eventStreamDefinitionDto = eventStreamAdminServiceStub.getStreamDefinitionDto(
                    streamName + ":" + Constants.DEFAULT_STREAM_VERSION);
            if (eventStreamDefinitionDto == null) {
                return Response.status(Response.Status.NO_CONTENT).build();
            }

            EventStreamAttributeDto[] eventStreamAttributeDtos = eventStreamDefinitionDto.getPayloadData();
            EventAttributeList eventAttributeList = new EventAttributeList();
            List<Attribute> attributes = new ArrayList<>();
            for (EventStreamAttributeDto eventStreamAttributeDto : eventStreamAttributeDtos) {
                attributes.add(new Attribute(eventStreamAttributeDto.getAttributeName()
                        , AttributeType.valueOf(eventStreamAttributeDto.getAttributeType().toUpperCase())));
            }
            eventAttributeList.setList(attributes);

            DeviceTypeEvent deviceTypeEvent = new DeviceTypeEvent();
            deviceTypeEvent.setEventAttributeList(eventAttributeList);
            deviceTypeEvent.setTransportType(TransportType.HTTP);
            eventReceiverAdminServiceStub = DeviceMgtAPIUtils.getEventReceiverAdminServiceStub();
            EventReceiverConfigurationDto eventReceiverConfigurationDto = eventReceiverAdminServiceStub
                    .getActiveEventReceiverConfiguration(getReceiverName(deviceType, tenantDomain));
            if (eventReceiverConfigurationDto != null) {
                String eventAdapterType = eventReceiverConfigurationDto.getFromAdapterConfigurationDto()
                        .getEventAdapterType();
                if (OAUTH_MQTT_ADAPTER_TYPE.equals(eventAdapterType)) {
                    deviceTypeEvent.setTransportType(TransportType.MQTT);
                }
            }
            return Response.ok().entity(deviceTypeEvent).build();
        } catch (AxisFault e) {
            log.error("failed to retrieve event definitions for tenantDomain:" + tenantDomain, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (RemoteException e) {
            log.error("Failed to connect with the remote services:" + tenantDomain, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (JWTClientException e) {
            log.error("Failed to generate jwt token for tenantDomain:" + tenantDomain, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (UserStoreException e) {
            log.error("Failed to connect with the user store, tenantDomain: " + tenantDomain, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (DeviceManagementException e) {
            log.error("Failed to access device management service, tenantDomain: " + tenantDomain, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            cleanup(eventStreamAdminServiceStub);
            cleanup(eventReceiverAdminServiceStub);
        }
    }

    /**
     * Deploy Event Stream, Receiver, Publisher and Store Configuration.
     */
    @POST
    @Path("/{type}")
    @Override
    public Response deployDeviceTypeEventDefinition(@PathParam("type") String deviceType,
                                                    @Valid DeviceTypeEvent deviceTypeEvent) {
        TransportType transportType = deviceTypeEvent.getTransportType();
        EventAttributeList eventAttributes = deviceTypeEvent.getEventAttributeList();
        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        boolean superTenantMode = false;
        try {
            if (eventAttributes == null || eventAttributes.getList() == null || eventAttributes.getList().size() == 0 ||
                    deviceType == null || transportType == null ||
                    !DeviceMgtAPIUtils.getDeviceManagementService().getAvailableDeviceTypes().contains(deviceType)) {
                String errorMessage = "Invalid Payload";
                log.error(errorMessage);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            String eventReceiverName = getReceiverName(deviceType, tenantDomain);
            String streamName = DeviceMgtAPIUtils.getStreamDefinition(deviceType, tenantDomain);
            String streamNameWithVersion = streamName + ":" + Constants.DEFAULT_STREAM_VERSION;
            publishStreamDefinitons(streamName, Constants.DEFAULT_STREAM_VERSION, deviceType, eventAttributes);
            publishEventReceivers(eventReceiverName, streamNameWithVersion, transportType, tenantDomain, deviceType);
            publishEventStore(streamName, Constants.DEFAULT_STREAM_VERSION, eventAttributes);
            publishWebsocketPublisherDefinition(streamNameWithVersion, deviceType);
            superTenantMode = true;
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(
                    MultitenantConstants.SUPER_TENANT_DOMAIN_NAME, true);
            if (!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
                if (transportType == TransportType.MQTT) {
                    publishStreamDefinitons(streamName, Constants.DEFAULT_STREAM_VERSION, deviceType, eventAttributes);
                    publishEventReceivers(eventReceiverName, streamNameWithVersion, transportType, tenantDomain,
                                          deviceType);
                } else {
                    publishStreamDefinitons(streamName, Constants.DEFAULT_STREAM_VERSION, deviceType, eventAttributes);
                }

            }
            DeviceMgtAPIUtils.getDynamicEventCache().remove(deviceType);
            return Response.ok().build();
        } catch (AxisFault e) {
            log.error("failed to create event definitions for tenantDomain:" + tenantDomain, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (RemoteException e) {
            log.error("Failed to connect with the remote services:" + tenantDomain, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (JWTClientException e) {
            log.error("Failed to generate jwt token for tenantDomain:" + tenantDomain, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (UserStoreException e) {
            log.error("Failed to connect with the user store, tenantDomain: " + tenantDomain, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (DeviceManagementException e) {
            log.error("Failed to access device management service, tenantDomain: " + tenantDomain, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (EventStreamPersistenceAdminServiceEventStreamPersistenceAdminServiceExceptionException e) {
            log.error("Failed to create event store for, tenantDomain: " + tenantDomain + " deviceType" + deviceType,
                      e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (superTenantMode) {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }
    }

    @DELETE
    @Path("/{type}")
    @Override
    public Response deleteDeviceTypeEventDefinitions(@PathParam("type") String deviceType) {
        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        boolean superTenantMode = false;
        EventReceiverAdminServiceStub eventReceiverAdminServiceStub = null;
        EventPublisherAdminServiceStub eventPublisherAdminServiceStub = null;
        EventStreamAdminServiceStub eventStreamAdminServiceStub = null;

        EventReceiverAdminServiceStub tenantBasedEventReceiverAdminServiceStub = null;
        EventStreamAdminServiceStub tenantBasedEventStreamAdminServiceStub = null;
        try {
            if (deviceType == null ||
                    !DeviceMgtAPIUtils.getDeviceManagementService().getAvailableDeviceTypes().contains(deviceType)) {
                String errorMessage = "Invalid device type";
                return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
            }
            String eventReceiverName = getReceiverName(deviceType, tenantDomain);
            String eventPublisherName = deviceType.trim().replace(" ", "_") + "_websocket_publisher";
            String streamName = DeviceMgtAPIUtils.getStreamDefinition(deviceType, tenantDomain);
            eventStreamAdminServiceStub = DeviceMgtAPIUtils.getEventStreamAdminServiceStub();
            if (eventStreamAdminServiceStub.getStreamDefinitionDto(streamName + ":" + Constants.DEFAULT_STREAM_VERSION) == null) {
                return Response.status(Response.Status.NO_CONTENT).build();
            }
            eventStreamAdminServiceStub.removeEventStreamDefinition(streamName, Constants.DEFAULT_STREAM_VERSION);
            EventReceiverAdminServiceCallbackHandler eventReceiverAdminServiceCallbackHandler =
                    new EventReceiverAdminServiceCallbackHandler() {};
            EventPublisherAdminServiceCallbackHandler eventPublisherAdminServiceCallbackHandler =
                    new EventPublisherAdminServiceCallbackHandler() {};

            eventReceiverAdminServiceStub = DeviceMgtAPIUtils.getEventReceiverAdminServiceStub();
            eventReceiverAdminServiceStub.startundeployInactiveEventReceiverConfiguration(eventReceiverName
                    , eventReceiverAdminServiceCallbackHandler);

            eventPublisherAdminServiceStub = DeviceMgtAPIUtils.getEventPublisherAdminServiceStub();
            eventPublisherAdminServiceStub.startundeployInactiveEventPublisherConfiguration(eventPublisherName
                    , eventPublisherAdminServiceCallbackHandler);

            superTenantMode = true;
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(
                    MultitenantConstants.SUPER_TENANT_DOMAIN_NAME, true);
            if (!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
                tenantBasedEventReceiverAdminServiceStub = DeviceMgtAPIUtils.getEventReceiverAdminServiceStub();
                tenantBasedEventStreamAdminServiceStub = DeviceMgtAPIUtils.getEventStreamAdminServiceStub();
                tenantBasedEventStreamAdminServiceStub.removeEventStreamDefinition(streamName, Constants.DEFAULT_STREAM_VERSION);
                tenantBasedEventReceiverAdminServiceStub.startundeployInactiveEventReceiverConfiguration(
                        eventReceiverName
                        , eventReceiverAdminServiceCallbackHandler);

            }
            return Response.ok().build();
        } catch (AxisFault e) {
            log.error("failed to delete event definitions for tenantDomain:" + tenantDomain, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (RemoteException e) {
            log.error("Failed to connect with the remote services:" + tenantDomain, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (JWTClientException e) {
            log.error("Failed to generate jwt token for tenantDomain:" + tenantDomain, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (UserStoreException e) {
            log.error("Failed to connect with the user store, tenantDomain: " + tenantDomain, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (DeviceManagementException e) {
            log.error("Failed to access device management service, tenantDomain: " + tenantDomain, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (superTenantMode) {
                PrivilegedCarbonContext.endTenantFlow();
            }
            cleanup(eventStreamAdminServiceStub);
            cleanup(eventPublisherAdminServiceStub);
            cleanup(eventReceiverAdminServiceStub);
            cleanup(eventReceiverAdminServiceStub);
            cleanup(eventStreamAdminServiceStub);
            cleanup(tenantBasedEventReceiverAdminServiceStub);
            cleanup(tenantBasedEventStreamAdminServiceStub);
        }
    }

    @GET
    @Path("/{type}/{deviceId}")
    @Override
    public Response getData(@PathParam("deviceId") String deviceId, @QueryParam("from") long from,
                            @QueryParam("to") long to, @PathParam("type") String deviceType, @QueryParam("offset")
                            int offset, @QueryParam("limit") int limit) {
        if (from == 0 || to == 0) {
            String errorMessage = "Invalid values for from/to";
            return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
        }
        String fromDate = String.valueOf(from);
        String toDate = String.valueOf(to);
        String query = DEFAULT_META_DEVICE_ID_ATTRIBUTE + ":" + deviceId
                + " AND _timestamp : [" + fromDate + " TO " + toDate + "]";
        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        String sensorTableName = getTableName(DeviceMgtAPIUtils.getStreamDefinition(deviceType, tenantDomain));
        try {
            if (deviceType == null ||
                    !DeviceMgtAPIUtils.getDeviceManagementService().getAvailableDeviceTypes().contains(deviceType)) {
                String errorMessage = "Invalid device type";
                log.error(errorMessage);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            if (!DeviceMgtAPIUtils.getDeviceAccessAuthorizationService().isUserAuthorized(
                    new DeviceIdentifier(deviceId, deviceType))) {
                return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
            }
            List<SortByField> sortByFields = new ArrayList<>();
            SortByField sortByField = new SortByField("_timestamp", SortType.DESC);
            sortByFields.add(sortByField);
            EventRecords eventRecords = getAllEventsForDevice(sensorTableName, query, sortByFields, offset, limit);
            return Response.status(Response.Status.OK.getStatusCode()).entity(eventRecords).build();
        } catch (AnalyticsException e) {
            String errorMsg = "Error on retrieving stats on table " + sensorTableName + " with query " + query;
            log.error(errorMsg);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).entity(errorMsg).build();
        } catch (DeviceAccessAuthorizationException e) {
            log.error(e.getErrorMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (DeviceManagementException e) {
            String errorMsg = "Error on retrieving stats on table " + sensorTableName + " with query " + query;
            log.error(errorMsg);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).entity(errorMsg).build();
        }
    }

    @GET
    @Path("/last-known/{type}/{deviceId}")
    @Override
    public Response getLastKnownData(@PathParam("deviceId") String deviceId, @PathParam("type") String deviceType) {
        String query = DEFAULT_META_DEVICE_ID_ATTRIBUTE + ":" + deviceId;
        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        String sensorTableName = getTableName(DeviceMgtAPIUtils.getStreamDefinition(deviceType, tenantDomain));
        try {
            if (deviceType == null ||
                    !DeviceMgtAPIUtils.getDeviceManagementService().getAvailableDeviceTypes().contains(deviceType)) {
                String errorMessage = "Invalid device type";
                log.error(errorMessage);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            if (!DeviceMgtAPIUtils.getDeviceAccessAuthorizationService().isUserAuthorized(
                    new DeviceIdentifier(deviceId, deviceType))) {
                return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
            }
            List<SortByField> sortByFields = new ArrayList<>();
            SortByField sortByField = new SortByField("_timestamp", SortType.DESC);
            sortByFields.add(sortByField);
            EventRecords eventRecords = getAllEventsForDevice(sensorTableName, query, sortByFields, 0, 1);
            return Response.status(Response.Status.OK.getStatusCode()).entity(eventRecords).build();
        } catch (AnalyticsException e) {
            String errorMsg = "Error on retrieving stats on table " + sensorTableName + " with query " + query;
            log.error(errorMsg);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).entity(errorMsg).build();
        } catch (DeviceAccessAuthorizationException e) {
            log.error(e.getErrorMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (DeviceManagementException e) {
            String errorMsg = "Error on retrieving stats on table " + sensorTableName + " with query " + query;
            log.error(errorMsg);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).entity(errorMsg).build();
        }
    }

    private void publishEventReceivers(String eventRecieverName, String streamNameWithVersion,
                                       TransportType transportType, String requestedTenantDomain,
                                       String deviceType)
            throws RemoteException, UserStoreException, JWTClientException {
        EventReceiverAdminServiceStub receiverAdminServiceStub = DeviceMgtAPIUtils.getEventReceiverAdminServiceStub();
        try {
            EventReceiverConfigurationDto eventReceiverConfigurationDto = receiverAdminServiceStub
                    .getActiveEventReceiverConfiguration(getReceiverName(deviceType, requestedTenantDomain));
            if (eventReceiverConfigurationDto != null) {
                String eventAdapterType = eventReceiverConfigurationDto.getFromAdapterConfigurationDto()
                        .getEventAdapterType();
                if (OAUTH_MQTT_ADAPTER_TYPE.equals(eventAdapterType)) {
                    if (transportType == TransportType.MQTT) {
                        return;
                    }

                } else if (THRIFT_ADAPTER_TYPE.equals(eventAdapterType)) {
                    if (transportType == TransportType.HTTP) {
                        return;
                    }
                }
                // remove mqtt event reciever before publishing
                receiverAdminServiceStub.undeployActiveEventReceiverConfiguration(eventRecieverName);
            }

            String adapterType = OAUTH_MQTT_ADAPTER_TYPE;
            BasicInputAdapterPropertyDto basicInputAdapterPropertyDtos[];
            if (transportType == TransportType.MQTT) {
                basicInputAdapterPropertyDtos = new BasicInputAdapterPropertyDto[1];
                basicInputAdapterPropertyDtos[0] = getBasicInputAdapterPropertyDto("topic", requestedTenantDomain
                        + "/" + deviceType + "/+/events");
            } else {
                adapterType = THRIFT_ADAPTER_TYPE;
                basicInputAdapterPropertyDtos = new BasicInputAdapterPropertyDto[1];
                basicInputAdapterPropertyDtos[0] = getBasicInputAdapterPropertyDto("events.duplicated.in.cluster", "false");
            }
            if (receiverAdminServiceStub.getActiveEventReceiverConfiguration(eventRecieverName) == null) {
                if (transportType == TransportType.MQTT) {
                    receiverAdminServiceStub.deployJsonEventReceiverConfiguration(eventRecieverName, streamNameWithVersion
                            , adapterType, null, basicInputAdapterPropertyDtos, false);
                } else {
                    EventMappingPropertyDto eventMappingPropertyDto = new EventMappingPropertyDto();

                    receiverAdminServiceStub.deployWso2EventReceiverConfiguration(eventRecieverName, streamNameWithVersion
                            , adapterType, null, null, null, basicInputAdapterPropertyDtos, false, null);
                }
            }
        } finally {
            cleanup(receiverAdminServiceStub);
        }
    }

    private void publishStreamDefinitons(String streamName, String version, String deviceType
            , EventAttributeList eventAttributes)
            throws RemoteException, UserStoreException, JWTClientException {
        EventStreamAdminServiceStub eventStreamAdminServiceStub = DeviceMgtAPIUtils.getEventStreamAdminServiceStub();
        try {
            EventStreamDefinitionDto eventStreamDefinitionDto = new EventStreamDefinitionDto();
            eventStreamDefinitionDto.setName(streamName);
            eventStreamDefinitionDto.setVersion(version);
            EventStreamAttributeDto eventStreamAttributeDtos[] =
                    new EventStreamAttributeDto[eventAttributes.getList().size()];
            EventStreamAttributeDto metaStreamAttributeDtos[] =
                    new EventStreamAttributeDto[1];
            int i = 0;
            for (Attribute attribute : eventAttributes.getList()) {
                EventStreamAttributeDto eventStreamAttributeDto = new EventStreamAttributeDto();
                eventStreamAttributeDto.setAttributeName(attribute.getName());
                eventStreamAttributeDto.setAttributeType(attribute.getType().toString());
                eventStreamAttributeDtos[i] = eventStreamAttributeDto;
                i++;
            }

            EventStreamAttributeDto eventStreamAttributeDto = new EventStreamAttributeDto();
            eventStreamAttributeDto.setAttributeName(DEFAULT_DEVICE_ID_ATTRIBUTE);
            eventStreamAttributeDto.setAttributeType(AttributeType.STRING.toString());
            metaStreamAttributeDtos[0] = eventStreamAttributeDto;
            eventStreamDefinitionDto.setPayloadData(eventStreamAttributeDtos);
            eventStreamDefinitionDto.setMetaData(metaStreamAttributeDtos);
            String streamId = streamName + ":" + version;
            if (eventStreamAdminServiceStub.getStreamDefinitionDto(streamId) != null) {
                eventStreamAdminServiceStub.editEventStreamDefinitionAsDto(eventStreamDefinitionDto, streamId);
            } else {
                eventStreamAdminServiceStub.addEventStreamDefinitionAsDto(eventStreamDefinitionDto);
            }
        } finally {
            cleanup(eventStreamAdminServiceStub);
        }
    }

    private void publishEventStore(String streamName, String version, EventAttributeList eventAttributes)
            throws RemoteException, UserStoreException, JWTClientException,
                   EventStreamPersistenceAdminServiceEventStreamPersistenceAdminServiceExceptionException {
        EventStreamPersistenceAdminServiceStub eventStreamPersistenceAdminServiceStub =
                DeviceMgtAPIUtils.getEventStreamPersistenceAdminServiceStub();
        try {
            AnalyticsTable analyticsTable = new AnalyticsTable();
            analyticsTable.setRecordStoreName(DEFAULT_EVENT_STORE_NAME);
            analyticsTable.setStreamVersion(version);
            analyticsTable.setTableName(streamName);
            analyticsTable.setMergeSchema(false);
            analyticsTable.setPersist(true);
            AnalyticsTableRecord analyticsTableRecords[] = new AnalyticsTableRecord[eventAttributes.getList().size() + 1];
            int i = 0;
            for (Attribute attribute : eventAttributes.getList()) {
                AnalyticsTableRecord analyticsTableRecord = new AnalyticsTableRecord();
                analyticsTableRecord.setColumnName(attribute.getName());
                analyticsTableRecord.setColumnType(attribute.getType().toString().toUpperCase());
                analyticsTableRecord.setFacet(false);
                analyticsTableRecord.setIndexed(true);
                analyticsTableRecord.setPersist(true);
                analyticsTableRecord.setPrimaryKey(false);
                analyticsTableRecord.setScoreParam(false);
                analyticsTableRecords[i] = analyticsTableRecord;
                i++;
            }
            AnalyticsTableRecord analyticsTableRecord = new AnalyticsTableRecord();
            analyticsTableRecord.setColumnName(DEFAULT_META_DEVICE_ID_ATTRIBUTE);
            analyticsTableRecord.setColumnType(AttributeType.STRING.toString().toUpperCase());
            analyticsTableRecord.setFacet(false);
            analyticsTableRecord.setIndexed(true);
            analyticsTableRecord.setPersist(true);
            analyticsTableRecord.setPrimaryKey(false);
            analyticsTableRecord.setScoreParam(false);
            analyticsTableRecords[i] = analyticsTableRecord;
            analyticsTable.setAnalyticsTableRecords(analyticsTableRecords);
            eventStreamPersistenceAdminServiceStub.addAnalyticsTable(analyticsTable);
        } finally {
            cleanup(eventStreamPersistenceAdminServiceStub);
        }
    }

    private void publishWebsocketPublisherDefinition(String streamNameWithVersion, String deviceType)
            throws RemoteException, UserStoreException, JWTClientException {
        EventPublisherAdminServiceStub eventPublisherAdminServiceStub = DeviceMgtAPIUtils
                .getEventPublisherAdminServiceStub();
        try {
            String eventPublisherName = deviceType.trim().replace(" ", "_") + "_websocket_publisher";
            if (eventPublisherAdminServiceStub.getActiveEventPublisherConfiguration(eventPublisherName) == null) {
                eventPublisherAdminServiceStub.deployJsonEventPublisherConfiguration(eventPublisherName
                        , streamNameWithVersion, DEFAULT_WEBSOCKET_PUBLISHER_ADAPTER_TYPE, null, null
                        , null, false);
            }
        } finally {
            cleanup(eventPublisherAdminServiceStub);
        }
    }

    private BasicInputAdapterPropertyDto getBasicInputAdapterPropertyDto(String key, String value) {
        BasicInputAdapterPropertyDto basicInputAdapterPropertyDto = new BasicInputAdapterPropertyDto();
        basicInputAdapterPropertyDto.setKey(key);
        basicInputAdapterPropertyDto.setValue(value);
        return basicInputAdapterPropertyDto;
    }

    private static String generateUUID() {
        UUID uuid = UUID.randomUUID();
        long l = ByteBuffer.wrap(uuid.toString().getBytes(StandardCharsets.UTF_8)).getLong();
        return Long.toString(l, Character.MAX_RADIX);
    }

    private String getTableName(String streamName) {
        return streamName.toUpperCase().replace('.', '_');
    }

    private String getReceiverName(String deviceType, String tenantDomain) {
        return deviceType.replace(" ", "_").trim() + "-" + tenantDomain + "-receiver";
    }

    public static AnalyticsDataAPI getAnalyticsDataAPI() {
        PrivilegedCarbonContext ctx = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        AnalyticsDataAPI analyticsDataAPI =
                (AnalyticsDataAPI) ctx.getOSGiService(AnalyticsDataAPI.class, null);
        if (analyticsDataAPI == null) {
            String msg = "Analytics api service has not initialized.";
            log.error(msg);
            throw new IllegalStateException(msg);
        }
        return analyticsDataAPI;
    }

    protected static EventRecords getAllEventsForDevice(String tableName, String query, List<SortByField> sortByFields
            , int offset, int limit) throws AnalyticsException {
        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        AnalyticsDataAPI analyticsDataAPI = getAnalyticsDataAPI();
        EventRecords eventRecords = new EventRecords();
        int eventCount = analyticsDataAPI.searchCount(tenantId, tableName, query);
        if (eventCount == 0) {
            eventRecords.setCount(0);
        }
        List<SearchResultEntry> resultEntries = analyticsDataAPI.search(tenantId, tableName, query, offset, limit,
                                                                        sortByFields);
        List<String> recordIds = getRecordIds(resultEntries);
        AnalyticsDataResponse response = analyticsDataAPI.get(tenantId, tableName, 1, null, recordIds);
        eventRecords.setCount(eventCount);
        eventRecords.setList(AnalyticsDataAPIUtil.listRecords(analyticsDataAPI, response));
        return eventRecords;
    }

    private static List<String> getRecordIds(List<SearchResultEntry> searchResults) {
        List<String> ids = new ArrayList<>();
        for (SearchResultEntry searchResult : searchResults) {
            ids.add(searchResult.getId());
        }
        return ids;
    }

    private void cleanup(Stub stub) {
        if (stub != null) {
            try {
                stub.cleanup();
            } catch (AxisFault axisFault) {
                // do nothing
            }
        }
    }

}