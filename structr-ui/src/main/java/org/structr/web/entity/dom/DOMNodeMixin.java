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
package org.structr.web.entity.dom;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.PropertyKey;

/**
 * Combines AbstractNode and org.w3c.dom.Node.
 */
public abstract class DOMNodeMixin extends AbstractNode implements DOMNode {

	private Map<String, Integer> cachedMostUsedTagNames                           = new LinkedHashMap<>();
	private Set<PropertyKey> customProperties                                     = null;
	private Page cachedOwnerDocument                                              = null;
	private String cachedPagePath                                                 = null;

	@Override
	public boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		if (super.onCreation(securityContext, errorBuffer)) {

			return checkName(errorBuffer);
		}

		return false;
	}

	@Override
	public boolean onModification(SecurityContext securityContext, ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		if (super.onModification(securityContext, errorBuffer, modificationQueue)) {

			if (customProperties != null) {

				// invalidate data property cache
				customProperties.clear();
			}


			try {

				increasePageVersion();

			} catch (FrameworkException ex) {

				logger.warn("Updating page version failed", ex);

			}

			return checkName(errorBuffer);
		}

		return false;
	}
}
