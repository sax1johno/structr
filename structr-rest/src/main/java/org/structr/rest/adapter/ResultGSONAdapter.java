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
package org.structr.rest.adapter;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Iterator;
import java.util.Locale;
import org.structr.api.QueryResult;
import org.structr.core.GraphObject;
import org.structr.core.Value;
import org.structr.core.rest.GraphObjectGSONAdapter;

/**
 * Controls deserialization of property sets.
 *
 *
 */
public class ResultGSONAdapter implements JsonSerializer<QueryResult>, JsonDeserializer<QueryResult> {

	private final DecimalFormat decimalFormat             = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
	private GraphObjectGSONAdapter graphObjectGsonAdapter = null;

	public ResultGSONAdapter(Value<String> propertyView, final int outputNestingDepth) {
		this.graphObjectGsonAdapter = new GraphObjectGSONAdapter(propertyView, outputNestingDepth);
	}

	@Override
	public JsonElement serialize(QueryResult src, Type typeOfSrc, JsonSerializationContext context) {

		long t0 = System.nanoTime();

		JsonObject result = new JsonObject();

		final Integer size               = (Integer)src.getMetaData("size");
		final Integer page               = (Integer)src.getMetaData("page");
		final Integer pageCount          = (Integer)src.getMetaData("pageCount");
		final Integer pageSize           = (Integer)src.getMetaData("pageSize");
		final String queryTime           = (String)src.getMetaData("queryTime");
		final String searchString        = (String)src.getMetaData("searchString");
		final String sortKey             = (String)src.getMetaData("sortKey");
		final String sortOrder           = (String)src.getMetaData("sortOrder");
		final GraphObject metaData       = (GraphObject)src.getMetaData("metaData");
		final Boolean isCollection       = (Boolean)src.getMetaData("isCollection");
		final Boolean isPrimitiveArray   = (Boolean)src.getMetaData("isPrimitiveArray");
		final Boolean hasNonGraphResult  = (Boolean)src.getMetaData("hasNonGraphResult");

		if(page != null) {
			result.add("page", new JsonPrimitive(page));
		}

		if(pageCount != null) {
			result.add("page_count", new JsonPrimitive(pageCount));
		}

		if(pageSize != null) {
			result.add("page_size", new JsonPrimitive(pageSize));
		}

		if(queryTime != null) {
			result.add("query_time", new JsonPrimitive(queryTime));
		}

		if(size != null) {
			result.add("result_count", new JsonPrimitive(size));
		}

		final Iterator iterator = src.iterator();

		if(Boolean.TRUE.equals(hasNonGraphResult)) {

			if (iterator.hasNext()) {

				final Object nonGraphObjectResult = iterator.next();

				result.add("result", graphObjectGsonAdapter.serializeObject(nonGraphObjectResult, System.currentTimeMillis()));


			} else {

				result.add("result", new JsonArray());
			}

		} else if (isPrimitiveArray) {

			JsonArray resultArray = new JsonArray();

			while (iterator.hasNext()) {

				final GraphObject graphObject = (GraphObject)iterator.next();
				final Object value            = graphObject.getProperty(GraphObject.id);

				if (value != null) {

					resultArray.add(new JsonPrimitive(value.toString()));
				}
			}

			result.add("result", resultArray);


		} else {

			// keep track of serialization time
			long startTime = System.currentTimeMillis();

			if(isCollection) {

				// serialize list of results
				JsonArray resultArray = new JsonArray();
				while (iterator.hasNext()) {

					final GraphObject graphObject = (GraphObject)iterator.next();
					JsonElement element           = graphObjectGsonAdapter.serialize(graphObject, startTime);

					if (element != null) {

						resultArray.add(element);

					} else {

						// stop serialization if timeout occurs
						result.add("status", new JsonPrimitive("Serialization aborted due to timeout"));
						src.setMetaData("hasPartialContent", true);

						break;
					}
				}

				result.add("result", resultArray);

			} else {

				// use GraphObject adapter to serialize single result
				result.add("result", graphObjectGsonAdapter.serialize((GraphObject)iterator.next(), startTime));
			}
		}

		if(searchString != null) {
			result.add("search_string", new JsonPrimitive(searchString));
		}

		if(sortKey != null) {
			result.add("sort_key", new JsonPrimitive(sortKey));
		}

		if(sortOrder != null) {
			result.add("sort_order", new JsonPrimitive(sortOrder));
		}

		if (metaData != null) {

			JsonElement element = graphObjectGsonAdapter.serialize(metaData, System.currentTimeMillis());
			if (element != null) {

				result.add("meta_data", element);
			}
		}

		result.add("serialization_time", new JsonPrimitive(decimalFormat.format((System.nanoTime() - t0) / 1000000000.0)));

		return result;
	}

	@Override
	public QueryResult deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		return null;
	}

	public GraphObjectGSONAdapter getGraphObjectGSONAdapter() {
		return graphObjectGsonAdapter;
	}
}
