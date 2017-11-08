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
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.EndNodes;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.mqtt.MQTTClientConnection;
import org.structr.mqtt.MQTTContext;
import org.structr.mqtt.MQTTInfo;
import org.structr.mqtt.entity.relation.MQTTClientHAS_SUBSCRIBERMQTTSubscriber;
import org.structr.rest.RestMethodResult;
import org.structr.schema.SchemaService;

public interface MQTTClient extends NodeInterface, MQTTInfo {

	static class Impl { static { SchemaService.registerMixinType(MQTTClient.class); }}

	public static final Property<List<MQTTSubscriber>> subscribers = new EndNodes<>("subscribers", MQTTClientHAS_SUBSCRIBERMQTTSubscriber.class);
	public static final Property<String>               protocol    = new StringProperty("protocol").defaultValue("tcp://");
	public static final Property<String>               url         = new StringProperty("url");
	public static final Property<Integer>              port        = new IntProperty("port");
	public static final Property<Integer>              qos         = new IntProperty("qos").defaultValue(0);
	public static final Property<Boolean>              isEnabled   = new BooleanProperty("isEnabled");
	public static final Property<Boolean>              isConnected = new BooleanProperty("isConnected");

	public static final View defaultView = new View(MQTTClient.class, PropertyView.Public, id, type, subscribers, protocol, url, port, qos, isEnabled, isConnected);

	public static final View uiView = new View(MQTTClient.class, PropertyView.Ui,
		id, name, owner, type, createdBy, deleted, hidden, createdDate, lastModifiedDate, visibleToPublicUsers, visibleToAuthenticatedUsers, visibilityStartDate, visibilityEndDate,
        subscribers, protocol, url, port, qos, isEnabled, isConnected
	);

	@Override
	default boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		if (getProperty(isEnabled)) {

			MQTTContext.connect(this);
		}

		return NodeInterface.super.onCreation(securityContext, errorBuffer);
	}

	@Override
	default boolean onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		if (modificationQueue.isPropertyModified(this,protocol) || modificationQueue.isPropertyModified(this,url) || modificationQueue.isPropertyModified(this,port)) {

			MQTTContext.disconnect(this);
		}

		if(modificationQueue.isPropertyModified(this,isEnabled) || modificationQueue.isPropertyModified(this,protocol) || modificationQueue.isPropertyModified(this,url) || modificationQueue.isPropertyModified(this,port)){

			MQTTClientConnection connection = MQTTContext.getClientForId(getUuid());
			boolean enabled                 = getProperty(isEnabled);
			if (!enabled) {

				if (connection != null && connection.isConnected()) {

					MQTTContext.disconnect(this);
					setProperties(securityContext, new PropertyMap(isConnected, false));
				}

			} else {

				if (connection == null || !connection.isConnected()) {

					MQTTContext.connect(this);
					MQTTContext.subscribeAllTopics(this);
				}

				connection = MQTTContext.getClientForId(getUuid());
				if (connection != null) {

					if (connection.isConnected()) {

						setProperties(securityContext, new PropertyMap(isConnected, true));
					} else {

						setProperties(securityContext, new PropertyMap(isConnected, false));
					}
				}
			}
		}

		return NodeInterface.super.onModification(securityContext, errorBuffer, modificationQueue);
	}

	@Override
	default boolean onDeletion(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {

		final String uuid = properties.get(id);
		if (uuid != null) {

			final MQTTClientConnection connection = MQTTContext.getClientForId(uuid);
			if (connection != null) {

				connection.disconnect();
			}
		}

		return NodeInterface.super.onDeletion(securityContext, errorBuffer, properties);
	}

	@Override
	default String getProtocol() {
		return getProperty(MQTTClient.protocol);
	}

	@Override
	default String getUrl() {
		return getProperty(MQTTClient.url);
	}

	@Override
	default int getPort() {
		return getProperty(MQTTClient.port);
	}

	@Override
	default int getQoS() {
		return getProperty(MQTTClient.qos);
	}

	@Override
	default void messageCallback(String topic, String message) {


		final App app = StructrApp.getInstance();
		try (final Tx tx = app.tx()) {

		List<MQTTSubscriber> subs = getProperty(MQTTClient.subscribers);

			for(MQTTSubscriber sub : subs) {

				String subTopic = sub.getProperty(MQTTSubscriber.topic);
				if(!StringUtils.isEmpty(subTopic)) {

					if(subTopic.equals(topic)){

						Map<String,Object> params = new HashMap<>();
						params.put("topic", topic);
						params.put("message", message);

						try {

							sub.invokeMethod("onMessage", params, false);
						} catch (FrameworkException ex) {

							logger.warn("Error while calling onMessage callback for MQTT subscriber.");
						}
					}
				}
			}

			tx.success();
		} catch (FrameworkException ex) {

			logger.error("Could not handle message callback for MQTT subscription.");
		}

	}

	@Override
	default void connectionStatusCallback(boolean connected) {

		final App app = StructrApp.getInstance();
		try(final Tx tx = app.tx()) {

			setProperty(isConnected, connected);
			tx.success();

		} catch (FrameworkException ex) {

			logger.warn("Error in connection status callback for MQTTClient.");
		}

	}

	@Override
	default String[] getTopics() {

		final App app = StructrApp.getInstance();
		try (final Tx tx = app.tx()) {

			List<MQTTSubscriber> subs = getProperty(subscribers);
			String[] topics = new String[subs.size()];

			for(int i = 0; i < subs.size(); i++) {

				topics[i] = subs.get(i).getProperty(MQTTSubscriber.topic);
			}

			tx.success();

			return topics;

		} catch (FrameworkException ex ) {

			logger.error("Couldn't retrieve client topics for MQTT subscription.");
			return null;
		}

	}

	@Export
	default RestMethodResult sendMessage(final String topic, final String message) throws FrameworkException {

		if (getProperty(isEnabled)) {

			final MQTTClientConnection connection = MQTTContext.getClientForId(getUuid());
			if (connection.isConnected()) {

				connection.sendMessage(topic, message);

			} else {

				throw new FrameworkException(422, "Not connected.");
			}
		}

		return new RestMethodResult(200);
	}

	@Export
	default RestMethodResult subscribeTopic(final String topic) throws FrameworkException {

		if (getProperty(isEnabled)) {

			final MQTTClientConnection connection = MQTTContext.getClientForId(getUuid());
			if (connection.isConnected()) {

				connection.subscribeTopic(topic);

			} else {

				throw new FrameworkException(422, "Not connected.");
			}
		}

		return new RestMethodResult(200);
	}

	@Export
	default RestMethodResult unsubscribeTopic(final String topic) throws FrameworkException {

		if (getProperty(isEnabled)) {

			final MQTTClientConnection connection = MQTTContext.getClientForId(getUuid());
			if (connection.isConnected()) {

				connection.unsubscribeTopic(topic);

			} else {

				throw new FrameworkException(422, "Not connected.");
			}
		}

		return new RestMethodResult(200);
	}

}
