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

import java.io.IOException;
import org.structr.api.config.Settings;
import org.structr.cmis.CMISInfo;
import org.structr.cmis.info.CMISDocumentInfo;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.fulltext.Indexable;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Favoritable;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.PropertyMap;
import org.structr.schema.SchemaService;
import org.structr.schema.action.JavaScriptSource;
import org.structr.web.common.FileHelper;

/**
 *
 *
 */
public class FileMixin extends AbstractNode implements File, Indexable, Linkable, JavaScriptSource, CMISInfo, CMISDocumentInfo, Favoritable {

	static {

		SchemaService.registerMixinType("File", AbstractNode.class, File.class);
	}

	@Override
	public boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		if (super.onCreation(securityContext, errorBuffer)) {

			final PropertyMap changedProperties = new PropertyMap();

			if (Settings.FilesystemEnabled.getValue() && !getProperty(AbstractFile.hasParent)) {

				final Folder workingOrHomeDir = getCurrentWorkingDir();
				if (workingOrHomeDir != null && getProperty(AbstractFile.parent) == null) {

					changedProperties.put(AbstractFile.parent, workingOrHomeDir);
				}
			}

			changedProperties.put(hasParent, getProperty(parentId) != null);

			setProperties(securityContext, changedProperties);

			return true;
		}

		return false;
	}

	@Override
	public boolean onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		if (super.onModification(securityContext, errorBuffer, modificationQueue)) {

			synchronized (this) {

				// save current security context
				final SecurityContext previousSecurityContext = securityContext;

				// replace with SU context
				this.securityContext = SecurityContext.getSuperUserInstance();

				// update metadata and parent as superuser
				FileHelper.updateMetadata(this, new PropertyMap(hasParent, getProperty(parentId) != null));

				// restore previous security context
				this.securityContext = previousSecurityContext;
			}

			triggerMinificationIfNeeded(modificationQueue);

			return true;
		}

		return false;
	}

	@Override
	public void onNodeCreation() {

		final String uuid     = getUuid();
		final String filePath = File.getDirectoryPath(uuid) + "/" + uuid;

		try {
			unlockSystemPropertiesOnce();
			setProperties(securityContext, new PropertyMap(relativeFilePath, filePath));

		} catch (Throwable t) {

			logger.warn("Exception while trying to set relative file path {}: {}", new Object[]{filePath, t});

		}
	}

	@Override
	public void onNodeDeletion() {

		String filePath = null;
		try {
			final String path = getRelativeFilePath();

			if (path != null) {

				filePath = FileHelper.getFilePath(path);

				java.io.File toDelete = new java.io.File(filePath);

				if (toDelete.exists() && toDelete.isFile()) {

					toDelete.delete();
				}
			}

		} catch (Throwable t) {

			logger.debug("Exception while trying to delete file {}: {}", new Object[]{filePath, t});

		}

	}

	@Override
	public void afterCreation(SecurityContext securityContext) {

		try {

			final String filesPath        = Settings.FilesPath.getValue();
			final java.io.File fileOnDisk = new java.io.File(filesPath + "/" + getRelativeFilePath());

			if (fileOnDisk.exists()) {
				return;
			}

			fileOnDisk.getParentFile().mkdirs();

			try {

				fileOnDisk.createNewFile();

			} catch (IOException ex) {

				logger.error("Could not create file", ex);
				return;
			}

			FileHelper.updateMetadata(this, new PropertyMap(version, 0));

		} catch (FrameworkException ex) {

			logger.error("Could not create file", ex);
		}

	}

}
