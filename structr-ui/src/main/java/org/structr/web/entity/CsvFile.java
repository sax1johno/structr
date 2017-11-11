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
import org.structr.common.fulltext.FulltextIndexer;
import org.structr.core.Export;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.ModificationQueue;
import org.structr.web.common.FileHelper;

/**
 */
public interface CsvFile extends File {

	@Override
	default boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
		return File.super.onCreation(securityContext, errorBuffer);
	}

	@Override
	default boolean onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
		return File.super.onModification(securityContext, errorBuffer, modificationQueue);
	}

	@Override
	default void onNodeCreation() {
		File.super.onNodeCreation();
	}

	@Override
	default void onNodeDeletion() {
		File.super.onNodeDeletion();
	}

	@Override
	default void afterCreation(SecurityContext securityContext) {
		File.super.afterCreation(securityContext);
	}

	@Override
	default public boolean isValid(final ErrorBuffer errorBuffer) {
		return File.super.isValid(errorBuffer);
	}

	@Override
	default String getPath() {
		return FileHelper.getFolderPath(this);
	}

	@Override
	default void notifyUploadCompletion() {
		File.super.notifyUploadCompletion();
	}

	@Export
	@Override
	default GraphObject getSearchContext(final String searchTerm, final int contextLength) {

		final String text = getProperty(extractedContent);
		if (text != null) {

			final FulltextIndexer indexer = StructrApp.getInstance(getSecurityContext()).getFulltextIndexer();
			return indexer.getContextObject(searchTerm, text, contextLength);
		}

		return null;
	}
}
