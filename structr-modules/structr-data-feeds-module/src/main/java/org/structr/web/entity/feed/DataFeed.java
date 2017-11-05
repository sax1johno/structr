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
package org.structr.web.entity.feed;


import java.util.Date;
import java.util.List;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.EndNodes;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.web.entity.relation.FeedItems;


public interface DataFeed extends NodeInterface {

	public static final Property<List<FeedItem>> items          = new EndNodes<>("items", FeedItems.class);
	public static final Property<String>         url            = new StringProperty("url").indexed();
	public static final Property<String>         feedType       = new StringProperty("feedType").indexed();
	public static final Property<String>         description    = new StringProperty("description").indexed();
	public static final Property<Long>           updateInterval = new LongProperty("updateInterval"); // update interval in milliseconds
	public static final Property<Date>           lastUpdated    = new ISO8601DateProperty("lastUpdated");
	public static final Property<Long>           maxAge         = new LongProperty("maxAge"); // maximum age of the oldest feed entry in milliseconds
	public static final Property<Integer>        maxItems       = new IntProperty("maxItems"); // maximum number of feed entries to retain

	public static final View defaultView = new View(DataFeed.class, PropertyView.Public,
		id, type, url, items, feedType, description
	);

	public static final View uiView = new View(DataFeed.class, PropertyView.Ui,
		id, name, owner, type, createdBy, deleted, hidden, createdDate, lastModifiedDate,
		visibleToPublicUsers, visibleToAuthenticatedUsers, visibilityStartDate, visibilityEndDate,
                url, items, feedType, description, lastUpdated, maxAge, maxItems, updateInterval
	);

	void updateIfDue();
}
