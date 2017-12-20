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
package org.structr.web.entity.html;

import java.net.URI;
import org.structr.common.PropertyView;
import org.structr.schema.SchemaService;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;
import org.structr.web.entity.LinkSource;
import org.structr.web.entity.dom.DOMElement;

public interface Img extends LinkSource {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("Img");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Img"));
		type.setExtends(URI.create("#/definitions/LinkSource"));

		type.addStringProperty("_html_alt",         PropertyView.Html);
		type.addStringProperty("_html_src",         PropertyView.Html);
		type.addStringProperty("_html_crossorigin", PropertyView.Html);
		type.addStringProperty("_html_usemap",      PropertyView.Html);
		type.addStringProperty("_html_ismap",       PropertyView.Html);
		type.addStringProperty("_html_width",       PropertyView.Html);
		type.addStringProperty("_html_height",      PropertyView.Html);

		type.overrideMethod("getHtmlAttributes", false, DOMElement.GET_HTML_ATTRIBUTES_CALL);
		type.overrideMethod("avoidWhitespace",   false, "return true;");
		type.overrideMethod("isVoidElement",     false, "return true;");
	}}
}
