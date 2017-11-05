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
package org.structr.xmpp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.EndNodes;
import org.structr.core.property.EnumProperty;
import org.structr.core.property.FunctionProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;

/**
 *
 *
 */
public interface XMPPClient extends NodeInterface, XMPPInfo {

	public static final Property<List<XMPPRequest>> pendingRequests = new EndNodes<>("pendingRequests", XMPPClientRequest.class);
	public static final Property<String>            xmppHandle      = new FunctionProperty("xmppHandle").format("concat(this.xmppUsername, '@', this.xmppHost)").indexed();
	public static final Property<String>            xmppUsername    = new StringProperty("xmppUsername").indexed();
	public static final Property<String>            xmppPassword    = new StringProperty("xmppPassword");
	public static final Property<String>            xmppService     = new StringProperty("xmppService");
	public static final Property<String>            xmppHost        = new StringProperty("xmppHost");
	public static final Property<Integer>           xmppPort        = new IntProperty("xmppPort");
	public static final Property<Mode>              presenceMode    = new EnumProperty("presenceMode", Mode.class, Mode.available);
	public static final Property<Boolean>           isEnabled       = new BooleanProperty("isEnabled");
	public static final Property<Boolean>           isConnected     = new BooleanProperty("isConnected");

	public static final View publicView = new View(XMPPClient.class, PropertyView.Public,
		xmppHandle, xmppUsername, xmppPassword, xmppService, xmppHost, xmppPort, presenceMode, isEnabled, isConnected, pendingRequests
	);

	public static final View uiView = new View(XMPPClient.class, PropertyView.Ui,
		xmppHandle, xmppUsername, xmppPassword, xmppService, xmppHost, xmppPort, presenceMode, isEnabled, isConnected, pendingRequests
	);

	// ----- static methods -----
	public static void onMessage(final String uuid, final Message message) {

		final App app = StructrApp.getInstance();
		try (final Tx tx = app.tx()) {

			final XMPPClient client = StructrApp.getInstance().get(XMPPClient.class, uuid);
			if (client != null) {

				final String callbackName            = "onXMPP" + message.getClass().getSimpleName();
				final Map<String, Object> properties = new HashMap<>();

				properties.put("sender", message.getFrom());
				properties.put("message", message.getBody());

				client.invokeMethod(callbackName, properties, false);
			}

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
		}
	}

	public static void onRequest(final String uuid, final IQ request) {

		final App app = StructrApp.getInstance();
		try (final Tx tx = app.tx()) {

			final XMPPClient client = StructrApp.getInstance().get(XMPPClient.class, uuid);
			if (client != null) {

				app.create(XMPPRequest.class,
					new NodeAttribute(XMPPRequest.client, client),
					new NodeAttribute(XMPPRequest.sender, request.getFrom()),
					new NodeAttribute(XMPPRequest.owner, client.getProperty(XMPPClient.owner)),
					new NodeAttribute(XMPPRequest.content, request.toXML().toString()),
					new NodeAttribute(XMPPRequest.requestType, request.getType())
				);
			}

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
		}
	}
}
