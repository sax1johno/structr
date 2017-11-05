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

import java.util.List;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.EndNodes;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.mqtt.MQTTInfo;
import org.structr.mqtt.entity.relation.MQTTClientHAS_SUBSCRIBERMQTTSubscriber;
import org.structr.rest.RestMethodResult;

public interface MQTTClient extends NodeInterface, MQTTInfo {

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

	RestMethodResult sendMessage(final String topic, final String message) throws FrameworkException;
	RestMethodResult subscribeTopic(final String topic) throws FrameworkException;
	RestMethodResult unsubscribeTopic(final String topic) throws FrameworkException;
}
