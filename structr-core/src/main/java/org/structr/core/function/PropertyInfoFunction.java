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
package org.structr.core.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.SchemaHelper;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class PropertyInfoFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_PROPERTY_INFO    = "Usage: ${property_info(type, name)}. Example ${property_info('User', 'name')}";
	public static final String ERROR_MESSAGE_PROPERTY_INFO_JS = "Usage: ${Structr.propertyInfo(type, name)}. Example ${Structr.propertyInfo('User', 'name')}";

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			if (!arrayHasLengthAndAllElementsNotNull(sources, 2)) {

				return null;
			}

			final ConfigurationProvider config = StructrApp.getConfiguration();
			final String typeName = sources[0].toString();
			final String keyName = sources[1].toString();

			Class type = config.getNodeEntityClass(typeName);
			if (type == null) {

				type = config.getRelationshipEntityClass(typeName);
			}

			if (type != null) {

				final PropertyKey key = StructrApp.key(type, keyName);
				if (key != null) {

					return SchemaHelper.getPropertyInfo(ctx.getSecurityContext(), key);

				} else {

					logger.warn("Error: Unknown property \"{}.{}\". Parameters: {}", new Object[] { typeName, keyName, getParametersAsString(sources) });
					return "Unknown property " + typeName + "." + keyName;
				}

			} else {

				logger.warn("Error: Unknown type \"{}\". Parameters: {}", new Object[] { typeName, getParametersAsString(sources) });
				return "Unknown type " + typeName;
			}

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());

		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_PROPERTY_INFO_JS : ERROR_MESSAGE_PROPERTY_INFO);
	}

	@Override
	public String shortDescription() {
		return "Returns the schema information for the given property";
	}

	@Override
	public String getName() {
		return "schema_property()";
	}

}
