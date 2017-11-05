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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.PropertyMap;
import org.structr.rest.RestMethodResult;
import org.structr.schema.SchemaService;

/**
 *
 *
 */
public class XMPPClientMixin extends AbstractNode implements XMPPClient {

	private static final Logger logger = LoggerFactory.getLogger(XMPPClientMixin.class.getName());

	static {

		SchemaService.registerMixinType("XMPPClient", AbstractNode.class, XMPPClientMixin.class);
	}

	@Override
	public boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		if (getProperty(isEnabled)) {
			XMPPContext.connect(this);
		}

		return super.onCreation(securityContext, errorBuffer);
	}

	@Override
	public boolean onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		XMPPClientConnection connection = XMPPContext.getClientForId(getUuid());
		boolean enabled                 = getProperty(isEnabled);
		if (!enabled) {

			if (connection != null && connection.isConnected()) {
				connection.disconnect();
			}

		} else {

			if (connection == null || !connection.isConnected()) {
				XMPPContext.connect(this);
			}

			connection = XMPPContext.getClientForId(getUuid());
			if (connection != null) {

				if (connection.isConnected()) {

					setProperty(isConnected, true);
					connection.setPresence(getProperty(presenceMode));

				} else {

					setProperty(isConnected, false);
				}
			}
		}

		return super.onModification(securityContext, errorBuffer, modificationQueue);
	}

	@Override
	public boolean onDeletion(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {

		final String uuid = properties.get(id);
		if (uuid != null) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(uuid);
			if (connection != null) {

				connection.disconnect();
			}
		}

		return super.onDeletion(securityContext, errorBuffer, properties);
	}

	@Override
	public String getUsername() {
		return getProperty(xmppUsername);
	}

	@Override
	public String getPassword() {
		return getProperty(xmppPassword);
	}

	@Override
	public String getService() {
		return getProperty(xmppService);
	}

	@Override
	public String getHostName() {
		return getProperty(xmppHost);
	}

	@Override
	public int getPort() {
		return getProperty(xmppPort);
	}

	@Export
	public RestMethodResult doSendMessage(final String recipient, final String message) throws FrameworkException {

		if (getProperty(isEnabled)) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(getUuid());
			if (connection.isConnected()) {

				connection.sendMessage(recipient, message);

			} else {

				throw new FrameworkException(422, "Not connected.");
			}
		}

		return new RestMethodResult(200);
	}

	@Export
	public RestMethodResult doSubscribe(final String recipient) throws FrameworkException {

		if (getProperty(isEnabled)) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(getUuid());
			if (connection.isConnected()) {

				connection.subscribe(recipient);

			} else {

				throw new FrameworkException(422, "Not connected.");
			}
		}

		return new RestMethodResult(200);
	}

	@Export
	public RestMethodResult doUnsubscribe(final String recipient) throws FrameworkException {

		if (getProperty(isEnabled)) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(getUuid());
			if (connection.isConnected()) {

				connection.unsubscribe(recipient);

			} else {

				throw new FrameworkException(422, "Not connected.");
			}
		}

		return new RestMethodResult(200);
	}

	@Export
	public RestMethodResult doConfirmSubscription(final String recipient) throws FrameworkException {

		if (getProperty(isEnabled)) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(getUuid());
			if (connection.isConnected()) {

				connection.confirmSubscription(recipient);

			} else {

				throw new FrameworkException(422, "Not connected.");
			}
		}

		return new RestMethodResult(200);
	}

	@Export
	public RestMethodResult doDenySubscription(final String recipient) throws FrameworkException {

		if (getProperty(isEnabled)) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(getUuid());
			if (connection.isConnected()) {

				connection.denySubscription(recipient);

			} else {

				throw new FrameworkException(422, "Not connected.");
			}
		}

		return new RestMethodResult(200);
	}

	@Export
	public RestMethodResult doJoinChat(final String chatRoom, final String nickname, final String password) throws FrameworkException {

		if (getProperty(isEnabled)) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(getUuid());
			if (connection.isConnected()) {

				connection.joinChat(chatRoom, nickname, password);

			} else {

				throw new FrameworkException(422, "Not connected.");
			}
		}

		return new RestMethodResult(200);

	}

	@Export
	public RestMethodResult doSendChatMessage(final String chatRoom, final String message, final String password) throws FrameworkException {

		if (getProperty(isEnabled)) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(getUuid());
			if (connection.isConnected()) {

				connection.sendChatMessage(chatRoom, message, password);

			} else {

				throw new FrameworkException(422, "Not connected.");
			}
		}

		return new RestMethodResult(200);
	}
}
