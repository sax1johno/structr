/**
 * Copyright (C) 2010-2019 Structr GmbH
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
package org.structr.core.property;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.search.Occurrence;
import org.structr.api.search.SortType;
import org.structr.api.util.Iterables;
import org.structr.common.NotNullPredicate;
import org.structr.common.SecurityContext;
import org.structr.common.TruePredicate;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.ManyEndpoint;
import org.structr.core.entity.Relation;
import org.structr.core.entity.Source;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.search.EmptySearchAttribute;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SourceSearchAttribute;
import org.structr.core.notion.Notion;
import org.structr.core.notion.ObjectNotion;

/**
 * A property that defines a relationship with the given parameters between a node and a collection of other nodes.
 *
 *
 */
public class EndNodes<S extends NodeInterface, T extends NodeInterface> extends Property<Iterable<T>> implements RelationProperty<T> {

	private static final Logger logger = LoggerFactory.getLogger(EndNodes.class.getName());

	private Relation<S, T, ? extends Source, ManyEndpoint<T>> relation = null;
	private Notion notion                                              = null;
	private Class<T> destType                                          = null;

	/**
	 * Constructs a collection property with the given name, the given destination type and the given relationship type.
	 *
	 * @param name
	 * @param relationClass
	 */
	public EndNodes(final String name, final Class<? extends Relation<S, T, ? extends Source, ManyEndpoint<T>>> relationClass) {
		this(name, relationClass, new ObjectNotion());
	}

	/**
	 * Constructs a collection property with the given name, the given destination type and the given relationship type.
	 *
	 * @param name
	 * @param relationClass
	 * @param notion
	 */
	public EndNodes(final String name, final Class<? extends Relation<S, T, ? extends Source, ManyEndpoint<T>>> relationClass, final Notion notion) {

		super(name);

		this.relation = Relation.getInstance(relationClass);
		this.notion   = notion;
		this.destType = relation.getTargetType();

		this.notion.setType(destType);
		this.notion.setRelationProperty(this);
		this.relation.setTargetProperty(this);

		StructrApp.getConfiguration().registerConvertedProperty(this);
	}

	/**
	 * Constructs a collection property with the given name, the given destination type and the given relationship type.
	 *
	 * @param name
	 * @param relation
	 * @param notion
	 */
	public EndNodes(final String name, final Relation<S, T, ? extends Source, ManyEndpoint<T>> relation, final Notion notion) {

		super(name);

		this.relation = relation;
		this.notion   = notion;
		this.destType = relation.getTargetType();

		this.notion.setType(destType);
		this.notion.setRelationProperty(this);

		StructrApp.getConfiguration().registerConvertedProperty(this);
	}

	@Override
	public String typeName() {
		return "collection";
	}

	@Override
	public SortType getSortType() {
		return SortType.Default;
	}

	@Override
	public PropertyConverter<Iterable<T>, ?> databaseConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public PropertyConverter<Iterable<T>, ?> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return null;
	}

	@Override
	public PropertyConverter<?, Iterable<T>> inputConverter(SecurityContext securityContext) {
		return getNotion().getCollectionConverter(securityContext);
	}

	@Override
	public Iterable<T> getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public Iterable<T> getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter, final Predicate<GraphObject> predicate) {

		ManyEndpoint<T> endpoint = relation.getTarget();

		if (predicate != null) {

			return Iterables.filter(predicate, Iterables.filter(new NotNullPredicate(), endpoint.get(securityContext, (NodeInterface)obj, new TruePredicate(predicate.comparator()))));

		} else {

			return Iterables.filter(new NotNullPredicate(), endpoint.get(securityContext, (NodeInterface)obj, null));
		}
	}

	@Override
	public Object setProperty(SecurityContext securityContext, GraphObject obj, Iterable<T> collection) throws FrameworkException {

		final ManyEndpoint<T> endpoint = relation.getTarget();

		return endpoint.set(securityContext, (NodeInterface)obj, collection);
	}

	@Override
	public Class relatedType() {
		return destType;
	}

	@Override
	public Class valueType() {
		return relatedType();
	}

	@Override
	public boolean isCollection() {
		return true;
	}

	@Override
	public Property<Iterable<T>> indexed() {
		return this;
	}

	@Override
	public Property<Iterable<T>> passivelyIndexed() {
		return this;
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		return null;
	}

	@Override
	public boolean isIndexed() {
		return false;
	}

	@Override
	public boolean isPassivelyIndexed() {
		return false;
	}

	// ----- interface RelationProperty -----
	@Override
	public Notion getNotion() {
		return notion;
	}

	@Override
	public void addSingleElement(final SecurityContext securityContext, final GraphObject obj, final T t) throws FrameworkException {

		final List<T> list = Iterables.toList(getProperty(securityContext, obj, false));

		list.add(t);

		setProperty(securityContext, obj, list);
	}

	@Override
	public Class<T> getTargetType() {
		return destType;
	}

	@Override
	public Iterable<T> convertSearchValue(SecurityContext securityContext, String requestParameter) throws FrameworkException {

		final PropertyConverter inputConverter = inputConverter(securityContext);
		if (inputConverter != null) {

			final List<String> sources = new LinkedList<>();
			if (requestParameter != null) {

				for (String part : requestParameter.split("[,;]+")) {
					sources.add(part);
				}
			}

			return (Iterable<T>)inputConverter.convert(sources);
		}

		return null;
	}

	@Override
	public SearchAttribute getSearchAttribute(SecurityContext securityContext, Occurrence occur, Iterable<T> searchValue, boolean exactMatch, final Query query) {

		final Predicate<GraphObject> predicate    = query != null ? query.toPredicate() : null;
		final SourceSearchAttribute attr          = new SourceSearchAttribute(occur);

		if (searchValue != null && searchValue.iterator().hasNext()) {

			if (!Occurrence.FORBIDDEN.equals(occur)) {

				final Set<GraphObject> intersectionResult = new LinkedHashSet<>();

				for (NodeInterface node : searchValue) {

					intersectionResult.addAll(getRelatedNodesReverse(securityContext, node, declaringClass, predicate));
				}

				attr.setResult(intersectionResult);
			}

		} else {

			// experimental filter attribute that removes entities with a non-empty value in the given field
			return new EmptySearchAttribute(this, null, true);
		}

		return attr;
	}

	// ----- overridden methods from super class -----
	@Override
	protected <T extends NodeInterface> Set<T> getRelatedNodesReverse(final SecurityContext securityContext, final NodeInterface obj, final Class destinationType, final Predicate<GraphObject> predicate) {

		Set<T> relatedNodes = new LinkedHashSet<>();

		try {

			final Object source = relation.getSource().get(securityContext, obj, predicate);
			if (source != null) {

				if (source instanceof Iterable) {

					Iterable<T> nodes = (Iterable<T>)source;
					for (final T n : nodes) {

						relatedNodes.add(n);
					}

				} else {

					relatedNodes.add((T)source);
				}
			}

		} catch (Throwable t) {

			logger.warn("Unable to fetch related node: {}", t.getMessage());
		}

		return relatedNodes;
	}

	@Override
	public Relation getRelation() {
		return relation;
	}

	@Override
	public boolean doAutocreate() {

		if (relation != null) {

			switch (relation.getAutocreationFlag()) {

				case Relation.ALWAYS:
				case Relation.SOURCE_TO_TARGET:
					return true;
			}
		}

		return false;
	}

	@Override
	public String getAutocreateFlagName() {

		if (relation != null) {
			return Relation.CASCADING_DESCRIPTIONS[relation.getAutocreationFlag()];
		}

		return Relation.CASCADING_DESCRIPTIONS[0];
	}

	@Override
	public String getDirectionKey() {
		return "out";
	}
}
