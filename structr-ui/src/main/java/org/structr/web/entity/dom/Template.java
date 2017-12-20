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

import java.net.URI;
import java.util.List;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.RelationshipInterface;
import org.structr.schema.SchemaService;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;
import org.structr.web.common.RenderContext.EditMode;

public interface Template extends Content {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("Template");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Template"));
		type.setExtends(URI.create("#/definitions/Content"));

		type.addStringProperty("contentType", PropertyView.Public).setIndexed(true);
		type.addStringProperty("content",     PropertyView.Public).setIndexed(true);

		type.overrideMethod("renderContent", false, Template.class.getName() + ".renderContent(this, arg0, arg1);");

	}}

	/*
	public static final org.structr.common.View uiView                                   = new org.structr.common.View(Content.class, PropertyView.Ui,
		children, childrenIds, content, contentType, parent, pageId, hideOnDetail, hideOnIndex, sharedComponent, syncedNodes, dataKey, restQuery, cypherQuery, xpathQuery, functionQuery,
		showForLocales, hideForLocales, showConditions, hideConditions, isContent
	);

	public static final org.structr.common.View publicView                               = new org.structr.common.View(Content.class, PropertyView.Public,
		children, childrenIds, content, contentType, parent, pageId, hideOnDetail, hideOnIndex, sharedComponent, syncedNodes, dataKey, restQuery, cypherQuery, xpathQuery, functionQuery,
		showForLocales, hideForLocales, showConditions, hideConditions, isContent
	);
	*/

	public static void renderContent(final Template thisTemplate, final RenderContext renderContext, final int depth) throws FrameworkException {

		final SecurityContext securityContext = thisTemplate.getSecurityContext();
		final EditMode editMode               = renderContext.getEditMode(securityContext.getUser(false));

		if (EditMode.DEPLOYMENT.equals(editMode)) {

			final DOMNode _syncedNode = thisTemplate.getSharedComponent();
			final AsyncBuffer out     = renderContext.getBuffer();

			if (depth > 0) {
				out.append(DOMNode.indent(depth, renderContext));
			}

			DOMNode.renderDeploymentExportComments(thisTemplate, out, true);

			out.append("<structr:template src=\"");

			if (_syncedNode != null) {

				// use name of synced node
				final String _name = _syncedNode.getProperty(AbstractNode.name);
				out.append(_name != null ? _name : _syncedNode.getUuid());

			} else {

				// use name of local template
				final String _name = thisTemplate.getProperty(AbstractNode.name);
				out.append(_name != null ? _name : thisTemplate.getUuid());
			}

			out.append("\"");

			DOMNode.renderSharedComponentConfiguration(thisTemplate, out, editMode);
			DOMNode.renderCustomAttributes(thisTemplate, out, securityContext, renderContext); // include custom attributes in templates as well!

			out.append(">");

			// fetch children
			final List<RelationshipInterface> rels = thisTemplate.getChildRelationships();
			if (rels.isEmpty()) {

				// No child relationships, maybe this node is in sync with another node
				if (_syncedNode != null) {
					rels.addAll(_syncedNode.getChildRelationships());
				}
			}

			for (final RelationshipInterface rel : rels) {

				final DOMNode subNode = (DOMNode) rel.getTargetNode();
				subNode.render(renderContext, depth + 1);
			}

			out.append(DOMNode.indent(depth, renderContext));
			out.append("</structr:template>");
			out.append(DOMNode.indent(depth-1, renderContext));

		} else {

			// "super" call using static method..
			Content.renderContent(thisTemplate, renderContext, depth);
		}
	}
}
