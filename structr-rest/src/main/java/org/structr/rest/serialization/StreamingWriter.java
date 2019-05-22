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
package org.structr.rest.serialization;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.util.ResultStream;
import org.structr.common.PropertyView;
import org.structr.common.QueryRange;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.core.GraphObject;
import org.structr.core.Value;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

/**
 *
 *
 */
public abstract class StreamingWriter {

	private static final Logger logger                   = LoggerFactory.getLogger(StreamingWriter.class.getName());
	private static final Set<PropertyKey> idTypeNameOnly = new LinkedHashSet<>();
	private static final Set<String> restrictedViews     = new HashSet<>();

	static {

		idTypeNameOnly.add(GraphObject.id);
		idTypeNameOnly.add(AbstractNode.type);
		idTypeNameOnly.add(AbstractNode.name);

		restrictedViews.add(PropertyView.All);
		restrictedViews.add(PropertyView.Ui);
		restrictedViews.add(PropertyView.Custom);
	}

	private final ExecutorService threadPool              = Executors.newWorkStealingPool();
	private final Map<String, Serializer> serializerCache = new LinkedHashMap<>();
	private final Map<String, Serializer> serializers     = new LinkedHashMap<>();
	private final Serializer<GraphObject> root            = new RootSerializer();
	private final Set<String> nonSerializerClasses        = new LinkedHashSet<>();
	private final DecimalFormat decimalFormat             = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
	private String resultKeyName                          = "result";
	private boolean renderSerializationTime               = true;
	private boolean reduceRedundancy                      = false;
	private int outputNestingDepth                        = 3;
	private Value<String> propertyView                    = null;
	protected boolean indent                              = true;
	protected boolean compactNestedProperties             = true;
	protected boolean wrapSingleResultInArray           = false;

	public abstract RestWriter getRestWriter(final SecurityContext securityContext, final Writer writer);

	public StreamingWriter(final Value<String> propertyView, final boolean indent, final int outputNestingDepth, final boolean wrapSingleResultInArray) {

		this.wrapSingleResultInArray   = wrapSingleResultInArray;
		this.reduceRedundancy          = Settings.JsonRedundancyReduction.getValue(true);
		this.outputNestingDepth        = outputNestingDepth;
		this.propertyView              = propertyView;
		this.indent                    = indent;

		serializers.put(GraphObject.class.getName(), root);
		serializers.put(PropertyMap.class.getName(), new PropertyMapSerializer());
		serializers.put(Iterable.class.getName(),    new IterableSerializer());
		serializers.put(Map.class.getName(),         new MapSerializer());

		nonSerializerClasses.add(Object.class.getName());
		nonSerializerClasses.add(String.class.getName());
		nonSerializerClasses.add(Integer.class.getName());
		nonSerializerClasses.add(Long.class.getName());
		nonSerializerClasses.add(Double.class.getName());
		nonSerializerClasses.add(Float.class.getName());
		nonSerializerClasses.add(Byte.class.getName());
		nonSerializerClasses.add(Character.class.getName());
		nonSerializerClasses.add(StringBuffer.class.getName());
		nonSerializerClasses.add(Boolean.class.getName());


		//this.writer = new StructrWriter(writer);
		//this.writer.setIndent("   ");
	}

	public void streamSingle(final SecurityContext securityContext, final Writer output, final GraphObject obj) throws IOException {

		final Set<Integer> visitedObjects = new LinkedHashSet<>();
		final RestWriter writer           = getRestWriter(securityContext, output);
		final String view                 = propertyView.get(securityContext);

		configureWriter(writer);

		writer.beginDocument(null, view);
		root.serialize(writer, obj, view, 0, visitedObjects);
		writer.endDocument();

	}

	public void stream(final SecurityContext securityContext, final Writer output, final ResultStream result, final String baseUrl) throws IOException {
		stream(securityContext, output, result, baseUrl, true);
	}

	public void stream(final SecurityContext securityContext, final Writer output, final ResultStream result, final String baseUrl, final boolean includeMetadata) throws IOException {

		long t0 = System.nanoTime();

		final RestWriter rootWriter = getRestWriter(securityContext, output);

		configureWriter(rootWriter);

		// result fields in alphabetical order
		final Set<Integer> visitedObjects             = new LinkedHashSet<>();
		final String queryTime                        = result.getQueryTime();
		final Integer page                            = result.getPage();
		final Integer pageSize                        = result.getPageSize();

		rootWriter.beginDocument(baseUrl, propertyView.get(securityContext));
		rootWriter.beginObject();

		if (result != null) {

			rootWriter.name(resultKeyName);
			root.serializeRoot(rootWriter, result, propertyView.get(securityContext), 0, visitedObjects);
		}

		if (includeMetadata) {

			// time delta for serialization
			long t1 = System.nanoTime();

			if (pageSize != null && !pageSize.equals(Integer.MAX_VALUE)) {

				if (page != null) {

					rootWriter.name("page").value(page);
				}

				rootWriter.name("page_size").value(pageSize);
			}

			if (queryTime != null) {
				rootWriter.name("query_time").value(queryTime);
			}

			if (!securityContext.ignoreResultCount()) {

				rootWriter.name("result_count").value(result.calculateTotalResultCount());
				rootWriter.name("page_count").value(result.calculatePageCount());
				rootWriter.name("result_count_time").value(decimalFormat.format((System.nanoTime() - t1) / 1000000000.0));
			}

			if (renderSerializationTime) {
				rootWriter.name("serialization_time").value(decimalFormat.format((System.nanoTime() - t0) / 1000000000.0));
			}
		}

		// finished
		rootWriter.endObject();
		rootWriter.endDocument();

		threadPool.shutdown();
	}

	public void setResultKeyName(final String resultKeyName) {
		this.resultKeyName = resultKeyName;
	}

	public void setRenderSerializationTime(final boolean doRender) {
		this.renderSerializationTime = doRender;
	}

	private Serializer getSerializerForType(Class type) {

		Class localType       = type;
		Serializer serializer = serializerCache.get(type.getName());

		if (serializer == null && !nonSerializerClasses.contains(type.getName())) {

			do {
				serializer = serializers.get(localType.getName());

				if (serializer == null) {

					Set<Class> interfaces = new LinkedHashSet<>();
					collectAllInterfaces(localType, interfaces);

					for (Class interfaceType : interfaces) {

						serializer = serializers.get(interfaceType.getName());

						if (serializer != null) {
							break;
						}
					}
				}

				localType = localType.getSuperclass();

			} while (serializer == null && !localType.equals(Object.class));


			// cache found serializer
			if (serializer != null) {
				serializerCache.put(type.getName(), serializer);
			}
		}

		return serializer;
	}

	private void collectAllInterfaces(Class type, Set<Class> interfaces) {

		if (interfaces.contains(type)) {
			return;
		}

		for (Class iface : type.getInterfaces()) {

			collectAllInterfaces(iface, interfaces);
			interfaces.add(iface);
		}
	}

	private void serializePrimitive(RestWriter writer, final Object value) throws IOException {

		if (value != null) {

			if (value instanceof Number) {

				writer.value((Number)value);

			} else if (value instanceof Boolean) {

				writer.value((Boolean)value);

			} else {

				writer.value(value.toString());
			}

		} else {

			writer.nullValue();
		}
	}

	private void configureWriter(final RestWriter writer) {

		if (indent && !writer.getSecurityContext().doMultiThreadedJsonOutput()) {
			writer.setIndent("	");
		}

	}

	// ----- nested classes -----
	public abstract class Serializer<T> {

		public abstract void serialize(final RestWriter writer, final T value, final String localPropertyView, final int depth, final Set<Integer> visitedObjects) throws IOException;

		public void serializeRoot(final RestWriter writer, final Object value, final String localPropertyView, final int depth, final Set<Integer> visitedObjects) throws IOException {

			if (value != null) {

				Serializer serializer = getSerializerForType(value.getClass());
				if (serializer != null) {

					serializer.serialize(writer, value, localPropertyView, depth, visitedObjects);

					return;
				}
			}

			serializePrimitive(writer, value);
		}

		public void serializeProperty(final RestWriter writer, final PropertyKey key, final Object value, final String localPropertyView, final int depth, final Set<Integer> visitedObjects) {

			final SecurityContext securityContext = writer.getSecurityContext();

			try {
				final PropertyConverter converter = key.inputConverter(securityContext);
				if (converter != null) {

					Object convertedValue = null;

					// ignore conversion errors
					try { convertedValue = converter.revert(value); } catch (Throwable t) {}

					serializeRoot(writer, convertedValue, localPropertyView, depth, visitedObjects);

				} else {

					serializeRoot(writer, value, localPropertyView, depth, visitedObjects);
				}

			} catch(Throwable t) {

				logger.warn("Exception while serializing property {} ({}) declared in {} with valuetype {} (value = {}) : {}", new Object[] {
					key.jsonName(),
					key.getClass(),
					key.getClass().getDeclaringClass(),
					value.getClass().getName(),
					value,
					t.getMessage()
				});
			}
		}
	}

	public class RootSerializer extends Serializer<GraphObject> {

		@Override
		public void serialize(final RestWriter writer, final GraphObject source, final String localPropertyView, final int depth, final Set<Integer> visitedObjects) throws IOException {

			int hashCode = -1;

			// mark object as visited
			if (source != null) {

				hashCode = source.hashCode();
				visitedObjects.add(hashCode);

				writer.beginObject(source);

				// prevent endless recursion by pruning at depth n
				if (depth <= outputNestingDepth) {

					// property keys
					Iterable<PropertyKey> keys = source.getPropertyKeys(localPropertyView);
					if (keys != null) {

						// speciality for all, custom and ui view: limit recursive rendering to (id, name)
						if (compactNestedProperties && depth > 0 && restrictedViews.contains(localPropertyView)) {
							keys = idTypeNameOnly;
						}

						for (final PropertyKey key : keys) {

							final QueryRange range = writer.getSecurityContext().getRange(key.jsonName());
							if (range != null) {
								// Reset count for each key
								range.resetCount();
							}

							// special handling for the internal _graph view: replace name with
							// the name property from the ui view, in case it was overwritten
							PropertyKey localKey = key;

							if (View.INTERNAL_GRAPH_VIEW.equals(localPropertyView)) {

								if (AbstractNode.name.equals(localKey)) {

									// replace key
									localKey = StructrApp.key(source.getClass(), AbstractNode.name.jsonName());
								}
							}

							final Object value = source.getProperty(localKey, range);
							if (value != null) {

								if (!(reduceRedundancy && value instanceof GraphObject && visitedObjects.contains(value.hashCode()))) {

									writer.name(key.jsonName());
									serializeProperty(writer, localKey, value, localPropertyView, depth+1, visitedObjects);
								}

							} else {

								writer.name(localKey.jsonName()).nullValue();
							}
						}
					}
				}

				writer.endObject(source);

				// unmark (visiting only counts for children)
				visitedObjects.remove(hashCode);
			}
		}
	}

	public class IterableSerializer extends Serializer<Iterable> {

		@Override
		public void serialize(final RestWriter parentWriter, final Iterable value, final String localPropertyView, final int depth, final Set<Integer> visitedObjects) throws IOException {

			final Iterator iterator  = value.iterator();
			final Object firstValue  = iterator.hasNext() ? iterator.next() : null;
			final Object secondValue = iterator.hasNext() ? iterator.next() : null;

			if (!wrapSingleResultInArray && depth == 0 && firstValue != null && secondValue == null && !Settings.ForceArrays.getValue()) {

				// prevent endless recursion by pruning at depth n
				if (depth <= outputNestingDepth) {

					serializeRoot(parentWriter, firstValue, localPropertyView, depth, visitedObjects);
				}

			} else {

				parentWriter.beginArray();

				// prevent endless recursion by pruning at depth n
				if (depth <= outputNestingDepth) {

					// first value?
					if (firstValue != null) {
						serializeRoot(parentWriter, firstValue, localPropertyView, depth, visitedObjects);
					}

					// second value?
					if (secondValue != null) {

						serializeRoot(parentWriter, secondValue, localPropertyView, depth, visitedObjects);

						// more values?
						while (iterator.hasNext()) {

							serializeRoot(parentWriter, iterator.next(), localPropertyView, depth, visitedObjects);
						}
					}
				}

				parentWriter.endArray();
			}
		}
	}

	public class MapSerializer extends Serializer<Map<String, Object>> {

		@Override
		public void serialize(final RestWriter writer, final Map<String, Object> source, final String localPropertyView, final int depth, final Set<Integer> visitedObjects) throws IOException {

			writer.beginObject();

			// prevent endless recursion by pruning at depth n
			if (depth <= outputNestingDepth) {

				for (Map.Entry<String, Object> entry : source.entrySet()) {

					String key   = getString(entry.getKey());
					Object value = entry.getValue();

					writer.name(key);
					serializeRoot(writer, value, localPropertyView, depth+1, visitedObjects);
				}
			}

			writer.endObject();
		}
	}

	public class PropertyMapSerializer extends Serializer<PropertyMap> {

		public PropertyMapSerializer() {}

		@Override
		public void serialize(final RestWriter writer, final PropertyMap source, final String localPropertyView, final int depth, final Set<Integer> visitedObjects) throws IOException {

			writer.beginObject();

			// prevent endless recursion by pruning at depth n
			if (depth <= outputNestingDepth) {

				for (Map.Entry<PropertyKey, Object> entry : source.entrySet()) {

					final PropertyKey key   = entry.getKey();
					final Object value      = entry.getValue();

					writer.name(key.jsonName());
					serializeProperty(writer, key, value, localPropertyView, depth+1, visitedObjects);
				}
			}

			writer.endObject();
		}
	}

	// ----- private methods -----
	private void doParallel(final List list, final RestWriter parentWriter, final Set<Integer> visitedObjects, final Operation op) {

		final SecurityContext securityContext = parentWriter.getSecurityContext();
		final int numberOfPartitions          = (int)Math.rint(Math.log(list.size())) + 1;
		final List<List> partitions           = ListUtils.partition(list, numberOfPartitions);
		final List<Future<String>> futures    = new LinkedList<>();

		for (final List partition : partitions) {

			futures.add(threadPool.submit(() -> {

				final StringWriter buffer = new StringWriter();

				// avoid deadlocks by preventing writes in this transaction
				securityContext.setReadOnlyTransaction();

				try (final Tx tx = StructrApp.getInstance(securityContext).tx(false, false, false)) {

					final RestWriter bufferingRestWriter = getRestWriter(securityContext, buffer);
					final Set<Integer> nestedObjects     = new LinkedHashSet<>(visitedObjects);
					configureWriter(bufferingRestWriter);

					bufferingRestWriter.beginArray();

					for (final Object o : partition) {

						op.run(bufferingRestWriter, o, nestedObjects);
					}

					bufferingRestWriter.endArray();
					bufferingRestWriter.flush();

					tx.success();
				}

				final String data = buffer.toString();
				final String sub  = data.substring(1, data.length() - 1);

				return sub;

			}));
		}

		for (final Iterator<Future<String>> it = futures.iterator(); it.hasNext();) {

			try {

				final Future<String> future = it.next();
				final String raw            = future.get();

				parentWriter.raw(raw);

				if (it.hasNext()) {
					parentWriter.raw(",");
				}

			} catch (Throwable t) {

				t.printStackTrace();
			}
		}
	}

	private String getString(final Object value) {
		
		if (value != null) {
			return value.toString();
		}

		throw new NullPointerException();
	}

	private interface Operation {

		public void run(final RestWriter writer, final Object o, final Set<Integer> visitedObjects) throws IOException;
	}
}
