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
package org.structr.api.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import org.structr.api.Predicate;
import org.structr.api.QueryResult;

public class QueryUtils {

	public static <T, C extends Collection<T>> C addAll(C collection, QueryResult<T> iterable) {


		try (final QueryResult<T> tmp = iterable) {

			final Iterator<T> iterator = tmp.iterator();
			while (iterator.hasNext()) {

				final T next = iterator.next();
				if (next != null) {

					collection.add(next);
				}
			}

		} catch (Throwable t) {
			t.printStackTrace();
		}

		return collection;
	}

	public static long count(QueryResult<?> iterable) {

		long c = 0;

		for (Iterator<?> iterator = iterable.iterator(); iterator.hasNext(); iterator.next()) {
			c++;
		}

		return c;
	}

	public static boolean isEmpty(final QueryResult<?> iterable) {
		return !iterable.iterator().hasNext();
	}

	public static <FROM, TO> QueryResult<TO> map(Function<? super FROM, ? extends TO> function, QueryResult<FROM> from) {
		return new MapIterable<>(from, function);
	}

	public static <FROM> QueryResult<FROM> filter(final Predicate<FROM> predicate, final QueryResult<FROM> from) {
		return new FilterIterable<>(from, predicate);
	}

	public static <FROM> QueryResult<FROM> filterNullValues(final QueryResult<FROM> from) {
		return new FilterIterable<>(from, (value) -> value != null);
	}

	public static <T> List<T> toList(QueryResult<T> iterable) {
		return addAll(new ArrayList<T>(100), iterable);
	}

	public static <T> Set<T> toSet(QueryResult<T> iterable) {
		return addAll(new HashSet<T>(), iterable);
	}

	public static <T> QueryResult<T> emptyResult() {
		return new EmptyResult<>();
	}

	public static <T> QueryResult<T> fromList(final List<T> list) {
		return fromList(list, list.size(), true, false);
	}

	public static <T> QueryResult<T> fromList(final List<T> list, final int resultCount, final boolean isCollection, final boolean isPrimitiveArray) {

		final QueryResult<T> listBasedResult = new ListBasedResult<>(list);

		listBasedResult.setMetaData("size",             resultCount);
		listBasedResult.setMetaData("isCollection",     isCollection);
		listBasedResult.setMetaData("isPrimitiveArray", isPrimitiveArray);

		return listBasedResult;
	}

	public static <T> QueryResult<T> single(final T single) {

		final List<T> list = new ArrayList<>();
		list.add(single);

		final QueryResult<T> listBasedResult = new ListBasedResult<>(list);

		listBasedResult.setMetaData("size",             1);
		listBasedResult.setMetaData("isCollection",     false);
		listBasedResult.setMetaData("isPrimitiveArray", false);

		return listBasedResult;
	}

	public static <T> QueryResult<T> emptyList() {
		return new ListBasedResult<>(new ArrayList<>(1000));
	}

	// ----- nested classes -----
	private static class MapIterable<FROM, TO> implements QueryResult<TO> {

		private final QueryResult<FROM> from;
		private final Function<? super FROM, ? extends TO> function;

		public MapIterable(final QueryResult<FROM> from, Function<? super FROM, ? extends TO> function) {
			this.from = from;
			this.function = function;
		}

		@Override
		public void close() {
			from.close();
		}

		@Override
		public Iterator<TO> iterator() {
			return new MapIterator<>(from.iterator(), function);
		}

		@Override
		public void setMetaData(String key, Object value) {
			from.setMetaData(key, value);
		}

		@Override
		public Object getMetaData(String key) {
			return from.getMetaData(key);
		}

		static class MapIterator<FROM, TO> implements Iterator<TO> {

			private final Function<? super FROM, ? extends TO> function;
			private final Iterator<FROM> fromIterator;

			public MapIterator(Iterator<FROM> fromIterator, Function<? super FROM, ? extends TO> function) {

				this.fromIterator = fromIterator;
				this.function     = function;
			}

			@Override
			public boolean hasNext() {
				return fromIterator.hasNext();
			}

			@Override
			public TO next() {

				final FROM from = fromIterator.next();
				return function.apply(from);
			}

			@Override
			public void remove() {
				fromIterator.remove();
			}
		}
	}

	private static class FilterIterable<T> implements QueryResult<T> {

		private final Predicate<? super T> specification;
		private final QueryResult <T> iterable;

		public FilterIterable(QueryResult <T> iterable, Predicate<? super T> specification) {

			this.specification = specification;
			this.iterable      = iterable;
		}

		@Override
		public Iterator<T> iterator() {
			return new FilterIterator<>(iterable.iterator(), specification);
		}

		@Override
		public void close() {
			iterable.close();
		}

		@Override
		public void setMetaData(String key, Object value) {
			iterable.setMetaData(key, value);
		}

		@Override
		public Object getMetaData(String key) {
			return iterable.getMetaData(key);
		}

		static class FilterIterator<T> implements Iterator<T> {

			private final Predicate<? super T> specification;
			private final Iterator<T> iterator;

			private T currentValue;
			boolean finished = false;
			boolean nextConsumed = true;

			public FilterIterator(Iterator<T> iterator, Predicate<? super T> specification) {
				this.specification = specification;
				this.iterator = iterator;
			}

			public boolean moveToNextValid() {

				boolean found = false;

				while (!found && iterator.hasNext()) {

					final T current = iterator.next();

					if (current != null && specification.accept(current)) {

						found             = true;
						this.currentValue = current;
						nextConsumed      = false;
					}
				}

				if (!found) {
					finished = true;
				}

				return found;
			}

			@Override
			public T next() {

				if (!nextConsumed) {

					nextConsumed = true;
					return currentValue;

				} else {

					if (!finished) {

						if (moveToNextValid()) {

							nextConsumed = true;
							return currentValue;
						}
					}
				}

				throw new NoSuchElementException("This iterator is exhausted.");
			}

			@Override
			public boolean hasNext() {
				return !finished && (!nextConsumed || moveToNextValid());
			}

			@Override
			public void remove() {
			}
		}
	}

	private static class EmptyResult<T> implements QueryResult<T> {

		private final Map<String, Object> metadata = new LinkedHashMap<>();

		@Override
		public void close() {
		}

		@Override
		public Iterator<T> iterator() {

			return new Iterator<T>() {

				@Override
				public boolean hasNext() {
					return false;
				}

				@Override
				public T next() {
					throw new NoSuchElementException("Empty iterator.");
				}
			};
		}

		@Override
		public void setMetaData(final String key, final Object value) {
			metadata.put(key, value);
		}

		@Override
		public Object getMetaData(final String key) {
			return metadata.get(key);
		}
	}

	private static class ListBasedResult<T> implements QueryResult<T> {

		private final Map<String, Object> metadata = new LinkedHashMap<>();
		private List<T> list                       = null;

		public ListBasedResult(final List<T> src) {
			this.list = src;
		}

		@Override
		public void close() {
		}

		@Override
		public Iterator<T> iterator() {
			return list.iterator();
		}

		public void add(final T t) {
			list.add(t);
		}

		public List<T> getList() {
			return list;
		}

		@Override
		public void setMetaData(final String key, final Object value) {
			metadata.put(key, value);
		}

		@Override
		public Object getMetaData(final String key) {
			return metadata.get(key);
		}
	}
}
