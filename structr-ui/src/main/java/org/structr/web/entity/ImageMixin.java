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
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.schema.SchemaService;

/**
 * An image whose binary data will be stored on disk.
 */
public class ImageMixin extends AbstractNode implements Image {

	static {

		SchemaService.registerMixinType("Image", "org.structr.dynamic.File", Image.class);
	}

	// ----- BEGIN Structr Mixin -----
	@Override
	public java.lang.Object setProperty(final PropertyKey key, final java.lang.Object value) throws FrameworkException {

		// Copy visibility properties and owner to all thumbnails
		if (visibleToPublicUsers.equals(key) ||
			visibleToAuthenticatedUsers.equals(key) ||
			visibilityStartDate.equals(key) ||
			visibilityEndDate.equals(key) ||
			owner.equals(key)) {

			for (org.structr.web.entity.Image tn : getThumbnails()) {

				if (!tn.getUuid().equals(getUuid())) {

					tn.setProperty(key, value);
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

				final List<org.structr.web.entity.Image> thumbnails = getThumbnails();

				for (org.structr.web.entity.Image tn : thumbnails) {

					if (!tn.getUuid().equals(getUuid())) {

						tn.setProperties(tn.getSecurityContext(), propertiesCopiedToAllThumbnails);
					}
				}
			}
		}

		super.setProperties(securityContext, properties);
	}
	// ----- END Structr Mixin -----
}
