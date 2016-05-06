/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.device.mgt.analytics.dashboard;

import org.wso2.carbon.device.mgt.analytics.dashboard.dao.bean.FilterSet;
import org.wso2.carbon.device.mgt.analytics.dashboard.dao.exception.InvalidParameterValueException;
import org.wso2.carbon.device.mgt.common.PaginationResult;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * To be updated...
 */
public interface GadgetDataService {

    @SuppressWarnings("unused")
    int getTotalDeviceCount() throws SQLException;

    @SuppressWarnings("unused")
    int getActiveDeviceCount() throws SQLException;

    @SuppressWarnings("unused")
    int getInactiveDeviceCount() throws SQLException;

    @SuppressWarnings("unused")
    int getRemovedDeviceCount() throws SQLException;

    @SuppressWarnings("unused")
    int getNonCompliantDeviceCount() throws SQLException;

    @SuppressWarnings("unused")
    int getUnmonitoredDeviceCount() throws SQLException;

    @SuppressWarnings("unused")
    PaginationResult getNonCompliantDeviceCountsByFeatures(int startIndex, int resultCount)
                                                           throws InvalidParameterValueException, SQLException;

    @SuppressWarnings("unused")
    int getDeviceCount(FilterSet filterSet) throws InvalidParameterValueException, SQLException;

    @SuppressWarnings("unused")
    int getFeatureNonCompliantDeviceCount(String nonCompliantFeatureCode,
                                          FilterSet filterSet) throws InvalidParameterValueException, SQLException;

    @SuppressWarnings("unused")
    Map<String, Integer> getDeviceCountsByPlatforms(FilterSet filterSet)
                                          throws InvalidParameterValueException, SQLException;

    @SuppressWarnings("unused")
    Map<String, Integer> getFeatureNonCompliantDeviceCountsByPlatforms(String nonCompliantFeatureCode,
                                          FilterSet filterSet) throws InvalidParameterValueException, SQLException;

    @SuppressWarnings("unused")
    Map<String, Integer> getDeviceCountsByOwnershipTypes(FilterSet filterSet)
                                          throws InvalidParameterValueException, SQLException;

    @SuppressWarnings("unused")
    Map<String, Integer> getFeatureNonCompliantDeviceCountsByOwnershipTypes(String nonCompliantFeatureCode,
                                          FilterSet filterSet) throws InvalidParameterValueException, SQLException;

    @SuppressWarnings("unused")
    PaginationResult getDevicesWithDetails(FilterSet filterSet, int startIndex, int resultCount)
                                                   throws InvalidParameterValueException, SQLException;

    @SuppressWarnings("unused")
    PaginationResult getFeatureNonCompliantDevicesWithDetails(String nonCompliantFeatureCode,
                                                   FilterSet filterSet, int startIndex, int resultCount)
                                                               throws InvalidParameterValueException, SQLException;

    @SuppressWarnings("unused")
    List<Map<String, Object>> getDevicesWithDetails(FilterSet filterSet)
                                                    throws InvalidParameterValueException, SQLException;

    @SuppressWarnings("unused")
    List<Map<String, Object>> getFeatureNonCompliantDevicesWithDetails(String nonCompliantFeatureCode,
                                          FilterSet filterSet) throws InvalidParameterValueException, SQLException;

}
