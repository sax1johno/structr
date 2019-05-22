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
package org.structr.core.script;

import org.mozilla.javascript.Script;

/**
 * A small piece of JavaScript code that can either be
 * compiled or run directly.
 */
public class Snippet {

	private Script compiled = null;
	private String name     = null;
	private String source   = null;

	public Snippet(final String name, final String source) {

		this.source = source;
		this.name   = name;
	}

	public Snippet(final Script compiled) {
		this.compiled = compiled;
	}

	public Script getCompiledScript() {
		return compiled;
	}

	public String getName() {
		return name;
	}

	public String getSource() {
		return source;
	}
}
