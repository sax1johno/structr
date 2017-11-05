/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.core.entity;

import org.structr.common.ValidationHelper;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UniqueToken;
import org.structr.core.Result;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.Mixin;
import org.structr.schema.SchemaService;

/**
 */
public class MailTemplateMixin extends AbstractNode implements MailTemplate, Mixin {

	static {
		SchemaService.registerMixinType("MailTemplate", AbstractNode.class, MailTemplate.class);
	}

	// ----- BEGIN Structr Mixin -----

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		String _name	= getProperty(name);
		String _locale	= getProperty(locale);
		String _uuid	= getProperty(id);

		valid &= ValidationHelper.isValidStringNotBlank(this, name, errorBuffer);
		valid &= ValidationHelper.isValidStringNotBlank(this, locale, errorBuffer);

		try {
			Result<MailTemplate> res = StructrApp.getInstance(securityContext).nodeQuery(MailTemplate.class).andName(_name).and(locale, _locale).getResult();
			if (res.size() > 1) {

				errorBuffer.add(new UniqueToken(MailTemplate.class.getName(), name, _uuid));
				errorBuffer.add(new UniqueToken(MailTemplate.class.getName(), locale, _uuid));

				valid = false;
			}

		} catch (FrameworkException fe) {

			logger.warn("Could not search a MailTemplate with name {} and locale {}", new Object[]{getProperty(name), getProperty(locale)});

		}

		return valid;

	}

	// ----- END Structr Mixin -----
}
