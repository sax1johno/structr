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
package org.structr.core.graph;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.graph.Node;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.bolt.wrapper.EntityWrapper;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Relation;
import org.structr.core.function.ChangelogFunction;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.PropertyKey;

/**
 *
 *
 */

public class ModificationQueue {

	private static final Logger logger = LoggerFactory.getLogger(ModificationQueue.class.getName());

	private final ConcurrentSkipListMap<String, GraphObjectModificationState> modifications = new ConcurrentSkipListMap<>();
	private final Collection<ModificationEvent> modificationEvents                          = new ArrayDeque<>(1000);
	private final Map<String, TransactionPostProcess> postProcesses                         = new LinkedHashMap<>();
	private final Set<String> alreadyPropagated                                             = new LinkedHashSet<>();
	private final Set<String> synchronizationKeys                                           = new TreeSet<>();
	private boolean doUpateChangelogIfEnabled                                               = true;

	public ModificationQueue() {
		this(true);
	}

	public ModificationQueue(final boolean doUpdateChangelogIfEnabled) {
		this.doUpateChangelogIfEnabled = doUpdateChangelogIfEnabled;
	}

	/**
	 * Returns a set containing the different entity types of
	 * nodes modified in this queue.
	 *
	 * @return the types
	 */
	public Set<String> getSynchronizationKeys() {
		return synchronizationKeys;
	}

	public int getSize() {
		return modifications.size();
	}

	public boolean doInnerCallbacks(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		long t0                  = System.currentTimeMillis();
		boolean hasModifications = true;

		// collect all modified nodes
		while (hasModifications) {

			hasModifications = false;

			for (GraphObjectModificationState state : getSortedModifications()) {

				if (state.wasModified()) {

					// do callback according to entry state
					if (!state.doInnerCallback(this, securityContext, errorBuffer)) {
						return false;
					}

					hasModifications = true;
				}
			}
		}

		long t = System.currentTimeMillis() - t0;
		if (t > 1000) {
			logger.info("{} ms ({} modifications)", t, modifications.size());
		}

		return true;
	}

	public boolean doValidation(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final boolean doValidation) throws FrameworkException {

		long t0 = System.currentTimeMillis();

		long validationTime = 0;
		long indexingTime = 0;

		// do validation and indexing
		for (final GraphObjectModificationState state : getSortedModifications()) {

			PropertyContainer container = state.getGraphObject().getPropertyContainer();
			if (container instanceof EntityWrapper && ((EntityWrapper) container).isStale()) {
				continue;
			}

			// do callback according to entry state
			boolean res = state.doValidationAndIndexing(this, securityContext, errorBuffer, doValidation);

			validationTime += state.getValdationTime();
			indexingTime += state.getIndexingTime();

			if (!res) {
				return false;
			}
		}

		long t = System.currentTimeMillis() - t0;
		if (t > 1000) {

			logger.info("doValidation: {} ms ({} modifications)   ({} ms validation - {} ms indexing)", t, modifications.size(), validationTime, indexingTime);
		}

		return true;
	}

	public boolean doPostProcessing(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		long t0 = System.currentTimeMillis();

		for (final TransactionPostProcess process : postProcesses.values()) {

			if (!process.execute(securityContext, errorBuffer)) {
				return false;
			}
		}

		long t = System.currentTimeMillis() - t0;
		if (t > 1000) {
			logger.info("doPostProcessing: {} ms", t);
		}

		return true;
	}

	public void doOuterCallbacks(final SecurityContext securityContext) throws FrameworkException {

		long t0 = System.currentTimeMillis();

		// copy modifications, do after transaction callbacks
		for (GraphObjectModificationState state : modifications.values()) {
			state.doOuterCallback(securityContext);
		}

		long t = System.currentTimeMillis() - t0;
		if (t > 3000) {
			logger.info("doOutCallbacks: {} ms ({} modifications)", t, modifications.size());
		}
	}

	public void updateChangelog() {

		if (doUpateChangelogIfEnabled && (Settings.ChangelogEnabled.getValue() || Settings.UserChangelogEnabled.getValue())) {

			for (final ModificationEvent ev: modificationEvents) {

				try {

					if (Settings.ChangelogEnabled.getValue()) {

						final GraphObject obj = ev.getGraphObject();
						final String newLog   = ev.getChangeLog();

						if (obj != null) {

							final String uuid           = ev.isDeleted() ? ev.getUuid() : obj.getUuid();
							final String typeFolderName = obj.isNode() ? "n" : "r";

							java.io.File file = ChangelogFunction.getChangeLogFileOnDisk(typeFolderName, uuid, true);

							FileUtils.write(file, newLog, "utf-8", true);
						}
					}

					if (Settings.UserChangelogEnabled.getValue()) {

						for (Map.Entry<String, StringBuilder> entry : ev.getUserChangeLogs().entrySet()) {

							java.io.File file = ChangelogFunction.getChangeLogFileOnDisk("u", entry.getKey(), true);

							FileUtils.write(file, entry.getValue().toString(), "utf-8", true);
						}
					}

				} catch (IOException ioex) {
					logger.error("Unable to write changelog to file: {}", ioex.getMessage());
				} catch (Throwable t) {
					logger.warn("", t);
				}
			}
		}
	}

	public void clear() {

		// clear collections afterwards
		alreadyPropagated.clear();
		modifications.clear();
		modificationEvents.clear();
	}

	public void create(final Principal user, final NodeInterface node) {

		getState(node).create();

		if (Settings.ChangelogEnabled.getValue() || Settings.UserChangelogEnabled.getValue()) {

			getState(node).updateChangeLog(user, GraphObjectModificationState.Verb.create, node.getUuid());

		}
	}

	public <S extends NodeInterface, T extends NodeInterface> void create(final Principal user, final RelationshipInterface relationship) {

		getState(relationship).create();

		final NodeInterface sourceNode = relationship.getSourceNode();
		final NodeInterface targetNode = relationship.getTargetNode();

		if (sourceNode != null && targetNode != null) {

			modifyEndNodes(user, sourceNode, targetNode, relationship, false);

			if (Settings.ChangelogEnabled.getValue() || Settings.UserChangelogEnabled.getValue()) {

				getState(relationship).updateChangeLog(user, GraphObjectModificationState.Verb.create, relationship.getType(), relationship.getUuid(), sourceNode.getUuid(), targetNode.getUuid());

				getState(sourceNode).updateChangeLog(user, GraphObjectModificationState.Verb.link, relationship.getType(), relationship.getUuid(), targetNode.getUuid(), GraphObjectModificationState.Direction.out);
				getState(targetNode).updateChangeLog(user, GraphObjectModificationState.Verb.link, relationship.getType(), relationship.getUuid(), sourceNode.getUuid(), GraphObjectModificationState.Direction.in);

			}
		}
	}

	public void modifyOwner(NodeInterface node) {
		getState(node).modifyOwner();
	}

	public void modifySecurity(NodeInterface node) {
		getState(node).modifySecurity();
	}

	public void modifyLocation(NodeInterface node) {
		getState(node).modifyLocation();
	}

	public void modify(final Principal user, final NodeInterface node, final PropertyKey key, final Object previousValue, final Object newValue) {

		getState(node).modify(user, key, previousValue, newValue);

		if (key != null&& key.requiresSynchronization()) {
			synchronizationKeys.add(key.getSynchronizationKey());
		}
	}

	public void modify(final Principal user, RelationshipInterface relationship, PropertyKey key, Object previousValue, Object newValue) {

		getState(relationship).modify(user, key, previousValue, newValue);

		if (key != null && key.requiresSynchronization()) {
			synchronizationKeys.add(key.getSynchronizationKey());
		}
	}

	public void propagatedModification(NodeInterface node) {

		if (node != null) {

			GraphObjectModificationState state = getState(node, true);
			if (state != null) {

				state.propagatedModification();

				// save hash to avoid repeated propagation
				alreadyPropagated.add(hash(node.getNode()));
			}
		}
	}

	public void delete(final Principal user, final NodeInterface node) {

		getState(node).delete(false);

		if (Settings.ChangelogEnabled.getValue() || Settings.UserChangelogEnabled.getValue()) {

			getState(node).updateChangeLog(user, GraphObjectModificationState.Verb.delete, node.getUuid());
		}
	}

	public void delete(final Principal user, final RelationshipInterface relationship, final boolean passive) {

		getState(relationship).delete(passive);

		final NodeInterface sourceNode = relationship.getSourceNodeAsSuperUser();
		final NodeInterface targetNode = relationship.getTargetNodeAsSuperUser();

		modifyEndNodes(user, sourceNode, targetNode, relationship, true);

		if (Settings.ChangelogEnabled.getValue() || Settings.UserChangelogEnabled.getValue()) {

			getState(relationship).updateChangeLog(user, GraphObjectModificationState.Verb.delete, relationship.getType(), relationship.getUuid(), sourceNode.getUuid(), targetNode.getUuid());

			getState(sourceNode).updateChangeLog(user, GraphObjectModificationState.Verb.unlink, relationship.getType(), relationship.getUuid(), targetNode.getUuid(), GraphObjectModificationState.Direction.out);
			getState(targetNode).updateChangeLog(user, GraphObjectModificationState.Verb.unlink, relationship.getType(), relationship.getUuid(), sourceNode.getUuid(), GraphObjectModificationState.Direction.in);

		}
	}

	public Collection<ModificationEvent> getModificationEvents() {
		return modificationEvents;
	}

	public void postProcess(final String key, final TransactionPostProcess process) {

		if (!postProcesses.containsKey(key)) {

			this.postProcesses.put(key, process);
		}
	}

	public boolean isDeleted(final Node node) {

		final GraphObjectModificationState state = modifications.get(hash(node));
		if (state != null) {

			return state.isDeleted() || state.isPassivelyDeleted();
		}

		return false;
	}

	public boolean isDeleted(final Relationship rel) {

		final GraphObjectModificationState state = modifications.get(hash(rel));
		if (state != null) {

			return state.isDeleted() || state.isPassivelyDeleted();
		}

		return false;
	}

	public void registerNodeCallback(final NodeInterface node, final String callbackId) {
		getState(node).setCallbackId(callbackId);
	}

	public void registerRelCallback(final RelationshipInterface rel, final String callbackId) {
		getState(rel).setCallbackId(callbackId);
	}

	/**
	 * Checks if the given key is present for the given graph object in the modifiedProperties of this queue.<br><br>
	 *
	 * This method is convenient if only one key has to be checked. If different
	 * actions should be taken for different keys one should rather use {@link #getModifiedProperties}.
	 *
	 * Note: This method only works for regular properties, not relationship properties (i.e. owner etc)
	 *
	 * @param graphObject The GraphObject we are interested in
	 * @param key The key to check
	 * @return
	 */
	public boolean isPropertyModified(final GraphObject graphObject, final PropertyKey key) {

		for (GraphObjectModificationState state : getSortedModifications()) {

			for (PropertyKey k : state.getModifiedProperties().keySet()) {

				if (k.equals(key) && graphObject.getUuid().equals(state.getGraphObject().getUuid()) ) {

					return true;

				}

			}

		}

		return false;
	}

	/**
	 * Returns a set of all modified keys.<br><br>
	 * Useful if different actions should be taken for different keys and we
	 * don't want to iterate over the subsets multiple times.
	 *
	 * If only one key is to be checked {@link #isPropertyModified} is preferred.
	 *
	 * Note: This method only works for regular properties, not relationship properties (i.e. owner etc)
	 *
	 * @return Set with all modified keys
	 */
	public Set<PropertyKey> getModifiedProperties () {

		HashSet<PropertyKey> modifiedKeys = new HashSet<>();

		for (GraphObjectModificationState state : getSortedModifications()) {

			for (PropertyKey key : state.getModifiedProperties().keySet()) {

				if (!modifiedKeys.contains(key)) {

					modifiedKeys.add(key);

				}

			}

		}

		return modifiedKeys;

	}

	public GraphObjectMap getModifications(final GraphObject forObject) {

		final GraphObjectMap result = new GraphObjectMap();
		final GraphObjectModificationState state;

		if (forObject.isNode()) {

			state = getState((NodeInterface)forObject);

		} else {

			state = getState((RelationshipInterface)forObject);
		}

		final GraphObjectMap before = new GraphObjectMap();
		final GraphObjectMap after  = new GraphObjectMap();

		before.putAll(state.getRemovedProperties());

		after.putAll(state.getModifiedProperties());
		after.putAll(state.getNewProperties());

		result.put(new GenericProperty("before"),  before);
		result.put(new GenericProperty("after"),   after);
		result.put(new GenericProperty("added"),   state.getAddedRemoteProperties());
		result.put(new GenericProperty("removed"), state.getRemovedRemoteProperties());

		return result;
	}

	public void disableChangelog() {
		this.doUpateChangelogIfEnabled = false;
	}

	// ----- private methods -----
	private void modifyEndNodes(final Principal user, final NodeInterface startNode, final NodeInterface endNode, final RelationshipInterface rel, final boolean isDeletion) {

		// only modify if nodes are accessible
		if (startNode != null && endNode != null) {

			final RelationshipType relType = rel.getRelType();

			if (RelType.OWNS.equals(relType)) {

				modifyOwner(startNode);
				modifyOwner(endNode);
				return;
			}

			if (RelType.SECURITY.equals(relType)) {

				modifySecurity(startNode);
				modifySecurity(endNode);
				return;
			}

			if (RelType.IS_AT.equals(relType)) {

				modifyLocation(startNode);
				modifyLocation(endNode);
				return;
			}

			final Relation relation  = Relation.getInstance((Class)rel.getClass());
			final PropertyKey source = relation.getSourceProperty();
			final PropertyKey target = relation.getTargetProperty();
			final Object sourceValue = source != null && source.isCollection() ? new LinkedList<>() : null;
			final Object targetValue = target != null && target.isCollection() ? new LinkedList<>() : null;

			modify(user, startNode, target, null, targetValue);
			modify(user, endNode, source, null, sourceValue);

			if (source != null && target != null) {

				if (isDeletion) {

					// update removed properties
					getState(startNode).remove(target, endNode);
					getState(endNode).remove(source, startNode);

				} else {


					// update added properties
					getState(startNode).add(target, endNode);
					getState(endNode).add(source, startNode);
				}

			} else {

				// dont log so much..
				//logger.warn("No properties registered for {}: source: {}, target: {}", rel.getClass(), source, target);
			}
		}
	}

	private GraphObjectModificationState getState(final NodeInterface node) {
		return getState(node, false);
	}

	private GraphObjectModificationState getState(final NodeInterface node, final boolean checkPropagation) {

		String hash = hash(node.getNode());
		GraphObjectModificationState state = modifications.get(hash);

		if (state == null && !(checkPropagation && alreadyPropagated.contains(hash))) {

			state = new GraphObjectModificationState(node);
			modifications.put(hash, state);
			modificationEvents.add(state);
		}

		return state;
	}

	private GraphObjectModificationState getState(final RelationshipInterface rel) {
		return getState(rel, true);
	}

	private GraphObjectModificationState getState(final RelationshipInterface rel, final boolean create) {

		String hash = hash(rel.getRelationship());
		GraphObjectModificationState state = modifications.get(hash);

		if (state == null && create) {

			state = new GraphObjectModificationState(rel);
			modifications.put(hash, state);
			modificationEvents.add(state);
		}

		return state;
	}

	private String hash(final Node node) {
		return "N" + node.getId();
	}

	private String hash(final Relationship rel) {
		return "R" + rel.getId();
	}

	private Iterable<GraphObjectModificationState> getSortedModifications() {

		final List<GraphObjectModificationState> state = new LinkedList<>(modifications.values());

		Collections.sort(state, (a, b) -> {

			final long t1 = a.getTimestamp();
			final long t2 = b.getTimestamp();

			if (t1 < t2) {
				return -1;
			}

			if (t1 > t2) {
				return 1;
			}

			return 0;
		});

		return state;
	}
}
