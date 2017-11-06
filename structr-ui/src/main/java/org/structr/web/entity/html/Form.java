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

import org.apache.commons.lang3.ArrayUtils;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.property.Property;
import org.structr.schema.SchemaService;
import org.structr.web.common.HtmlProperty;
import org.structr.web.entity.dom.DOMElement;

/**
 *
 */
public interface Form extends DOMElement {

	static class Impl { static { SchemaService.registerMixinType(Form.class); }}

	public static final Property<String> _acceptCharset = new HtmlProperty("accept-charset");
	public static final Property<String> _action        = new HtmlProperty("action");
	public static final Property<String> _autocomplete  = new HtmlProperty("autocomplete");
	public static final Property<String> _enctype       = new HtmlProperty("enctype");
	public static final Property<String> _method        = new HtmlProperty("method");
	public static final Property<String> _name          = new HtmlProperty("name");
	public static final Property<String> _novalidate    = new HtmlProperty("novalidate");
	public static final Property<String> _target        = new HtmlProperty("target");

	public static final View htmlView = new View(Form.class, PropertyView.Html,
	    _acceptCharset, _action, _autocomplete, _enctype, _method, _name, _novalidate, _target
	);

	@Override
	default Property[] getHtmlAttributes() {
		return (Property[]) ArrayUtils.addAll(DOMElement.super.getHtmlAttributes(), htmlView.properties());
	}
}
