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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.search.Occurrence;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.Query;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.search.ArraySearchAttribute;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchAttributeGroup;

/**
 * A property that stores and retrieves an array of Booleans.
 *
 *
 */
public class BooleanArrayProperty extends AbstractPrimitiveProperty<Boolean[]> {

	private static final Logger logger = LoggerFactory.getLogger(BooleanArrayProperty.class.getName());

	public BooleanArrayProperty(final String name) {

		super(name);
	}

	@Override
	public Object fixDatabaseProperty(Object value) {

		// We can only try to fix a String and convert it into a String[]
		if (value != null && value instanceof String) {

			String[] fixedValue = null;

			final String stringValue = (String)value;

			if (stringValue.contains(",")) {
				fixedValue = stringValue.split(",");
			}

			if (stringValue.contains(" ")) {
				fixedValue = stringValue.split(" ");
			}

			if (securityContext != null && entity != null) {

				try {
					setProperty(securityContext, entity, convert(Arrays.asList(fixedValue)));

				} catch (FrameworkException ex) {
					logger.warn("", ex);
				}
			}

			return fixedValue;

		}

		return value;
	}

	@Override
	public String typeName() {
		return "Boolean[]";
	}

	@Override
	public Class valueType() {
		return Boolean.class;
	}

	@Override
	public SortType getSortType() {
		return SortType.Default;
	}

	@Override
	public PropertyConverter<Boolean[], ?> databaseConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public PropertyConverter<Boolean[], ?> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		this.securityContext = securityContext;
		this.entity = entity;
		return null;
	}

	@Override
	public PropertyConverter<?, Boolean[]> inputConverter(SecurityContext securityContext) {
		return new ArrayInputConverter(securityContext);
	}

	private class ArrayInputConverter extends PropertyConverter<Object, Boolean[]> {

		public ArrayInputConverter(SecurityContext securityContext) {
			super(securityContext, null);
		}

		@Override
		public Object revert(Boolean[] source) throws FrameworkException {
			return source != null ? Arrays.asList(source) : null;
		}

		@Override
		public Boolean[] convert(Object source) throws FrameworkException {

			if (source == null) {
				return null;
			}

			if (source instanceof List) {
				return BooleanArrayProperty.this.convert((List)source);
			}

			if (source.getClass().isArray()) {
				return convert(Arrays.asList((Boolean[])source));
			}

			if (source instanceof String) {

				final String s = (String)source;
				if (s.contains(",")) {

					return BooleanArrayProperty.this.convert(Arrays.asList(s.split(",")));
				}

				// special handling of empty search attribute
				if (StringUtils.isBlank(s)) {
					return null;
				}

			}

			return (Boolean[])new Boolean[] { Boolean.valueOf(source.toString()) };
		}

	}

	@Override
	public SearchAttribute getSearchAttribute(SecurityContext securityContext, Occurrence occur, Boolean[] searchValue, boolean exactMatch, Query query) {

		// early exit, return empty search attribute
		if (searchValue == null) {
			return new ArraySearchAttribute(this, "", exactMatch ? occur : Occurrence.OPTIONAL, exactMatch);
		}

		final SearchAttributeGroup group = new SearchAttributeGroup(occur);

		for (Boolean value : searchValue) {

			group.add(new ArraySearchAttribute(this, value, exactMatch ? occur : Occurrence.OPTIONAL, exactMatch));
		}

		return group;
	}

	@Override
	public boolean isCollection() {
		return true;
	}

	// ----- private methods -----
	private Boolean[] convert(final List source) {

		final ArrayList<Boolean> result = new ArrayList<>();

		for (final Object o : source) {

			if (o instanceof Boolean) {

				result.add((Boolean)o);

			} else if (o != null) {

				result.add(Boolean.valueOf(o.toString()));

			} else {

				// dont know
				throw new IllegalStateException("Conversion of array type failed.");
			}
		}

		return (Boolean[])result.toArray(new Boolean[0]);
	}
}
