/**
 * Copyright (c) 2015 Intel Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.trustedanalytics.serviceexposer.retriver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trustedanalytics.cloud.cc.api.CcExtendedServiceInstance;
import org.trustedanalytics.cloud.cc.api.CcExtendedServicePlan;
import org.trustedanalytics.cloud.cc.api.CcOperations;
import org.trustedanalytics.cloud.cc.api.queries.Filter;
import org.trustedanalytics.cloud.cc.api.queries.FilterOperator;
import org.trustedanalytics.cloud.cc.api.queries.FilterQuery;
import rx.Observable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ServicesRetriver {

    private static final Logger LOG = LoggerFactory.getLogger(ServicesRetriver.class);
    private static final String successfullState = "succeeded";
    private CcOperations ccClient;
    private List<String> restrictedNames;

    public ServicesRetriver(CcOperations ccClient, List<String> restirctedNames) {
        this.ccClient = ccClient;
        this.restrictedNames = restirctedNames;
    }

    public Set<CcExtendedServiceInstance> getServiceInstances(String serviceType) {
        try {

            Set<CcExtendedServiceInstance> allServiceGuidsForGivenType = new HashSet<>();
            for (CcExtendedServicePlan servicePlan : getServicePlans(serviceType)) {
                String planGuid = servicePlan.getMetadata().getGuid().toString();
                ccClient.getExtendedServiceInstances(
                        FilterQuery.from(Filter.SERVICE_PLAN_GUID, FilterOperator.EQ, planGuid))
                        .filter(service -> (service.getEntity().getLastOperation().toString().contains("state=" + successfullState)))
                        .filter(service -> !restrictedNames.contains(service.getEntity().getName()))
                        .forEach(serviceInstance ->
                                        allServiceGuidsForGivenType.add(
                                                serviceInstance
                                        )
                        );
            }
            return allServiceGuidsForGivenType;

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return Collections.emptySet();
    }

    public Collection<CcExtendedServicePlan> getServicePlans(String serviceType) {
        return ccClient.getExtendedServices()
                .filter(service -> serviceType.equals(service.getEntity().getLabel()))
                .firstOrDefault(null)
                .flatMap(service -> {
                    if (service != null) {
                        return ccClient.getExtendedServicePlans(service.getMetadata().getGuid());
                    } else {
                        return Observable.empty();
                    }
                })
                .toList()
                .toBlocking()
                .single();
    }
}

