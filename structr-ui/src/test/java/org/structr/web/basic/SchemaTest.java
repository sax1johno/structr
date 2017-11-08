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
package org.structr.web.basic;

import org.structr.web.StructrUiTest;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.property.StringProperty;


public class SchemaTest extends StructrUiTest {

	@Test
	public void testInheritedValidation01() {

		try (final Tx tx = app.tx()) {

			app.create(StructrApp.getConfiguration().getNodeEntityClass("Group"));

			tx.success();

			fail("Validation of Principal interface was not called.");

		} catch (FrameworkException fex) {
		}
	}

	@Test
	public void testInheritedValidation02() {

		try (final Tx tx = app.tx()) {

			// Create schema node type that extends existing Group interface
			// but adds a validated string property to test the isValid()
			// method.
			app.create(SchemaNode.class,
				new NodeAttribute<>(SchemaNode.name, "ExtendedGroup"),
				new NodeAttribute<>(SchemaNode.extendsClass, "org.structr.dynamic.Group"),
				new NodeAttribute<>(new StringProperty("_test"), "+String!")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			app.create(StructrApp.getConfiguration().getNodeEntityClass("Group"));
			tx.success();

			fail("Validation of Principal interface was not called.");

		} catch (FrameworkException fex) {
		}
	}

	@Test
	public void testUnknowInterfaceError() {

		try (final Tx tx = app.tx()) {

			// Create schema node type that extends existing Group interface
			// but adds a validated string property to test the isValid()
			// method.
			app.create(SchemaNode.class,
				new NodeAttribute<>(SchemaNode.name, "InheritanceTest"),
				new NodeAttribute<>(SchemaNode.implementsInterfaces,
					"org.structr.core.entity.File"
				)
			);

			tx.success();

			fail("Unknown interface should throw an exception.");

		} catch (FrameworkException fex) {
		}
	}

	@Test
	public void testInheritedValidationWithoutValidatedProperty() {

		try (final Tx tx = app.tx()) {

			// Create schema node type that extends existing Group interface
			// but adds a validated string property to test the isValid()
			// method.
			app.create(SchemaNode.class,
				new NodeAttribute<>(SchemaNode.name, "InheritanceTest"),
				new NodeAttribute<>(SchemaNode.implementsInterfaces,
					"org.structr.core.entity.Principal," +
					"org.structr.core.entity.Location," +
					"org.structr.web.entity.File"
				)
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testInheritedValidationWithValidatedProperty() {

		try (final Tx tx = app.tx()) {

			// Create schema node type that extends existing Group interface
			// but adds a validated string property to test the isValid()
			// method.
			app.create(SchemaNode.class,
				new NodeAttribute<>(SchemaNode.name, "InheritanceTest"),
				new NodeAttribute<>(SchemaNode.implementsInterfaces,
					"org.structr.core.entity.Principal," +
					"org.structr.core.entity.Location," +
					"org.structr.web.entity.File"
				),
				new NodeAttribute<>(new StringProperty("_test"), "+String!")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testInheritedValidationWithMethods() {

		try (final Tx tx = app.tx()) {

			// Create schema node type that extends existing Group interface
			// but adds a validated string property to test the isValid()
			// method.
			app.create(SchemaNode.class,
				new NodeAttribute<>(SchemaNode.name, "InheritanceTest"),
				new NodeAttribute<>(SchemaNode.implementsInterfaces,
					"org.structr.core.entity.Principal," +
					"org.structr.core.entity.Location," +
					"org.structr.web.entity.File"
				),
				new NodeAttribute<>(new StringProperty("___onCreate"),  "log('test')"),
				new NodeAttribute<>(new StringProperty("___onSave"),    "log('test')"),
				new NodeAttribute<>(new StringProperty("___onDelete"),  "log('test')")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}
}
