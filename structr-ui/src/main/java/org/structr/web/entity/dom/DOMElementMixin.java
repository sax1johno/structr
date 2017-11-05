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

import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.ModificationQueue;
import static org.structr.core.graph.NodeInterface.name;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyMap;
import org.structr.web.entity.relation.Sync;

/**
 */
public class DOMElementMixin extends AbstractNode implements DOMElement {

	@Override
	public boolean onModification(SecurityContext securityContext, ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		if (super.onModification(securityContext, errorBuffer, modificationQueue)) {

			final PropertyMap map = new PropertyMap();

			for (Sync rel : getOutgoingRelationships(Sync.class)) {

				final DOMElement syncedNode = (DOMElement) rel.getTargetNode();

				map.clear();

				// sync HTML properties only
				for (Property htmlProp : syncedNode.getHtmlAttributes()) {
					map.put(htmlProp, getProperty(htmlProp));
				}

				map.put(name, getProperty(name));

				syncedNode.setProperties(securityContext, map);
			}

			final Sync rel = getIncomingRelationship(Sync.class);
			if (rel != null) {

				final DOMElement otherNode = (DOMElement) rel.getSourceNode();
				if (otherNode != null) {

					map.clear();

					// sync both ways
					for (Property htmlProp : otherNode.getHtmlAttributes()) {
						map.put(htmlProp, getProperty(htmlProp));
					}

					map.put(name, getProperty(name));

					otherNode.setProperties(securityContext, map);
				}
			}

			return true;
		}

		return false;
	}
}
