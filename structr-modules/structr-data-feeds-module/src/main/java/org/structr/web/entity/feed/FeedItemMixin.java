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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.ValidationHelper;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.fulltext.FulltextIndexer;
import static org.structr.common.fulltext.Indexable.extractedContent;
import org.structr.core.Export;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.schema.SchemaService;
import org.structr.rest.common.HttpHelper;
import static org.structr.web.entity.feed.FeedItem.url;

/**
 * Represents a single item of a data feed.
 *
 */
public class FeedItemMixin extends AbstractNode implements FeedItem {

        static {
            SchemaService.registerMixinType("FeedItem", AbstractNode.class, FeedItemMixin.class);
        }

	private static final Logger logger = LoggerFactory.getLogger(FeedItemMixin.class.getName());

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidStringNotBlank(this, url, errorBuffer);
		valid &= ValidationHelper.isValidUniqueProperty(this, url, errorBuffer);

		return valid;
	}

	@Override
	public boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		final FulltextIndexer indexer = StructrApp.getInstance(securityContext).getFulltextIndexer();
		indexer.addToFulltextIndex(this);

		return super.onCreation(securityContext, errorBuffer);
	}

	@Override
	public void afterCreation(final SecurityContext securityContext) {

		try {
			final FulltextIndexer indexer = StructrApp.getInstance(securityContext).getFulltextIndexer();
			indexer.addToFulltextIndex(this);

		} catch (FrameworkException fex) {

			logger.warn("Unable to index " + this, fex);
		}
	}

	@Export
	@Override
	public GraphObject getSearchContext(final String searchTerm, final int contextLength) {

		final String text = getProperty(extractedContent);
		if (text != null) {

			final FulltextIndexer indexer = StructrApp.getInstance(securityContext).getFulltextIndexer();
			return indexer.getContextObject(searchTerm, text, contextLength);
		}

		return null;
	}

	public void increaseVersion() throws FrameworkException {

		final Integer _version = getProperty(FeedItemMixin.version);

		unlockSystemPropertiesOnce();
		if (_version == null) {

			setProperty(FeedItemMixin.version, 1);

		} else {

			setProperty(FeedItemMixin.version, _version + 1);
		}
	}

	@Override
	public InputStream getInputStream() {

		final String remoteUrl = getProperty(url);
		if (StringUtils.isNotBlank(remoteUrl)) {

			return HttpHelper.getAsStream(remoteUrl);
		}

		return null;
	}
}
