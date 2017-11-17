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

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.ValidationHelper;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.function.LocalizeFunction;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.schema.SchemaService;

public interface Localization extends NodeInterface {

	static class Impl { static { SchemaService.registerMixinType(Localization.class); }}

	public static final Property<String>  localizedName = new StringProperty("localizedName").cmis().indexed();
	public static final Property<String>  description   = new StringProperty("description");
	public static final Property<String>  domain        = new StringProperty("domain").cmis().indexed();
	public static final Property<String>  locale        = new StringProperty("locale").notNull().cmis().indexed();
	public static final Property<Boolean> imported      = new BooleanProperty("imported").cmis().indexed();

	public static final View defaultView = new View(Localization.class, PropertyView.Public,
		domain, name, locale, localizedName, description, imported
	);

	public static final View uiView = new View(Localization.class, PropertyView.Ui,
		domain, name, locale, localizedName, description, imported
	);

	@Override
	default boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		if (NodeInterface.super.onCreation(securityContext, errorBuffer)) {

			setProperty(visibleToPublicUsers, true);
			setProperty(visibleToAuthenticatedUsers, true);

			LocalizeFunction.invalidateCache();

			return true;
		}

		return false;
	}

	@Override
	default boolean onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		if (NodeInterface.super.onModification(securityContext, errorBuffer, modificationQueue)) {

			LocalizeFunction.invalidateCache();
			return true;
		}

		return false;
	}

	@Override
	default boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = NodeInterface.super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidStringNotBlank(this, Localization.name, errorBuffer);
		valid &= ValidationHelper.isValidStringNotBlank(this, Localization.locale, errorBuffer);

		return valid;
	}
}
