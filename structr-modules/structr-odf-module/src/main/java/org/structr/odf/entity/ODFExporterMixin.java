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

import java.net.URI;
import org.odftoolkit.odfdom.doc.OdfDocument;
import org.odftoolkit.odfdom.pkg.OdfPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.StringProperty;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Image;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Base class for ODF exporter
 */
public abstract class ODFExporterMixin extends AbstractNode implements ODFExporter {

	protected static final Logger logger = LoggerFactory.getLogger(ODTExporter.class.getName());

	@Export
	@Override
	public void createDocumentFromTemplate() throws FrameworkException {

		OdfDocument templateOdt;
		final File template = getProperty(documentTemplate);
		File output = getProperty(resultDocument);

		try {

			// If no result file is given, create one and set it as result document
			if (output == null) {

				output = FileHelper.createFile(securityContext, new byte[]{}, template.getContentType(), File.class, getName().concat("_").concat(template.getName()));

				output.setProperty(File.parent, template.getProperty(File.parent));

				output.unlockSystemPropertiesOnce();
				output.setProperty(AbstractNode.type, File.class.getSimpleName());

				setProperty(resultDocument, output);

			}

			templateOdt = OdfDocument.loadDocument(template.getFileOnDisk().getAbsolutePath());
			templateOdt.save(output.getOutputStream());
			templateOdt.close();

		} catch (Exception e) {

			logger.error("Error while creating ODS from template", e);

		}
	}

	@Export
	@Override
	public void exportImage(String uuid) {

		File output = getProperty(resultDocument);

		try {

			final App app = StructrApp.getInstance();
			final Image result = app.nodeQuery(Image.class).and(GraphObject.id, uuid).getFirst();

			String imageName = result.getProperty(new StringProperty("name"));
			String contentType = result.getProperty(new StringProperty("contentType"));

			String templateImagePath = null;

			OdfDocument doc = OdfDocument.loadDocument(output.getFileOnDisk().getAbsolutePath());

			NodeList nodes = doc.getContentRoot().getElementsByTagName(ODF_IMAGE_PARENT_NAME);
			for (int i = 0; i < nodes.getLength(); i++) {

				Node currentNode = nodes.item(i);
				NamedNodeMap attrs = currentNode.getAttributes();
				Node fieldName = attrs.getNamedItem(ODF_IMAGE_ATTRIBUTE_PARENT_IMAGE_NAME);
				if (fieldName != null && fieldName.getTextContent().equals(imageName)) {
					NamedNodeMap childAttrs = currentNode.getFirstChild().getAttributes();
					Node filePath = childAttrs.getNamedItem(ODF_IMAGE_ATTRIBUTE_FILE_PATH);
					templateImagePath = filePath.getTextContent();
					filePath.setTextContent(ODF_IMAGE_DIRECTORY + imageName);
				}

			}

			OdfPackage pkg = doc.getPackage();
			if (templateImagePath != null && templateImagePath.length() > 0) {

				pkg.remove(templateImagePath);

			}
			pkg.insert(new URI(result.getFileOnDisk().getAbsolutePath()), ODF_IMAGE_DIRECTORY + imageName, contentType);
			pkg.save(output.getFileOnDisk().getAbsolutePath());
			pkg.close();
			doc.close();

		} catch (Exception e) {

			logger.error("Error while exporting image to document", e);

		}
	}

}
