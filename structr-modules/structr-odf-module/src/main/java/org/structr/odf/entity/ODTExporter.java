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

import org.structr.common.error.FrameworkException;

/**
 * Reads a nodes attributes and tries to replace matching attributes in the
 * given ODT-File template.
 */
public interface ODTExporter extends ODFExporter {

	public final String ODT_FIELD_TAG_NAME        = "text:user-field-decl";
	public final String ODT_FIELD_ATTRIBUTE_NAME  = "text:name";
	public final String ODT_FIELD_ATTRIBUTE_VALUE = "office:string-value";

	void exportAttributes(String uuid) throws FrameworkException;
}
