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
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.core.entity.relationship.NodeHasLocation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.DoubleProperty;
import org.structr.core.property.Property;
import org.structr.schema.SchemaService;

/**
 * The Location entity.
 */
public interface Location extends NodeInterface {

	static class Impl { static { SchemaService.registerMixinType(Location.class); }}

	public static final Property<Double> latitude  = new DoubleProperty("latitude").cmis().passivelyIndexed();	// these need to be indexed at the end
	public static final Property<Double> longitude = new DoubleProperty("longitude").cmis().passivelyIndexed();	// of the transaction so the spatial
	public static final Property<Double> altitude  = new DoubleProperty("altitude").cmis().passivelyIndexed();	// indexer sees all properties at once

	public static final View publicView = new View(Location.class, PropertyView.Public,
		latitude, longitude, altitude
	);

	public static final View uiView = new View(Location.class, PropertyView.Ui,
		latitude, longitude, altitude
	);

	@Override
	default void afterCreation(SecurityContext securityContext) {
		NodeInterface.super.afterCreation(securityContext);
		notifyLocatables();
	}

	@Override
	default void afterModification(SecurityContext securityContext) {
		NodeInterface.super.afterModification(securityContext);
		notifyLocatables();
	}

	@Override
	default boolean isValid(ErrorBuffer errorBuffer) {

		boolean valid = NodeInterface.super.isValid(errorBuffer);

		valid &= notifyLocatables();

		return valid;

	}

	default boolean notifyLocatables() {

		// FIXME: LocationRelationship has a direction. but it is ignored here

		boolean allLocatablesAreValid = true;

		for(RelationshipInterface rel : this.getRelationships(NodeHasLocation.class)) {

			NodeInterface otherNode = rel.getOtherNode(this);
			if(otherNode != null && otherNode instanceof Locatable) {

				// notify other node of location change
				allLocatablesAreValid &= !((Locatable)otherNode).locationChanged();
			}
		}

		return allLocatablesAreValid;
	}

}
