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
package org.structr.payment.entity;

import org.structr.core.entity.AbstractNode;
import org.structr.schema.SchemaService;

/**
 *
 */
public class PaymentItemNodeMixin extends AbstractNode implements PaymentItemNode {

	static {

		SchemaService.registerMixinType("PaymentItemNode", AbstractNode.class, PaymentItemNodeMixin.class);
	}

	@Override
	public int getAmount() {
		return getProperty(amount);
	}

	@Override
	public int getQuantity() {
		return getProperty(quantity);
	}

	@Override
	public String getDescription() {
		return getProperty(description);
	}

	@Override
	public String getItemNumber() {
		return getProperty(number);
	}

	@Override
	public String getItemUrl() {
		return getProperty(url);
	}
}
