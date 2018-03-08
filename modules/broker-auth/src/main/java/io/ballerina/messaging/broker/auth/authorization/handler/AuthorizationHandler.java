/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package io.ballerina.messaging.broker.auth.authorization.handler;

import io.ballerina.messaging.broker.auth.AuthManager;
import io.ballerina.messaging.broker.auth.UsernamePrincipal;
import io.ballerina.messaging.broker.auth.authorization.Authorizer;
import io.ballerina.messaging.broker.auth.authorization.authorizer.rdbms.resource.AuthResource;
import io.ballerina.messaging.broker.auth.authorization.enums.ResourceActions;
import io.ballerina.messaging.broker.auth.authorization.enums.ResourceAuthScopes;
import io.ballerina.messaging.broker.auth.authorization.enums.ResourceTypes;
import io.ballerina.messaging.broker.auth.exception.BrokerAuthDuplicateException;
import io.ballerina.messaging.broker.auth.exception.BrokerAuthException;
import io.ballerina.messaging.broker.auth.exception.BrokerAuthNotFoundException;
import io.ballerina.messaging.broker.auth.exception.BrokerAuthServerException;
import io.ballerina.messaging.broker.common.ResourceNotFoundException;

import javax.security.auth.Subject;

/**
 * Class handles authorization for broker actions.
 */
public class AuthorizationHandler {

    private Authorizer authorizer;
    private UsernamePrincipal userPrincipal;

    public AuthorizationHandler(AuthManager authManager, Subject subject) {
        authorizer = authManager.getAuthorizer();
        this.userPrincipal = (UsernamePrincipal) subject.getPrincipals().iterator().next();
    }

    /**
     * Handle given auth scope and auth resource authorization.
     *
     * @param brokerAuthScope auth scope
     * @param resourceType    resource type
     * @param resourceName    resource name
     * @param action          action
     * @throws BrokerAuthException throws if error occurs while authorizing resource.
     */
    public void handle(ResourceAuthScopes brokerAuthScope, ResourceTypes resourceType, String resourceName,
                       ResourceActions action) throws BrokerAuthException, ResourceNotFoundException {
        handle(brokerAuthScope);
        handle(resourceType, resourceName, action);
    }

    /**
     * Handle given auth scope  authorization.
     *
     * @param resourceType resource type
     * @param resourceName resource name
     * @param action       action
     * @throws BrokerAuthException throws if error occurs while authorizing resource.
     */
    public void handle(ResourceTypes resourceType, String resourceName, ResourceActions action)
            throws BrokerAuthException, ResourceNotFoundException {
        try {
            if (!authorizer.authorize(resourceType.toString(),
                                      resourceName,
                                      action.toString(),
                                      userPrincipal.getName())) {
                throw new BrokerAuthException("Unauthorized action on : " + resourceType.toString() +
                                                      " resourceName: " + resourceName +
                                                      " action: " + action.toString());
            }
        } catch (BrokerAuthServerException e) {
            throw new BrokerAuthException("Error occurred while authorizing on : " + resourceType.toString() +
                                                  " resourceName: " + resourceName +
                                                  " action: " + action.toString(), e);
        } catch (BrokerAuthNotFoundException e) {
            throw new ResourceNotFoundException("Error occurred while authorizing due to resource name : " +
                                                  resourceName + " not found.");
        }
    }

    /**
     * Handle given auth scope authorization.
     *
     * @param authScope authScope
     * @throws BrokerAuthException throws if error occurs while authorizing resource.
     */
    public void handle(ResourceAuthScopes authScope)
            throws BrokerAuthException {
        try {
            if (!authorizer.authorize(authScope.toString(),
                                      userPrincipal.getName())) {
                throw new BrokerAuthException("Unauthorized action on auth scope key : " + authScope.toString());
            }
        } catch (BrokerAuthServerException | BrokerAuthNotFoundException e) {
            throw new BrokerAuthException("Error occurred while authorizing auth scope key : " +
                                                  authScope.toString(), e);
        }
    }

    /**
     * Create auth resource.
     *
     * @param resourceType resource type
     * @param resourceName resource name
     * @param durable      is durable
     * @throws BrokerAuthException throws if error occurs while authorizing resource.
     */
    public void createAuthResource(ResourceTypes resourceType, String resourceName, boolean durable)
            throws BrokerAuthException {
        try {
            authorizer.getAuthResourceStore().add(new AuthResource(resourceType.toString(),
                                                                   resourceName,
                                                                   durable,
                                                                   userPrincipal.getName()));
        } catch (BrokerAuthServerException e) {
            throw new BrokerAuthException("Error while creating " + resourceType + " with name : " + resourceName, e);
        } catch (BrokerAuthDuplicateException e) {
            throw new BrokerAuthException("Duplicate resource found for resource type : " + resourceType +
                    " with name : " + resourceName, e);
        }
    }

    /**
     * Delete auth resource.
     *
     * @param resourceType resource type
     * @param resourceName resource name
     * @throws BrokerAuthException throws if error occurs while authorizing resource.
     */
    public void deleteAuthResource(ResourceTypes resourceType, String resourceName)
            throws BrokerAuthException, ResourceNotFoundException {
        try {
            authorizer.getAuthResourceStore().delete(resourceType.toString(), resourceName);
        } catch (BrokerAuthServerException e) {
            throw new BrokerAuthException("Error while deleting " + resourceType + " with name : " + resourceName, e);
        } catch (BrokerAuthNotFoundException e) {
            throw new ResourceNotFoundException("Error occurred while authorizing due to resource name : " +
                    resourceName + " not found.");
        }
    }
}
