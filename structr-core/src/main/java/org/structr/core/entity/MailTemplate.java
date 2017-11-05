/**
 * Copyright (C) 2010-2017 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.entity;

import org.structr.common.PropertyView;
import static org.structr.core.GraphObject.type;
import org.structr.core.graph.NodeInterface;
import static org.structr.core.graph.NodeInterface.name;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;

/**
 * Interface that represents a template with placeholders, to be used for sending e-mails.
 */
public interface MailTemplate extends NodeInterface {

	public static final Property<String>  text   = new StringProperty("text").cmis().indexed().notNull().compound();
	public static final Property<String>  locale = new StringProperty("locale").cmis().indexed().notNull().compound();

	public static final org.structr.common.View uiView = new org.structr.common.View(MailTemplate.class, PropertyView.Ui,
		type, name, text, locale
	);

	public static final org.structr.common.View publicView = new org.structr.common.View(MailTemplate.class, PropertyView.Public,
		type, name, text, locale
	);
}
