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
package org.structr.odf.entity;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import static org.structr.core.GraphObject.createdBy;
import static org.structr.core.GraphObject.createdDate;
import static org.structr.core.GraphObject.id;
import static org.structr.core.GraphObject.lastModifiedDate;
import static org.structr.core.GraphObject.type;
import static org.structr.core.GraphObject.visibilityEndDate;
import static org.structr.core.GraphObject.visibilityStartDate;
import static org.structr.core.GraphObject.visibleToAuthenticatedUsers;
import static org.structr.core.GraphObject.visibleToPublicUsers;
import org.structr.core.graph.NodeInterface;
import static org.structr.core.graph.NodeInterface.deleted;
import static org.structr.core.graph.NodeInterface.hidden;
import static org.structr.core.graph.NodeInterface.name;
import static org.structr.core.graph.NodeInterface.owner;
import org.structr.core.property.EndNode;
import org.structr.core.property.Property;
import org.structr.odf.relations.DocumentResult;
import org.structr.odf.relations.DocumentTemplate;
import org.structr.odf.relations.TransformationRules;
import org.structr.transform.VirtualType;
import org.structr.web.entity.File;

/**
 * Base class for ODF exporter
 */
public interface ODFExporter extends NodeInterface {

	//General ODF specific constants and field specifiers
	//Images

	public final String ODF_IMAGE_PARENT_NAME                 = "draw:frame";
	public final String ODF_IMAGE_ATTRIBUTE_PARENT_IMAGE_NAME = "draw:name";
	public final String ODF_IMAGE_ATTRIBUTE_FILE_PATH         = "xlink:href";
	public final String ODF_IMAGE_DIRECTORY                   = "Pictures/";

	public static final Property<VirtualType> transformationProvider = new EndNode("transformationProvider", TransformationRules.class);
	public static final Property<File> documentTemplate              = new EndNode("documentTemplate", DocumentTemplate.class);
	public static final Property<File> resultDocument                = new EndNode("resultDocument", DocumentResult.class);

	public static final View defaultView = new View(ODFExporter.class, PropertyView.Public,
		id, type, transformationProvider, documentTemplate, resultDocument
	);

	public static final View uiView = new View(ODFExporter.class, PropertyView.Ui,
		id, name, owner, type, createdBy, deleted, hidden, createdDate, lastModifiedDate,
		visibleToPublicUsers, visibleToAuthenticatedUsers, visibilityStartDate, visibilityEndDate,
		transformationProvider, documentTemplate, resultDocument
	);

	void createDocumentFromTemplate() throws FrameworkException;
	void exportImage(String uuid);
}
