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
package org.structr.ldap;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Group;
import org.structr.core.property.PropertyKey;
import org.structr.schema.SchemaService;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;
import org.structr.web.entity.User;

/**
 *
 */
public interface LDAPUser extends User {

	static final Logger logger = LoggerFactory.getLogger(LDAPGroup.class);

	static class Impl { static {

		final JsonSchema schema    = SchemaService.getDynamicSchema();
		final JsonObjectType type  = schema.addType("LDAPUser");

		type.setExtends(schema.getType("User"));
		type.setImplements(URI.create("https://structr.org/v1.1/definitions/LDAPUser"));

		type.addStringProperty("distinguishedName", PropertyView.Public, PropertyView.Ui).setUnique(true).setIndexed(true);
		type.addLongProperty("lastLDAPSync");

		type.addPropertyGetter("distinguishedName", String.class);
		type.addPropertySetter("distinguishedName", String.class);

		type.overrideMethod("onAuthenticate",   true, LDAPUser.class.getName() + ".onAuthenticate(this);");
		type.overrideMethod("initializeFrom",  false, LDAPUser.class.getName() + ".initializeFrom(this, arg0);");
		type.overrideMethod("isValidPassword", false, "return " + LDAPUser.class.getName() + ".isValidPassword(this, arg0);");
	}}

	String getDistinguishedName();

	void initializeFrom(final Entry entry) throws FrameworkException;
	void setDistinguishedName(final String distinguishedName) throws FrameworkException;

	static void initializeFrom(final LDAPUser thisUser, final Entry entry) throws FrameworkException {

		final LDAPService ldapService      = Services.getInstance().getService(LDAPService.class);
		final Map<String, String> mappings = new LinkedHashMap<>();

		if (ldapService != null) {

			mappings.putAll(ldapService.getPropertyMapping());
		}

		try {

			// apply mappings
			for (final String key : mappings.keySet()) {

				final String structrName = mappings.get(key);
				final String ldapName    = key;

				thisUser.setProperty(StructrApp.key(LDAPUser.class, structrName), LDAPUser.getString(entry, ldapName));
			}

			// update lastUpdate timestamp
			thisUser.setProperty(StructrApp.key(LDAPUser.class, "lastLDAPSync"), System.currentTimeMillis());


		} catch (final LdapInvalidAttributeValueException ex) {
			ex.printStackTrace();
		}
	}

	static boolean isValidPassword(final LDAPUser thisUser, final String password) {

		final LDAPService ldapService = Services.getInstance().getService(LDAPService.class);
		final String dn               = thisUser.getDistinguishedName();
		boolean hasLDAPGroups         = false;

		if (ldapService != null) {

			// delete this user if there is no group association left
			for (final Group group : Iterables.toList(thisUser.getGroups())) {

				if (group instanceof LDAPGroup) {

					hasLDAPGroups = true;
					break;
				}
			}

			if (!hasLDAPGroups) {

				logger.info("LDAPUser {} with UUID {} is not associated with an LDAPGroup, authentication denied.", thisUser.getName(), thisUser.getUuid());
				return false;
			}

			return ldapService.canSuccessfullyBind(dn, password);

		} else {

			logger.warn("Unable to reach LDAP server for authentication of {}", dn);
		}

		return false;
	}

	static void onAuthenticate(final LDAPUser thisUser) {

		final PropertyKey<Long> lastUpdateKey = StructrApp.key(LDAPUser.class, "lastLDAPSync");
		final Long lastUpdate = thisUser.getProperty(lastUpdateKey);

		if ((lastUpdate == null || System.currentTimeMillis() > (lastUpdate + (Settings.LDAPUpdateInterval.getValue(600) * 1000)))) {

			try {

				// update all LDAP groups..
				logger.info("Updating LDAP information for {} ({})", thisUser.getName(), thisUser.getProperty(StructrApp.key(LDAPUser.class, "distinguishedName")));

				final LDAPService service = Services.getInstance().getService(LDAPService.class);
				if (service != null) {

						for (final LDAPGroup group : StructrApp.getInstance().nodeQuery(LDAPGroup.class).getAsList()) {

							service.synchronizeGroup(group);
						}
				}

				thisUser.setProperty(lastUpdateKey, System.currentTimeMillis());

			} catch (CursorException | LdapException | IOException | FrameworkException fex) {
				logger.warn("Unable to update LDAP information for user {}: {}", thisUser.getName(), fex.getMessage());
			}
		}
	}

	static String getString(final Entry entry, final String key) throws LdapInvalidAttributeValueException {

		final Attribute attribute = entry.get(key);
		if (attribute != null) {

			return attribute.getString();
		}

		return null;
	}
}
