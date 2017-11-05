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
import org.structr.common.fulltext.Indexable;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.EndNodes;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.web.entity.relation.FeedItemContents;
import org.structr.web.entity.relation.FeedItemEnclosures;
import org.structr.web.entity.relation.FeedItems;

/**
 * Represents a single item of a data feed.
 *
 */
public interface FeedItem extends NodeInterface, Indexable {

	public static final Property<String> url                         = new StringProperty("url").unique().indexed();
	public static final Property<String> author                      = new StringProperty("author");
	public static final Property<String> comments                    = new StringProperty("comments");
        public static final Property<String> description                 = new StringProperty("description");
	public static final Property<List<FeedItemContent>> contents     = new EndNodes<>("contents", FeedItemContents.class);
        public static final Property<List<FeedItemEnclosure>> enclosures = new EndNodes<>("enclosures", FeedItemEnclosures.class);
	public static final Property<Date> pubDate                       = new ISO8601DateProperty("pubDate").indexed();

	public static final Property<Long> checksum                      = new LongProperty("checksum").indexed().unvalidated().readOnly();
	public static final Property<Integer> cacheForSeconds            = new IntProperty("cacheForSeconds").cmis();
	public static final Property<Integer> version                    = new IntProperty("version").indexed().readOnly();

	public static final Property<DataFeed> feed                      = new StartNode<>("feed", FeedItems.class);

	public static final View publicView = new View(FeedItem.class, PropertyView.Public, type, name, contentType, owner,
		url, author, comments, contents, pubDate, description, enclosures
	);

	public static final View uiView     = new View(FeedItem.class, PropertyView.Ui, type, contentType, checksum, version, cacheForSeconds, owner, extractedContent, indexedWords,
		url, feed, author, comments, contents, pubDate, description, enclosures);
}
