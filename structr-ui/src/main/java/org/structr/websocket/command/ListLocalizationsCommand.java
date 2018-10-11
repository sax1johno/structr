/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.websocket.command;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.Page;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

/**
 * Websocket command to retrieve DOM nodes which are not attached to a parent element.
 */
public class ListLocalizationsCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(ListActiveElementsCommand.class.getName());

	static {

		StructrWebSocket.addCommand(ListLocalizationsCommand.class);
	}


	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(false);

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final App app                         = StructrApp.getInstance(securityContext);
		final String id                       = webSocketData.getId();
		final String locale                   = webSocketData.getNodeDataStringValue("locale");

		try (final Tx tx = app.tx(true, true, false)) {

			final Page page                = app.get(Page.class, id);

			if (page != null) {

				// using this, we differentiate in the localize() function how to proceed
				securityContext.setAccessMode(AccessMode.Backend);

				final RenderContext rCtx = new RenderContext(securityContext);
				securityContext.setRequest(getWebSocket().getRequest());
				getWebSocket().getRequest().getParameterMap().put("locale", new String[]{ locale });
				rCtx.setLocale(securityContext.getEffectiveLocale());

				Page.render(page, rCtx, 0);

				final List<GraphObjectMap> result = rCtx.getContextStore().getRequestedLocalizations();

				// set full result list
				webSocketData.setResult(result);
				webSocketData.setRawResultCount(result.size());

				// send only over local connection
				getWebSocket().send(webSocketData, true);

			} else {

				getWebSocket().send(MessageBuilder.status().code(404).message("Page with ID " + id + " not found.").build(), true);
			}

		} catch (FrameworkException fex) {

			logger.warn("Exception occured", fex);
			getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);
		}
	}

	@Override
	public String getCommand() {

		return "LIST_LOCALIZATIONS";

	}
}