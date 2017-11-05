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

import java.util.List;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.schema.SchemaService;
import org.structr.web.common.ImageHelper;

/**
 * An image whose binary data will be stored on disk.
 */
public class ImageMixin extends FileMixin implements Image {

	static {

		SchemaService.registerMixinType("Image", AbstractNode.class, Image.class);
	}

	@Override
	public Object setProperty(final PropertyKey key, final Object value) throws FrameworkException {

		// Copy visibility properties and owner to all thumbnails
		if (visibleToPublicUsers.equals(key) ||
			visibleToAuthenticatedUsers.equals(key) ||
			visibilityStartDate.equals(key) ||
			visibilityEndDate.equals(key) ||
			owner.equals(key)) {

			for (Image tn : getThumbnails()) {

				if (!tn.getUuid().equals(getUuid())) {
					tn.setProperty(key, value);
				} else {
//					logger.info("Ignoring recursive setProperty for thumbnail where image is its own thumbnail");
				}

			}

		}

		return super.setProperty(key, value);
	}

	@Override
	public void setProperties(final SecurityContext securityContext, final PropertyMap properties) throws FrameworkException {

		if ( !isThumbnail() ) {

			final PropertyMap propertiesCopiedToAllThumbnails = new PropertyMap();

			for (final PropertyKey key : properties.keySet()) {

					if (visibleToPublicUsers.equals(key) ||
						visibleToAuthenticatedUsers.equals(key) ||
						visibilityStartDate.equals(key) ||
						visibilityEndDate.equals(key) ||
						owner.equals(key)) {

						propertiesCopiedToAllThumbnails.put(key, properties.get(key));
					}
			}

			if ( !propertiesCopiedToAllThumbnails.isEmpty() ) {

				final List<Image> thumbnails = getThumbnails();

				for (Image tn : thumbnails) {

					if (!tn.getUuid().equals(getUuid())) {
						tn.setProperties(tn.getSecurityContext(), propertiesCopiedToAllThumbnails);
					} else {
//						logger.info("Ignoring recursive setProperty for thumbnail where image is its own thumbnail");
					}

				}

			}

		}

		super.setProperties(securityContext, properties);
	}

	@Override
	public boolean onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		if (super.onModification(securityContext, errorBuffer, modificationQueue)) {

			if ( !isThumbnail() ) {

				if (modificationQueue.isPropertyModified(this, name)) {

					final String newImageName = getName();

					for (Image tn : getThumbnails()) {

						final String expectedThumbnailName = ImageHelper.getThumbnailName(newImageName, tn.getWidth(), tn.getHeight());
						final String currentThumbnailName  = tn.getName();

						if ( !expectedThumbnailName.equals(currentThumbnailName) ) {

							logger.debug("Auto-renaming Thumbnail({}) from '{}' to '{}'", tn.getUuid(), currentThumbnailName, expectedThumbnailName);
							tn.setProperty(AbstractNode.name, expectedThumbnailName);

						}

					}

				}

			}

			return true;
		}

		return false;
	}
}
