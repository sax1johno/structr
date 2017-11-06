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

import org.structr.common.error.FrameworkException;
import org.structr.schema.NonIndexed;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.Renderable;
import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 */
public interface DocumentFragment extends DOMNode, org.w3c.dom.DocumentFragment, NonIndexed {

	@Override
	default String getContextName() {
		return "DocumentFragment";
	}

	// ----- interface org.w3c.dom.Node -----
	@Override
	default String getLocalName() {
		return null;
	}

	@Override
	default String getNodeName() {
		return "#document-fragment";
	}

	@Override
	default String getNodeValue() throws DOMException {
		return null;
	}

	@Override
	default void setNodeValue(final String string) throws DOMException {
	}

	@Override
	default short getNodeType() {
		return DOCUMENT_FRAGMENT_NODE;
	}

	@Override
	default NamedNodeMap getAttributes() {
		return null;
	}

	@Override
	default boolean hasAttributes() {
		return false;
	}

	@Override
	default boolean contentEquals(final DOMNode otherNode) {
		return false;
	}

	@Override
	default void updateFromNode(final DOMNode newNode) throws FrameworkException {
		// do nothing
	}

	@Override
	default boolean isSynced() {
		return false;
	}

	// ----- interface Renderable -----
	@Override
	default void render(final RenderContext renderContext, final int depth) throws FrameworkException {

		NodeList _children = getChildNodes();
		int len            = _children.getLength();

		for (int i=0; i<len; i++) {

			Node child = _children.item(i);

			if (child != null && child instanceof Renderable) {

				((Renderable)child).render(renderContext, depth);
			}
		}

	}

	@Override
	default void renderContent(final RenderContext renderContext, final int depth) throws FrameworkException {
	}

	@Override
	default Node doAdopt(final Page newPage) throws DOMException {

		// do nothing, only children of DocumentFragments are
		// adopted
		return null;
	}

	@Override
	default Node doImport(final Page newPage) throws DOMException {
		// simply return an empty DocumentFragment, as the importing
		// will be done by the Page method if deep importing is enabled.
		return newPage.createDocumentFragment();
	}
}
