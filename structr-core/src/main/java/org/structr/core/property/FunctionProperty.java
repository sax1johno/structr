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
package org.structr.core.property;

import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;

/**
 *
 *
 */
public class FunctionProperty<T> extends Property<T> {

	private static final Logger logger            = LoggerFactory.getLogger(FunctionProperty.class.getName());
	private static final BooleanProperty pBoolean = new BooleanProperty("pBoolean");
	private static final IntProperty pInt         = new IntProperty("pInt");
	private static final LongProperty pLong       = new LongProperty("pLong");
	private static final DoubleProperty pDouble   = new DoubleProperty("pDouble");
	private static final DateProperty pDate       = new DateProperty("pDate");

	public FunctionProperty(final String name) {
		super(name);
	}

	public FunctionProperty(final String name, final String dbName) {
		super(name, dbName);
	}

	@Override
	public Property<T> indexed() {

		super.indexed();
		super.passivelyIndexed();

		return this;
	}

	@Override
	public Property<T> setSourceUuid(final String sourceUuid) {
		this.sourceUuid = sourceUuid;
		return this;
	}

	@Override
	public Class relatedType() {
		return null;
	}

	@Override
	public T getProperty(final SecurityContext securityContext, final GraphObject obj, final boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public T getProperty(final SecurityContext securityContext, final GraphObject target, final boolean applyConverter, final Predicate<GraphObject> predicate) {

		try {

			if (!securityContext.doInnerCallbacks()) {
				return null;
			}

			final GraphObject obj     = PropertyMap.unwrap(target);
			final String readFunction = getReadFunction();

			if (obj != null) {

				// ignore empty read function, don't log error message (it's not an error)
				if (readFunction != null) {

					if (cachingEnabled) {

						Object cachedValue = securityContext.getContextStore().retrieveFunctionPropertyResult(obj.getUuid(), jsonName);

						if (cachedValue != null) {
							return (T) cachedValue;
						}
					}

					final ActionContext actionContext = new ActionContext(securityContext);

					// don't ignore predicate
					actionContext.setPredicate(predicate);

					Object result = Scripting.evaluate(actionContext, obj, "${".concat(readFunction).concat("}"), "getProperty(" + jsonName + ")");

					securityContext.getContextStore().storeFunctionPropertyResult(obj.getUuid(), jsonName, result);

					return (T)result;
				}

			} else {

				logger.warn("Unable to evaluate function property {}, object was null.", jsonName());
			}

		} catch (Throwable t) {

			t.printStackTrace();
			logger.warn("Exception while evaluating read function in Function property \"{}\"", jsonName());
		}

		return null;
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public SortType getSortType() {
		return SortType.Default;
	}

	@Override
	public Class valueType() {

		if (typeHint != null) {

			switch (typeHint.toLowerCase()) {

				case "boolean": return Boolean.class;
				case "string":  return String.class;
				case "int":     return Integer.class;
				case "long":    return Long.class;
				case "double":  return Double.class;
				case "date":    return Date.class;
			}
		}

		return Object.class;
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		return value;
	}

	@Override
	public String typeName() {
		return valueType().getSimpleName();
	}

	@Override
	public PropertyConverter<T, ?> databaseConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public PropertyConverter<T, ?> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return null;
	}

	@Override
	public PropertyConverter<?, T> inputConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public Object setProperty(final SecurityContext securityContext, final GraphObject target, final T value) throws FrameworkException {

		final ActionContext ctx = new ActionContext(securityContext);
		final GraphObject obj   = PropertyMap.unwrap(target);
		final String func       = getWriteFunction();
		T result                = null;

		if (func != null) {

			try {

				if (!securityContext.doInnerCallbacks()) {
					return null;
				}

				ctx.setConstant("value", value);

				result = (T)Scripting.evaluate(ctx, obj, "${".concat(func).concat("}"), "setProperty(" + jsonName + ")");

			} catch (FrameworkException fex) {

				// catch and re-throw FrameworkExceptions
				throw fex;

			} catch (Throwable t) {

				logger.warn("Exception while evaluating write function in Function property \"{}\": {}", jsonName(), t.getMessage());
			}
		}

		if (ctx.hasError()) {

			throw new FrameworkException(422, "Server-side scripting error", ctx.getErrorBuffer());
		}

		return result;
	}

	@Override
	public Property<T> format(final String format) {
		this.readFunction = format;
		return this;
	}

	@Override
	public T convertSearchValue(final SecurityContext securityContext, final String requestParameter) throws FrameworkException {

		if (typeHint != null) {

			PropertyConverter converter = null;

			switch (typeHint.toLowerCase()) {

				case "boolean": converter = pBoolean.inputConverter(securityContext); break;
				case "int":     converter = pInt.inputConverter(securityContext); break;
				case "long":    converter = pLong.inputConverter(securityContext); break;
				case "double":  converter = pDouble.inputConverter(securityContext); break;
				case "date":    converter = pDate.inputConverter(securityContext); break;
			}

			if (converter != null) {

				return (T)converter.convert(requestParameter);
			}
		}

		// fallback
		return super.convertSearchValue(securityContext, requestParameter);
	}

	// ----- private methods -----
	private String getReadFunction() throws FrameworkException {
		return getCachedSourceCode(sourceUuid, SchemaProperty.readFunction, this.readFunction);
	}

	private String getWriteFunction() throws FrameworkException {
		return getCachedSourceCode(sourceUuid, SchemaProperty.writeFunction, this.writeFunction);
	}

	public String getCachedSourceCode(final String uuid, final PropertyKey<String> key, final String defaultValue) throws FrameworkException {

		final SchemaProperty property = StructrApp.getInstance().get(SchemaProperty.class, uuid);
		if (property != null) {

			final String value = property.getProperty(key);
			if (value != null) {

				return value;
			}
		}

		return defaultValue;
	}
}
