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
package org.structr.web.entity;

import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.PropertyMap;

/**
 * Trash folder
 */
public interface Trash extends Folder {

	@Override
	default boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
		return Folder.super.onCreation(securityContext, errorBuffer);
	}

	@Override
	default boolean onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
		return Folder.super.onModification(securityContext, errorBuffer, modificationQueue);
	}

	@Override
	default boolean onDeletion(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {
		return Folder.super.onDeletion(securityContext, errorBuffer, properties);
	}

	@Override
	default boolean isValid(final ErrorBuffer errorBuffer) {
		return Folder.super.isValid(errorBuffer);
	}
}
