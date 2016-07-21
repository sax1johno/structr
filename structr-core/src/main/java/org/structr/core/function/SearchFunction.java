/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.util.Map;
import java.util.logging.Level;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class SearchFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_SEARCH    = "Usage: ${search(type, key, value)}. Example: ${search(\"User\", \"name\", \"abc\")}";
	public static final String ERROR_MESSAGE_SEARCH_JS = "Usage: ${{Structr.search(type, key, value)}}. Example: ${{Structr.search(\"User\", \"name\", \"abc\")}}";

	@Override
	public String getName() {
		return "search()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (sources != null) {

			final SecurityContext securityContext = entity != null ? entity.getSecurityContext() : ctx.getSecurityContext();
			final ConfigurationProvider config = StructrApp.getConfiguration();
			final Query query = StructrApp.getInstance(securityContext).nodeQuery();
			Class type = null;

			if (sources.length >= 1 && sources[0] != null) {

				final String typeString = sources[0].toString();
				type = config.getNodeEntityClass(typeString);

				if (type != null) {

					query.andTypes(type);

				} else {

					logger.log(Level.WARNING, "Error in search(): type {0} not found.", typeString);
					return "Error in search(): type " + typeString + " not found.";

				}
			}

			// exit gracefully instead of crashing..
			if (type == null) {
				logger.log(Level.WARNING, "Error in search(): no type specified. Parameters: {0}", getParametersAsString(sources));
				return "Error in search(): no type specified.";
			}

			// experimental: disable result count, prevents instantiation
			// of large collections just for counting all the objects..
			securityContext.ignoreResultCount(true);

			// extension for native javascript objects
			if (sources.length == 2 && sources[1] instanceof Map) {

				final PropertyMap map = PropertyMap.inputTypeToJavaType(securityContext, type, (Map)sources[1]);
				for (final Map.Entry<PropertyKey, Object> entry : map.entrySet()) {

					query.and(entry.getKey(), entry.getValue(), false);
				}

			} else {

				final Integer parameter_count = sources.length;

				if (parameter_count % 2 == 0) {

					throw new FrameworkException(400, "Invalid number of parameters: " + parameter_count + ". Should be uneven: " + ERROR_MESSAGE_SEARCH);
				}

				for (Integer c = 1; c < parameter_count; c += 2) {

					final PropertyKey key = config.getPropertyKeyForJSONName(type, sources[c].toString());

					if (key != null) {

						// throw exception if key is not indexed (otherwise the user will never know)
						if (!key.isSearchable()) {

							throw new FrameworkException(400, "Search key " + key.jsonName() + " is not indexed.");
						}

						final PropertyConverter inputConverter = key.inputConverter(securityContext);
						Object value = sources[c + 1];

						if (inputConverter != null) {

							value = inputConverter.convert(value);
						}

						query.and(key, value, false);
					}

				}
			}

			final Object x = query.getAsList();

			// return search results
			return x;
			
		} else {
			logParameterError(entity, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_SEARCH_JS : ERROR_MESSAGE_SEARCH);
	}

	@Override
	public String shortDescription() {
		return "Returns a collection of entities of the given type from the database, takes optional key/value pairs. Searches case-insensitve / inexact.";
	}
}
