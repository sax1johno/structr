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
package org.structr.web.entity.dom;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.profiles.pegdown.Extensions;
import com.vladsch.flexmark.profiles.pegdown.PegdownOptionsAdapter;
import com.vladsch.flexmark.util.options.MutableDataSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.java.textilej.parser.MarkupParser;
import net.java.textilej.parser.markup.confluence.ConfluenceDialect;
import net.java.textilej.parser.markup.mediawiki.MediaWikiDialect;
import net.java.textilej.parser.markup.textile.TextileDialect;
import net.java.textilej.parser.markup.trac.TracWikiDialect;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Asciidoctor.Factory;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Adapter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.PropertyMap;
import org.structr.schema.SchemaService;
import org.structr.web.entity.relation.Sync;

/**
 * Represents a content node. This class implements the org.w3c.dom.Text interface.
 * All methods in the W3C Text interface are based on the raw database content.
 *
 *
 */
public class ContentMixin extends AbstractNode implements Content {

	static {

		SchemaService.registerMixinType("Content", AbstractNode.class, Content.class);
	}

	// ----- BEGIN Structr Mixin -----

	private static final Map<String, Adapter<String, String>> contentConverters          = new LinkedHashMap<>();

	private static final ThreadLocalAsciiDocProcessor asciiDocProcessor                  = new ThreadLocalAsciiDocProcessor();
	private static final ThreadLocalTracWikiProcessor tracWikiProcessor                  = new ThreadLocalTracWikiProcessor();
	private static final ThreadLocalTextileProcessor textileProcessor                    = new ThreadLocalTextileProcessor();
	private static final ThreadLocalFlexMarkProcessor flexMarkProcessor                  = new ThreadLocalFlexMarkProcessor();
	private static final ThreadLocalMediaWikiProcessor mediaWikiProcessor                = new ThreadLocalMediaWikiProcessor();
	private static final ThreadLocalConfluenceProcessor confluenceProcessor              = new ThreadLocalConfluenceProcessor();

	static {

		contentConverters.put("text/markdown", new Adapter<String, String>() {

			@Override
			public String adapt(String s) throws FrameworkException {

				if (s != null) {
					com.vladsch.flexmark.ast.Node document = flexMarkProcessor.get().parser.parse(s);
					return flexMarkProcessor.get().renderer.render(document);
				}

				return "";
			}

		});
		contentConverters.put("text/textile", new Adapter<String, String>() {

			@Override
			public String adapt(String s) throws FrameworkException {

				if (s != null) {
					return textileProcessor.get().parseToHtml(s);
				}

				return "";

			}

		});
		contentConverters.put("text/mediawiki", new Adapter<String, String>() {

			@Override
			public String adapt(String s) throws FrameworkException {

				if (s != null) {
					return mediaWikiProcessor.get().parseToHtml(s);
				}

				return "";
			}

		});
		contentConverters.put("text/tracwiki", new Adapter<String, String>() {

			@Override
			public String adapt(String s) throws FrameworkException {

				if (s != null) {
					return tracWikiProcessor.get().parseToHtml(s);
				}

				return "";

			}

		});
		contentConverters.put("text/confluence", new Adapter<String, String>() {

			@Override
			public String adapt(String s) throws FrameworkException {

				if (s != null) {
					return confluenceProcessor.get().parseToHtml(s);
				}

				return "";

			}

		});
		contentConverters.put("text/asciidoc", new Adapter<String, String>() {

			@Override
			public String adapt(String s) throws FrameworkException {

				if (s != null) {
					return asciiDocProcessor.get().render(s, new HashMap<String, Object>());
				}

				return "";

			}

		});
	}

	@Override
	public Object getFeature(final String version, final String feature) {
		return null;
	}

	@Override
	public boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		if (super.isValid(errorBuffer)) {

			if (getProperty(Content.contentType) == null) {
				setProperty(Content.contentType, "text/plain");
			}

			return true;
		}

		return false;
	}

	@Override
	public boolean onModification(SecurityContext securityContext, ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		if (super.onModification(securityContext, errorBuffer, modificationQueue)) {

			for (final Sync rel : getOutgoingRelationships(Sync.class)) {

				final Content syncedNode = (Content) rel.getTargetNode();
				final PropertyMap map    = new PropertyMap();

				// sync content only
				map.put(content, getProperty(content));
				map.put(contentType, getProperty(contentType));
				map.put(name, getProperty(name));

				syncedNode.setProperties(securityContext, map);
			}

			final Sync rel = getIncomingRelationship(Sync.class);
			if (rel != null) {

				final Content otherNode = (Content) rel.getSourceNode();
				if (otherNode != null) {

					final PropertyMap map = new PropertyMap();

					// sync both ways
					map.put(content, getProperty(content));
					map.put(contentType, getProperty(contentType));
					map.put(name, getProperty(name));

					otherNode.setProperties(otherNode.getSecurityContext(), map);
				}
			}

			return true;
		}

		return false;
	}

	@Override
	public Adapter<String, String> getContentConverter(final String contentType) {
		return contentConverters.get(contentType);
	}

	// ----- nested classes -----
	private static class ThreadLocalConfluenceProcessor extends ThreadLocal<MarkupParser> {

		@Override
		protected MarkupParser initialValue() {

			return new MarkupParser(new ConfluenceDialect());

		}

	}


	private static class ThreadLocalMediaWikiProcessor extends ThreadLocal<MarkupParser> {

		@Override
		protected MarkupParser initialValue() {

			return new MarkupParser(new MediaWikiDialect());

		}

	}

	private static class ThreadLocalFlexMarkProcessor extends ThreadLocal<FlexMarkProcessor> {

		@Override
		protected FlexMarkProcessor initialValue() {

			final MutableDataSet options = new MutableDataSet();

                        options.setAll(PegdownOptionsAdapter.flexmarkOptions(Extensions.ALL));
//			options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");

			Parser parser = Parser.builder(options).build();
			HtmlRenderer renderer = HtmlRenderer.builder(options).build();

			return new FlexMarkProcessor(parser, renderer);
		}

	}


	private static class ThreadLocalTextileProcessor extends ThreadLocal<MarkupParser> {

		@Override
		protected MarkupParser initialValue() {

			return new MarkupParser(new TextileDialect());

		}

	}

	private static class ThreadLocalTracWikiProcessor extends ThreadLocal<MarkupParser> {

		@Override
		protected MarkupParser initialValue() {

			return new MarkupParser(new TracWikiDialect());

		}
	}

	private static class ThreadLocalAsciiDocProcessor extends ThreadLocal<Asciidoctor> {

		@Override
		protected Asciidoctor initialValue() {

			return Factory.create();
		}
	}

	private static class FlexMarkProcessor {

		Parser parser;
		HtmlRenderer renderer;

		public FlexMarkProcessor(final Parser parser, final HtmlRenderer renderer) {
			this.parser = parser;
			this.renderer = renderer;
		}
	}
	// ----- END Structr Mixin -----
}
