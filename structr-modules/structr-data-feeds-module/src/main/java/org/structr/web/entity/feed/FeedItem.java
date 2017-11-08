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

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.ValidationHelper;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.fulltext.FulltextIndexer;
import org.structr.common.fulltext.Indexable;
import org.structr.core.Export;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.property.EndNodes;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.rest.common.HttpHelper;
import org.structr.schema.SchemaService;
import org.structr.web.entity.relation.FeedItemContents;
import org.structr.web.entity.relation.FeedItemEnclosures;
import org.structr.web.entity.relation.FeedItems;

/**
 * Represents a single item of a data feed.
 *
 */
public interface FeedItem extends Indexable {

	static class Impl { static { SchemaService.registerMixinType(FeedItem.class); }}

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

	@Override
	default boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = Indexable.super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidStringNotBlank(this, url, errorBuffer);
		valid &= ValidationHelper.isValidUniqueProperty(this, url, errorBuffer);

		return valid;
	}

	@Override
	default boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		final FulltextIndexer indexer = StructrApp.getInstance(securityContext).getFulltextIndexer();
		indexer.addToFulltextIndex(this);

		return Indexable.super.onCreation(securityContext, errorBuffer);
	}

	@Override
	default void afterCreation(final SecurityContext securityContext) {

		try {
			final FulltextIndexer indexer = StructrApp.getInstance(securityContext).getFulltextIndexer();
			indexer.addToFulltextIndex(this);

		} catch (FrameworkException fex) {

			logger.warn("Unable to index " + this, fex);
		}
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

	default void increaseVersion() throws FrameworkException {

		final Integer _version = getProperty(FeedItem.version);

		unlockSystemPropertiesOnce();
		if (_version == null) {

			setProperty(FeedItem.version, 1);

		} else {

			setProperty(FeedItem.version, _version + 1);
		}
	}

	@Override
	default InputStream getInputStream() {

		final String remoteUrl = getProperty(url);
		if (StringUtils.isNotBlank(remoteUrl)) {

			return HttpHelper.getAsStream(remoteUrl);
		}

		return null;
	}
}
