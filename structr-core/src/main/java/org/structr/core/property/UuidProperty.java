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

import org.structr.api.search.Occurrence;
import org.structr.common.SecurityContext;
import org.structr.core.app.Query;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.UuidSearchAttribute;

/**
 *
 *
 */
public class UuidProperty extends StringProperty {

	public UuidProperty() {

		super("id");

		indexed();
		systemInternal();
		readOnly();
		writeOnce();
		unique(true);
		notNull(true);

	}

	@Override
	public SearchAttribute getSearchAttribute(final SecurityContext securityContext, final Occurrence occur, final String searchValue, final boolean exactMatch, final Query query) {
		return new UuidSearchAttribute(searchValue, occur);
	}
}
