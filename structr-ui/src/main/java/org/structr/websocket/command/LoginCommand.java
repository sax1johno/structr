/**
 * Copyright (C) 2010-2019 Structr GmbH
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

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.Authenticator;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.auth.exception.PasswordChangeRequiredException;
import org.structr.core.auth.exception.TooManyFailedLoginAttemptsException;
import org.structr.core.auth.exception.TwoFactorAuthenticationFailedException;
import org.structr.core.auth.exception.TwoFactorAuthenticationRequiredException;
import org.structr.core.auth.exception.TwoFactorAuthenticationTokenInvalidException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;
import org.structr.rest.auth.AuthHelper;
import org.structr.rest.auth.SessionHelper;
import org.structr.schema.action.ActionContext;
import org.structr.web.function.BarcodeFunction;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- classes ----------------------------------------------------------------
/**
 *
 *
 */
public class LoginCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(LoginCommand.class.getName());

	static {

		StructrWebSocket.addCommand(LoginCommand.class);

	}

	//~--- methods --------------------------------------------------------
	@Override
	public void processMessage(final WebSocketMessage webSocketData) throws FrameworkException {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx(true, true, true)) {

			final String username       = webSocketData.getNodeDataStringValue("username");
			final String password       = webSocketData.getNodeDataStringValue("password");
			final String twoFactorToken = webSocketData.getNodeDataStringValue("twoFactorToken");
			final String twoFactorCode  = webSocketData.getNodeDataStringValue("twoFactorCode");
			Principal user = null;

			try {

				Authenticator auth = getWebSocket().getAuthenticator();

				if (StringUtils.isNotEmpty(twoFactorToken)) {

					user = AuthHelper.getUserForTwoFactorToken(twoFactorToken);

				} else if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {

					user = auth.doLogin(getWebSocket().getRequest(), username, password);

					tx.setSecurityContext(SecurityContext.getInstance(user, AccessMode.Backend));
				}

				if (user != null) {

					final boolean twoFactorAuthenticationSuccessOrNotNecessary = AuthHelper.handleTwoFactorAuthentication(user, twoFactorCode, twoFactorToken, ActionContext.getRemoteAddr(getWebSocket().getRequest()));

					if (twoFactorAuthenticationSuccessOrNotNecessary) {

						String sessionId = webSocketData.getSessionId();
						if (sessionId == null) {

							logger.debug("Unable to login {}: No sessionId found", new Object[]{ username, password });
							getWebSocket().send(MessageBuilder.status().code(403).build(), true);

						} else {

							sessionId = SessionHelper.getShortSessionId(sessionId);

							// Clear possible existing sessions
							SessionHelper.clearSession(sessionId);
							user.addSessionId(sessionId);

							AuthHelper.sendLoginNotification(user);

							// store token in response data
							webSocketData.getNodeData().clear();
							webSocketData.setSessionId(sessionId);
							webSocketData.getNodeData().put("username", user.getProperty(AbstractNode.name));

							// authenticate socket
							getWebSocket().setAuthenticated(sessionId, user);

							tx.setSecurityContext(getWebSocket().getSecurityContext());

							// send data..
							getWebSocket().send(webSocketData, false);
						}
					}

				} else {

					getWebSocket().send(MessageBuilder.status().code(401).build(), true);

				}

			} catch (PasswordChangeRequiredException | TooManyFailedLoginAttemptsException | TwoFactorAuthenticationFailedException | TwoFactorAuthenticationTokenInvalidException ex) {

				logger.info(ex.getMessage());
				getWebSocket().send(MessageBuilder.status().message(ex.getMessage()).code(401).data("reason", ex.getReason()).build(), true);

			} catch (TwoFactorAuthenticationRequiredException ex) {

				logger.debug(ex.getMessage());

				final MessageBuilder msg = MessageBuilder.status().message(ex.getMessage()).data("token", ex.getNextStepToken());

				if (ex.showQrCode()) {

					try {

						final Map<String, Object> hints = new HashMap();
						hints.put("MARGIN", 0);
						hints.put("ERROR_CORRECTION", "M");

						final String qrdata = Base64.getEncoder().encodeToString(BarcodeFunction.getQRCode(Principal.getTwoFactorUrl(user), "QR_CODE", 200, 200, hints).getBytes("ISO-8859-1"));

						msg.data("qrdata", qrdata);

					} catch (UnsupportedEncodingException uee) {
						logger.warn("Charset ISO-8859-1 not supported!?", uee);
					}
				}

				getWebSocket().send(msg.code(202).build(), true);

			} catch (AuthenticationException e) {

				logger.info("Unable to login {}, probably wrong password", username);
				getWebSocket().send(MessageBuilder.status().code(403).build(), true);

			} catch (FrameworkException fex) {

				logger.warn("Unable to execute command", fex);
				getWebSocket().send(MessageBuilder.status().code(401).build(), true);

			}

			tx.success();
		}
	}

	//~--- get methods ----------------------------------------------------
	@Override
	public String getCommand() {
		return "LOGIN";
	}

	@Override
	public boolean requiresEnclosingTransaction () {
		return false;
	}
}
