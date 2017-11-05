/**
 * Copyright (C) 2010-2017 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.mqtt.entity;

import java.util.HashMap;
import java.util.Map;
import org.apache.cxf.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.script.Scripting;
import org.structr.rest.RestMethodResult;
import org.structr.schema.SchemaService;
import org.structr.schema.action.ActionContext;

public class MQTTSubscriberMixin extends AbstractNode implements MQTTSubscriber {

	private static final Logger logger = LoggerFactory.getLogger(MQTTSubscriberMixin.class.getName());

	static {

		SchemaService.registerMixinType("MQTTSubscriber", AbstractNode.class, MQTTSubscriberMixin.class);
	}

	@Override
	public boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		if(!StringUtils.isEmpty(getProperty(topic)) && (getProperty(client) != null) && getProperty(client).getProperty(MQTTClient.isConnected)) {
			Map<String,Object> params = new HashMap<>();
			params.put("topic", getProperty(topic));
			getProperty(client).invokeMethod("subscribeTopic", params, false);
		}

		return super.onCreation(securityContext, errorBuffer);
	}

	@Override
	public boolean onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		if(!StringUtils.isEmpty(getProperty(topic)) && (getProperty(client) != null) && getProperty(client).getProperty(MQTTClient.isConnected)) {

			if(modificationQueue.isPropertyModified(this,topic)){

				Map<String,Object> params = new HashMap<>();
				params.put("topic", getProperty(topic));
				getProperty(client).invokeMethod("subscribeTopic", params, false);
			}
		}


		return super.onModification(securityContext, errorBuffer, modificationQueue);
	}

	@Export
	public RestMethodResult onMessage(final String topic, final String message) throws FrameworkException {

		if (!StringUtils.isEmpty(getProperty(source))) {

			String script = "${" + getProperty(source) + "}";

			Map<String, Object> params = new HashMap<>();
			params.put("topic", topic);
			params.put("message", message);

			ActionContext ac = new ActionContext(securityContext, params);
			Scripting.replaceVariables(ac, this, script);
		}

		return new RestMethodResult(200);
	}

}
