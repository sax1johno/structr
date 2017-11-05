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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.core.Export;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;

/**
 *
 */
public class LDAPUserMixin extends AbstractNode implements LDAPUser {

	private static final Logger logger = LoggerFactory.getLogger(LDAPUserMixin.class);

	@Override
	public boolean isValidPassword(final String password) {

		final LDAPService ldapService = Services.getInstance().getService(LDAPService.class);
		final String dn               = getProperty(distinguishedName);

		if (ldapService != null) {

			return ldapService.canSuccessfullyBind(dn, password);

		} else {

			logger.warn("Unable to reach LDAP server for authentication of {}", dn);
		}

		return false;
	}

	@Export
	public void printDebug() {

		final LDAPService ldapService = Services.getInstance().getService(LDAPService.class);
		final String dn               = getProperty(distinguishedName);

		if (ldapService != null) {

			System.out.println(ldapService.fetchObjectInfo(dn));

		} else {

			logger.warn("Unable to reach LDAP server for user information of {}", dn);
		}
	}
}
