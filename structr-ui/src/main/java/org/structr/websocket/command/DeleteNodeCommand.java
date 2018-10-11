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

import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.LinkedTreeNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.web.entity.dom.DOMNode;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;
import org.w3c.dom.DOMException;

/**
 * Websocket command to delete a single node.
 */
public class DeleteNodeCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(DeleteNodeCommand.class.getName());

	static {

		StructrWebSocket.addCommand(DeleteNodeCommand.class);

	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);
		
		try {
			
			final Boolean recursive = webSocketData.getNodeDataBooleanValue("recursive");
			final NodeInterface obj = getNode(webSocketData.getId());

			if (obj != null) {

				TransactionCommand.registerNodeCallback((NodeInterface) obj, callback);

				deleteNode(getWebSocket(), obj, recursive);
			}
		} catch (FrameworkException ex) {
			logger.warn("Exception occured", ex);
			getWebSocket().send(MessageBuilder.status().code(ex.getStatus()).message(ex.getMessage()).build(), true);
		}
	}

	protected static void deleteNode(final StructrWebSocket ws, final NodeInterface obj, final Boolean recursive) {

		final SecurityContext securityContext = ws.getSecurityContext();

		final App app = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			if (!(obj.isGranted(Permission.delete, securityContext))) {

				logger.warn("No delete permission for {} on {}", new Object[]{ ws.getCurrentUser().toString(), obj.toString() });
				ws.send(MessageBuilder.status().message("No delete permission").code(400).build(), true);
				tx.success();

				return;

			}

		} catch (FrameworkException ex) {
			logger.warn("", ex);
		}

		if (Boolean.TRUE.equals(recursive)) {

			// Remove all child nodes first
			try {

				final List<NodeInterface> filteredResults = new LinkedList<>();
				if (obj instanceof DOMNode) {

					DOMNode node = (DOMNode) obj;

					filteredResults.addAll(DOMNode.getAllChildNodes(node));

				} else if (obj instanceof LinkedTreeNode) {

					LinkedTreeNode node = (LinkedTreeNode) obj;

					filteredResults.addAll(node.getAllChildNodes());
				}

				for (NodeInterface node : filteredResults) {
					app.delete(node);
				}

			} catch (FrameworkException fex) {

				logger.warn("Exception occured", fex);
				ws.send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);

			} catch (DOMException dex) {

				logger.warn("DOMException occured.", dex);
				ws.send(MessageBuilder.status().code(422).message(dex.getMessage()).build(), true);

			}

		}

		try {
			app.delete(obj);

		} catch (FrameworkException fex) {
			logger.warn("Unable to delete node(s)", fex);
		}
	}

	//~--- get methods ----------------------------------------------------
	@Override
	public String getCommand() {
		return "DELETE";
	}
}
