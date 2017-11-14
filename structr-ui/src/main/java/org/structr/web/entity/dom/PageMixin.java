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

import org.structr.web.entity.html.Html5DocumentType;
import org.structr.core.entity.AbstractNode;
import org.structr.schema.SchemaService;
import org.w3c.dom.Node;

/**
 * Mixin code for Page interface, to be loaded at runtime.
 */
public class PageMixin extends AbstractNode implements Page {

	// register this type as an overridden builtin type
	static {
		SchemaService.registerMixinType(Page.class);
	}

	// ----- BEGIN Structr Mixin -----
	private Html5DocumentType docTypeNode = null;

	@Override
	public Object getFeature(final String version, final String feature) {
		return null;
	}

	@Override
	public Node getFirstChild() {

		if (docTypeNode == null) {
			docTypeNode = new Html5DocumentType(this);
		}

		return docTypeNode;
	}
	// ----- END Structr Mixin -----
}
