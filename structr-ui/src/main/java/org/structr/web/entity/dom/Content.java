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

import org.apache.commons.lang3.StringUtils;
import org.structr.common.Permission;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.Adapter;
import org.structr.core.entity.Favoritable;
import org.structr.core.property.ConstantBooleanProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.core.script.Scripting;
import org.structr.schema.NonIndexed;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;
import org.structr.web.common.RenderContext.EditMode;
import org.structr.web.entity.html.Textarea;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * Represents a content node. This class implements the org.w3c.dom.Text interface.
 * All methods in the W3C Text interface are based on the raw database content.
 */
public interface Content extends DOMNode, Text, NonIndexed, Favoritable {

	public static final Property<String> contentType                                     = new StringProperty("contentType").indexed();
	public static final Property<String> content                                         = new StringProperty("content").indexed();
	public static final Property<Boolean> isContent                                      = new ConstantBooleanProperty("isContent", true);

	public static final org.structr.common.View uiView                                   = new org.structr.common.View(Content.class, PropertyView.Ui,
		content, contentType, parent, pageId, syncedNodes, sharedComponent, sharedComponentConfiguration, dataKey, restQuery, cypherQuery, xpathQuery, functionQuery,
		hideOnDetail, hideOnIndex, showForLocales, hideForLocales, showConditions, hideConditions, isContent, isDOMNode, isFavoritable
	);

	public static final org.structr.common.View publicView                               = new org.structr.common.View(Content.class, PropertyView.Public,
		content, contentType, parent, pageId, syncedNodes, sharedComponent, sharedComponentConfiguration, dataKey, restQuery, cypherQuery, xpathQuery, functionQuery,
		hideOnDetail, hideOnIndex, showForLocales, hideForLocales, showConditions, hideConditions, isContent, isDOMNode, isFavoritable
	);

	Adapter<String, String> getContentConverter(final String contentType);

	@Override
	default boolean contentEquals(DOMNode otherNode) {

		if (otherNode instanceof Content) {

			final String content1 = getTextContent();
			final String content2 = ((Content) otherNode).getTextContent();

			if (content1 == null && content2 == null) {
				return true;
			}

			if (content1 != null && content2 != null) {

				return content1.equals(content2);
			}
		}

		return false;
	}

	@Override
	default String getContextName() {
		return "#text";
	}

	@Override
	default void updateFromNode(final DOMNode newNode) throws FrameworkException {

		if (newNode instanceof Content) {

			setProperties(getSecurityContext(), new PropertyMap(content, newNode.getProperty(Content.content)));

		}

	}

	@Override
	default String getIdHash() {

		final DOMNode _parent = getProperty(DOMNode.parent);
		if (_parent != null) {

			String dataHash = _parent.getProperty(DOMNode.dataHashProperty);
			if (dataHash == null) {
				dataHash = _parent.getIdHash();
			}

			return dataHash + "Content" + treeGetChildPosition(this);
		}

		return DOMNode.super.getIdHash();
	}

	@Override
	default void renderContent(final RenderContext renderContext, final int depth) throws FrameworkException {

		try {
			final EditMode edit = renderContext.getEditMode(getSecurityContext().getUser(false));
			if (EditMode.DEPLOYMENT.equals(edit)) {

				final AsyncBuffer buf = renderContext.getBuffer();

				// output ownership comments
				renderDeploymentExportComments(buf, true);

				// EditMode "deployment" means "output raw content, do not interpret in any way
				buf.append(DOMNode.escapeForHtml(getProperty(Content.content)));

				return;
			}

			if (isDeleted() || isHidden() || !displayForLocale(renderContext) || !displayForConditions(renderContext)) {
				return;
			}

			final String id            = getUuid();
			final boolean inBody       = renderContext.inBody();
			final AsyncBuffer out      = renderContext.getBuffer();

			String _contentType = getProperty(contentType);

			// apply configuration for shared component if present
			final String _sharedComponentConfiguration = getProperty(sharedComponentConfiguration);
			if (StringUtils.isNotBlank(_sharedComponentConfiguration)) {

				Scripting.evaluate(renderContext, this, "${" + _sharedComponentConfiguration + "}", "shared component configuration");
			}

			// fetch content with variable replacement
			String _content = getPropertyWithVariableReplacement(renderContext, Content.content);

			if (!(EditMode.RAW.equals(edit) || EditMode.WIDGET.equals(edit)) && (_contentType == null || ("text/plain".equals(_contentType)))) {

				_content = DOMNode.escapeForHtml(_content);

			}

			if (EditMode.CONTENT.equals(edit) && inBody && this.isGranted(Permission.write, getSecurityContext())) {

				if ("text/javascript".equals(_contentType)) {

					// Javascript will only be given some local vars
					out.append("// data-structr-type='").append(getType()).append("'\n// data-structr-id='").append(id).append("'\n");

				} else if ("text/css".equals(_contentType)) {

					// CSS will only be given some local vars
					out.append("/* data-structr-type='").append(getType()).append("'*/\n/* data-structr-id='").append(id).append("'*/\n");

				} else {

					// In edit mode, add an artificial comment tag around content nodes within body to make them editable
					final String cleanedContent = StringUtils.remove(StringUtils.remove(org.apache.commons.lang3.StringUtils.replace(getProperty(Content.content), "\n", "\\\\n"), "<!--"), "-->");
					out.append("<!--data-structr-id=\"".concat(id)
						.concat("\" data-structr-raw-value=\"").concat(DOMNode.escapeForHtmlAttributes(cleanedContent)).concat("\"-->"));

				}

			}

			// examine content type and apply converter
			if (_contentType != null) {

				final Adapter<String, String> converter = getContentConverter(_contentType);
				if (converter != null) {

					try {

						// apply adapter
						_content = converter.adapt(_content);
					} catch (FrameworkException fex) {

						logger.warn("Unable to convert content: {}", fex.getMessage());

					}

				}

			}

			// replace newlines with <br /> for rendering
			if (((_contentType == null) || _contentType.equals("text/plain")) && (_content != null) && !_content.isEmpty()) {

				final DOMNode _parent = getProperty(Content.parent);
				if (_parent == null || !(_parent instanceof Textarea)) {

					_content = _content.replaceAll("[\\n]{1}", "<br>");
				}
			}

			if (_content != null) {

				// insert whitespace to make element clickable
				if (EditMode.CONTENT.equals(edit) && _content.length() == 0) {
					_content = "--- empty ---";
				}

				out.append(_content);
			}

			if (EditMode.CONTENT.equals(edit) && inBody && !("text/javascript".equals(getProperty(contentType))) && !("text/css".equals(getProperty(contentType)))) {

				out.append("<!---->");
			}

		} catch (Throwable t) {

			// catch exception to prevent ugly status 500 error pages in frontend.
			logger.error("", t);

		}

	}

	@Override
	default boolean isSynced() {
		return false;
	}

	// ----- interface org.w3c.dom.Text -----

	@Override
	default Text splitText(int offset) throws DOMException {

		checkWriteAccess();

		String text = getProperty(content);

		if (text != null) {

			int len = text.length();

			if (offset < 0 || offset > len) {

				throw new DOMException(DOMException.INDEX_SIZE_ERR, INDEX_SIZE_ERR_MESSAGE);

			} else {

				final String firstPart  = text.substring(0, offset);
				final String secondPart = text.substring(offset);

				final Document document  = getOwnerDocument();
				final Node parent        = getParentNode();

				if (document != null && parent != null) {

					try {

						// first part goes into existing text element
						setProperties(getSecurityContext(), new PropertyMap(content, firstPart));

						// second part goes into new text element
						Text newNode = document.createTextNode(secondPart);

						// make new node a child of old parent
						parent.appendChild(newNode);


						return newNode;

					} catch (FrameworkException fex) {

						throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());

					}

				} else {

					throw new DOMException(DOMException.INVALID_STATE_ERR, CANNOT_SPLIT_TEXT_WITHOUT_PARENT);
				}
			}
		}

		throw new DOMException(DOMException.INDEX_SIZE_ERR, INDEX_SIZE_ERR_MESSAGE);
	}

	@Override
	default boolean isElementContentWhitespace() {

		checkReadAccess();

		String text = getProperty(content);

		if (text != null) {

			return !text.matches("[\\S]*");
		}

		return false;
	}

	@Override
	default String getWholeText() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	default Text replaceWholeText(String string) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	default String getData() throws DOMException {

		checkReadAccess();

		return getProperty(content);
	}

	@Override
	default void setData(final String data) throws DOMException {

		checkWriteAccess();
		try {
			setProperties(getSecurityContext(), new PropertyMap(content, data));

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());

		}
	}

	@Override
	default int getLength() {

		String text = getProperty(content);

		if (text != null) {

			return text.length();
		}

		return 0;
	}

	@Override
	default String substringData(int offset, int count) throws DOMException {

		checkReadAccess();

		String text = getProperty(content);

		if (text != null) {

			try {

				return text.substring(offset, offset + count);

			} catch (IndexOutOfBoundsException iobex) {

				throw new DOMException(DOMException.INDEX_SIZE_ERR, INDEX_SIZE_ERR_MESSAGE);
			}
		}

		return "";
	}

	@Override
	default void appendData(final String data) throws DOMException {

		checkWriteAccess();

		try {
			String text = getProperty(content);
			setProperties(getSecurityContext(), new PropertyMap(content, text.concat(data)));

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());

		}
	}

	@Override
	default void insertData(final int offset, final String data) throws DOMException {

		checkWriteAccess();

		try {

			String text = getProperty(content);

			String leftPart  = text.substring(0, offset);
			String rightPart = text.substring(offset);

			StringBuilder buf = new StringBuilder(text.length() + data.length() + 1);
			buf.append(leftPart);
			buf.append(data);
			buf.append(rightPart);

			// finally, set content to concatenated left, data and right parts
			setProperties(getSecurityContext(), new PropertyMap(content, buf.toString()));


		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());

		}
	}

	@Override
	default void deleteData(final int offset, final int count) throws DOMException {

		checkWriteAccess();

		// finally, set content to concatenated left and right parts
		try {

			String text = getProperty(content);

			String leftPart  = text.substring(0, offset);
			String rightPart = text.substring(offset + count);

			setProperties(getSecurityContext(), new PropertyMap(content, leftPart.concat(rightPart)));

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());

		}
	}

	@Override
	default void replaceData(final int offset, final int count, final String data) throws DOMException {

		checkWriteAccess();

		// finally, set content to concatenated left and right parts
		try {

			String text = getProperty(content);

			String leftPart  = text.substring(0, offset);
			String rightPart = text.substring(offset + count);

			StringBuilder buf = new StringBuilder(leftPart.length() + data.length() + rightPart.length());
			buf.append(leftPart);
			buf.append(data);
			buf.append(rightPart);

			setProperties(getSecurityContext(), new PropertyMap(content, buf.toString()));

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());

		}
	}

	// ----- interface org.w3c.dom.Node -----
	@Override
	default String getTextContent() throws DOMException {
		return getData();
	}

	@Override
	default void setTextContent(String textContent) throws DOMException {
		setData(textContent);
	}

	@Override
	default String getLocalName() {
		return null;
	}

	@Override
	default short getNodeType() {
		return TEXT_NODE;
	}

	@Override
	default String getNodeName() {
		return "#text";
	}

	@Override
	default String getNodeValue() throws DOMException {
		return getData();
	}

	@Override
	default void setNodeValue(String data) throws DOMException {
		setData(data);
	}

	@Override
	default NamedNodeMap getAttributes() {
		return null;
	}

	@Override
	default boolean hasAttributes() {
		return false;
	}

	// ----- interface DOMImportable -----
	@Override
	default Node doImport(Page newPage) throws DOMException {

		// for #text elements, importing is basically a clone operation
		return newPage.createTextNode(getData());
	}

	// ----- interface Favoritable -----
	@Override
	default String getContext() {
		return getPagePath();
	}

	@Override
	default String getFavoriteContent() {
		return getProperty(Content.content);
	}

	@Override
	default String getFavoriteContentType() {
		return getProperty(Content.contentType);
	}

	@Override
	default void setFavoriteContent(String content) throws FrameworkException {
		setProperty(Content.content, content);
	}
}
