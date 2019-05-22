/**
 * Copyright (C) 2010-2019 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.auth;

import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.auth.exception.PasswordChangeRequiredException;
import org.structr.core.auth.exception.TooManyFailedLoginAttemptsException;
import org.structr.core.auth.exception.TwoFactorAuthenticationFailedException;
import org.structr.core.auth.exception.TwoFactorAuthenticationRequiredException;
import org.structr.core.auth.exception.TwoFactorAuthenticationTokenInvalidException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SuperUser;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.schema.action.Actions;


/**
 * Utility class for authentication.
 */
public class AuthHelper {

	public static final String STANDARD_ERROR_MSG = "Wrong username or password, or user is blocked. Check caps lock. Note: Username is case sensitive!";
	private static final Logger logger            = LoggerFactory.getLogger(AuthHelper.class.getName());

	/**
	 * Find a {@link Principal} for the given credential
	 *
	 * @param key
	 * @param value
	 * @return principal
	 */
	public static <T> Principal getPrincipalForCredential(final PropertyKey<T> key, final T value) {

		return getPrincipalForCredential(key, value, false);

	}

	public static <T> Principal getPrincipalForCredential(final PropertyKey<T> key, final T value, final boolean isPing) {

		if (value != null) {

			try {

				return StructrApp.getInstance().nodeQuery(Principal.class).and(key, value).disableSorting().isPing(isPing).getFirst();

			} catch (FrameworkException fex) {

				logger.warn("Error while searching for principal: {}", fex.getMessage());
			}
		}

		return null;
	}

	/**
	 * Find a {@link Principal} with matching password and given key or name
	 *
	 * @param key
	 * @param value
	 * @param password
	 * @return principal
	 * @throws AuthenticationException
	 */
	public static Principal getPrincipalForPassword(final PropertyKey<String> key, final String value, final String password) throws AuthenticationException, TooManyFailedLoginAttemptsException, PasswordChangeRequiredException {

		Principal principal  = null;

		final String superuserName = Settings.SuperUserName.getValue();
		final String superUserPwd  = Settings.SuperUserPassword.getValue();

		if (StringUtils.isEmpty(value)) {

			logger.info("Empty value for key {}", key.dbName());
			throw new AuthenticationException(STANDARD_ERROR_MSG);
		}

		if (StringUtils.isEmpty(password)) {

			logger.info("Empty password");
			throw new AuthenticationException(STANDARD_ERROR_MSG);
		}

		if (superuserName.equals(value) && superUserPwd.equals(password)) {

			// logger.info("############# Authenticated as superadmin! ############");

			principal = new SuperUser();

		} else {

			try {

				principal = StructrApp.getInstance().nodeQuery(Principal.class).and().or(key, value).or(AbstractNode.name, value).disableSorting().getFirst();

			} catch (FrameworkException fex) {

				logger.warn("", fex);
			}

			if (principal == null) {

				logger.info("No principal found for {} {}", key.dbName(), value);

				throw new AuthenticationException(STANDARD_ERROR_MSG);

			} else {

				if (principal.isBlocked()) {

					logger.info("Principal {} is blocked", principal);

					throw new AuthenticationException(STANDARD_ERROR_MSG);
				}

				// let Principal decide how to check password
				final boolean passwordValid = principal.isValidPassword(password);

				if (!passwordValid) {

					AuthHelper.incrementFailedLoginAttemptsCounter(principal);
				}

				AuthHelper.checkTooManyFailedLoginAttempts(principal);

				if (!passwordValid) {

					throw new AuthenticationException(STANDARD_ERROR_MSG);

				} else {

					AuthHelper.handleForcePasswordChange(principal);
					AuthHelper.resetFailedLoginAttemptsCounter(principal);

					// allow external users (LDAP etc.) to update group membership
					principal.onAuthenticate();
				}
			}
		}

		return principal;
	}

	/**
	 * Find a {@link Principal} for the given session id
	 *
	 * @param sessionId
	 * @return principal
	 */
	public static Principal getPrincipalForSessionId(final String sessionId) {

		return getPrincipalForSessionId(sessionId, false);

	}

	public static Principal getPrincipalForSessionId(final String sessionId, final boolean isPing) {

		return getPrincipalForCredential(StructrApp.key(Principal.class, "sessionIds"), new String[]{ sessionId }, isPing);

	}

	public static void doLogin(final HttpServletRequest request, final Principal user) throws FrameworkException {

		if (request.getSession(false) == null) {
			SessionHelper.newSession(request);
		}

		SessionHelper.clearInvalidSessions(user);

		// We need a session to login a user
		if (request.getSession(false) != null) {

			final String sessionId = request.getSession(false).getId();

			SessionHelper.clearSession(sessionId);
			user.addSessionId(sessionId);

			AuthHelper.sendLoginNotification(user);
		}
	}

	public static void doLogout(final HttpServletRequest request, final Principal user) throws FrameworkException {

		final String sessionId = SessionHelper.getShortSessionId(request.getRequestedSessionId());

		if (sessionId == null) return;

		SessionHelper.clearSession(sessionId);
		SessionHelper.invalidateSession(sessionId);

		AuthHelper.sendLogoutNotification(user);
	}

	public static void sendLoginNotification (final Principal user) throws FrameworkException {

		try {

			final Map<String, Object> params = new HashMap<>();
			params.put("user", user);

			Actions.callAsSuperUser(Actions.NOTIFICATION_LOGIN, params);

		} catch (UnlicensedScriptException ex) {
			ex.log(logger);
		}
	}

	public static void sendLogoutNotification (final Principal user) throws FrameworkException {

		try {

			final Map<String, Object> params = new HashMap<>();
			params.put("user", user);

			Actions.callAsSuperUser(Actions.NOTIFICATION_LOGOUT, params);

		} catch (UnlicensedScriptException ex) {
				ex.log(logger);
		}
	}

	/**
	 * @return A confirmation key with the current timestamp
	 */
	public static String getConfirmationKey() {

		return UUID.randomUUID().toString() + "!" + new Date().getTime();
	}

	/**
	 * Determines if the key is valid or not. If the key has no timestamp the configuration setting for keys without timestamp is used
	 *
	 * @param confirmationKey The confirmation key to check
	 * @param validityPeriod The validity period for the key (in minutes)
	 * @return
	 */
	public static boolean isConfirmationKeyValid(final String confirmationKey, final Integer validityPeriod) {

		final String[] parts = confirmationKey.split("!");

		if (parts.length == 2) {

			final long confirmationKeyCreated = Long.parseLong(parts[1]);
			final long maxValidity            = confirmationKeyCreated + validityPeriod * 60 * 1000;

			return (maxValidity >= new Date().getTime());
		}

		return Settings.ConfirmationKeyValidWithoutTimestamp.getValue();
	}

	public static void incrementFailedLoginAttemptsCounter (final Principal principal) {

		try {

			final PropertyKey<Integer> passwordAttemptsKey = StructrApp.key(Principal.class, "passwordAttempts");

			Integer failedAttempts = principal.getProperty(passwordAttemptsKey);

			if (failedAttempts == null) {
				failedAttempts = 0;
			}

			failedAttempts++;

			principal.setProperty(passwordAttemptsKey, failedAttempts);

		} catch (FrameworkException fex) {

			logger.warn("Exception while incrementing failed login attempts counter", fex);
		}
	}

	public static void checkTooManyFailedLoginAttempts (final Principal principal) throws TooManyFailedLoginAttemptsException {

		final PropertyKey<Integer> passwordAttemptsKey = StructrApp.key(Principal.class, "passwordAttempts");
		final int maximumAllowedFailedAttempts = Settings.PasswordAttempts.getValue();

		if (maximumAllowedFailedAttempts > 0) {

			Integer failedAttempts = principal.getProperty(passwordAttemptsKey);

			if (failedAttempts == null) {
				failedAttempts = 0;
			}

			if (failedAttempts > maximumAllowedFailedAttempts) {

				throw new TooManyFailedLoginAttemptsException();
			}
		}
	}

	public static void resetFailedLoginAttemptsCounter (final Principal principal) {

		try {

			principal.setProperty(StructrApp.key(Principal.class, "passwordAttempts"), 0);

		} catch (FrameworkException fex) {

			logger.warn("Exception while resetting failed login attempts counter", fex);
		}
	}

	public static void handleForcePasswordChange (final Principal principal) throws PasswordChangeRequiredException {

		final boolean forcePasswordChange = Settings.PasswordForceChange.getValue();

		if (forcePasswordChange) {

			final PropertyKey<Date> passwordChangeDateKey  = StructrApp.key(Principal.class, "passwordChangeDate");
			final int passwordDays = Settings.PasswordForceChangeDays.getValue();

			final Date now = new Date();
			final Date passwordChangeDate = (principal.getProperty(passwordChangeDateKey) != null) ? principal.getProperty(passwordChangeDateKey) : new Date (0); // setting date in past if not yet set
			final int daysApart = (int) ((now.getTime() - passwordChangeDate.getTime()) / (1000 * 60 * 60 * 24l));

			if (daysApart > passwordDays) {

				throw new PasswordChangeRequiredException();
			}
		}
	}

	public static Principal getUserForTwoFactorToken (final String twoFactorIdentificationToken) throws TwoFactorAuthenticationTokenInvalidException, FrameworkException {

		final App app = StructrApp.getInstance();

		Principal principal = null;

		final PropertyKey<String> twoFactorTokenKey   = StructrApp.key(Principal.class, "twoFactorToken");

		try (final Tx tx = app.tx()) {
			principal = app.nodeQuery(Principal.class).and(twoFactorTokenKey, twoFactorIdentificationToken).getFirst();
			tx.success();
		}

		if (principal != null) {

			if (!AuthHelper.isTwoFactorTokenValid(twoFactorIdentificationToken)) {

				principal.setProperty(twoFactorTokenKey, null);

				throw new TwoFactorAuthenticationTokenInvalidException();
			}
		}

		return principal;
	}

	public static boolean isTwoFactorTokenValid(final String twoFactorIdentificationToken) {

		final String[] parts = twoFactorIdentificationToken.split("!");

		if (parts.length == 2) {

			final long tokenCreatedTimestamp = Long.parseLong(parts[1]);
			final long maxTokenValidity      = tokenCreatedTimestamp + Settings.TwoFactorLoginTimeout.getValue() * 1000;

			return (maxTokenValidity >= new Date().getTime());
		}

		return false;
	}

	public static boolean isRequestingIPWhitelistedForTwoFactorAuthentication(final String requestIP) {

		final String whitelistedIPs = Settings.TwoFactorWhitelistedIPs.getValue();

		if (!StringUtils.isEmpty(whitelistedIPs) && !StringUtils.isEmpty(requestIP)) {
			for (final String whitelistedIP : whitelistedIPs.split(",")) {
				if (whitelistedIP.trim().equals(requestIP.split(":")[0])) {
					return true;
				}
			}
		}

		return false;
	}

	public static boolean handleTwoFactorAuthentication (final Principal principal, final String twoFactorCode, final String twoFactorToken, final String requestIP) throws FrameworkException, TwoFactorAuthenticationRequiredException, TwoFactorAuthenticationFailedException {

		if (!AuthHelper.isRequestingIPWhitelistedForTwoFactorAuthentication(requestIP)) {

			final PropertyKey<String> twoFactorTokenKey      = StructrApp.key(Principal.class, "twoFactorToken");
			final PropertyKey<Boolean> isTwoFactorUserKey    = StructrApp.key(Principal.class, "isTwoFactorUser");
			final PropertyKey<Boolean> twoFactorConfirmedKey = StructrApp.key(Principal.class, "twoFactorConfirmed");

			final int twoFactorLevel   = Settings.TwoFactorLevel.getValue();
			boolean isTwoFactorUser    = principal.getProperty(isTwoFactorUserKey);
			boolean twoFactorConfirmed = principal.getProperty(twoFactorConfirmedKey);

			boolean userNeedsTwoFactor = twoFactorLevel == 2 || (twoFactorLevel == 1 && isTwoFactorUser == true);

			if (userNeedsTwoFactor) {

				if (twoFactorToken == null) {

					// user just logged in via username/password - no two factor identification token

					final String newTwoFactorToken = AuthHelper.getIdentificationTokenForPrincipal();
					principal.setProperty(twoFactorTokenKey, newTwoFactorToken);

					throw new TwoFactorAuthenticationRequiredException(newTwoFactorToken, !twoFactorConfirmed);

				} else {

					try {

						final String currentKey = TimeBasedOneTimePasswordHelper.generateCurrentNumberString(Principal.getTwoFactorSecret(principal), AuthHelper.getCryptoAlgorithm(), Settings.TwoFactorPeriod.getValue(), Settings.TwoFactorDigits.getValue());

						// check two factor authentication
						if (currentKey.equals(twoFactorCode)) {

							principal.setProperty(twoFactorTokenKey,     null);   // reset token
							principal.setProperty(twoFactorConfirmedKey, true);   // user has verified two factor use
							principal.setProperty(isTwoFactorUserKey,    true);

							logger.info("Successful two factor authentication ({})", principal.getName());

							return true;

						} else {

							// two factor authentication not successful
						   logger.info("Two factor authentication failed ({})", principal.getName());

						   throw new TwoFactorAuthenticationFailedException();
						}

					} catch (GeneralSecurityException ex) {

						logger.warn("Two factor authentication key could not be generated - login not possible");

						return false;
					}
				}
			}
		}

		return true;
	}

	public static String getIdentificationTokenForPrincipal () {
		return UUID.randomUUID().toString() + "!" + new Date().getTime();
	}

	// The StandardName for the given SHA algorithm.
	// see https://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#Mac
	private static String getCryptoAlgorithm() {
		return "Hmac" + Settings.TwoFactorAlgorithm.getValue();
	}
}
