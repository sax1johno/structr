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
package org.structr.ldap;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.web.entity.User;

/**
 *
 */
public interface LDAPUser extends User {

	public static final Property<String> distinguishedName = new StringProperty("distinguishedName").unique().indexed();
	public static final Property<String> description       = new StringProperty("description").indexed();
	public static final Property<String> commonName        = new StringProperty("commonName").indexed();
	public static final Property<String> entryUuid         = new StringProperty("entryUuid").unique().indexed();

	public static final org.structr.common.View uiView = new org.structr.common.View(LDAPUser.class, PropertyView.Ui,
		distinguishedName, entryUuid, commonName, description
	);

	public static final org.structr.common.View publicView = new org.structr.common.View(LDAPUser.class, PropertyView.Public,
		distinguishedName, entryUuid, commonName, description
	);

	default void initializeFrom(final Entry entry) throws FrameworkException, LdapInvalidAttributeValueException {

		setProperty(LDAPUserMixin.description, getString(entry, "description"));
		setProperty(LDAPUserMixin.entryUuid,   getString(entry, "entryUUID"));
		setProperty(LDAPUserMixin.name,        getString(entry, "uid"));
		setProperty(LDAPUserMixin.commonName,  getString(entry, "cn"));
		setProperty(LDAPUserMixin.eMail,       getString(entry, "mail"));
	}

	// ----- private methods -----
	default String getString(final Entry entry, final String key) throws LdapInvalidAttributeValueException {

		final Attribute attribute = entry.get(key);
		if (attribute != null) {

			return attribute.getString();
		}

		return null;
	}
}
