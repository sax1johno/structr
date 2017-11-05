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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.payment.entity;

import java.util.List;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.EndNodes;
import org.structr.core.property.EnumProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.payment.api.Payment;
import org.structr.payment.api.PaymentState;

/**
 *
 */
public interface PaymentNode extends NodeInterface, Payment {

	public static final Property<List<PaymentItemNode>> items                   = new EndNodes<>("items", PaymentItems.class);
	public static final Property<PaymentState>          stateProperty           = new EnumProperty("state", PaymentState.class).indexed();
	public static final Property<String>                descriptionProperty     = new StringProperty("description").indexed();
	public static final Property<String>                currencyProperty        = new StringProperty("currency").indexed();
	public static final Property<String>                tokenProperty           = new StringProperty("token").indexed();
	public static final Property<String>                billingAgreementId      = new StringProperty("billingAgreementId");
	public static final Property<String>                note                    = new StringProperty("note");
	public static final Property<String>                billingAddressName      = new StringProperty("billingAddressName");
	public static final Property<String>                billingAddressStreet1   = new StringProperty("billingAddressStreet1");
	public static final Property<String>                billingAddressStreet2   = new StringProperty("billingAddressStreet2");
	public static final Property<String>                billingAddressZip       = new StringProperty("billingAddressZip");
	public static final Property<String>                billingAddressCity      = new StringProperty("billingAddressCity");
	public static final Property<String>                billingAddressCountry   = new StringProperty("billingAddressCountry");
	public static final Property<String>                invoiceId               = new StringProperty("invoiceId");
	public static final Property<String>                payerAddressName        = new StringProperty("payerAddressName");
	public static final Property<String>                payerAddressStreet1     = new StringProperty("payerAddressStreet1");
	public static final Property<String>                payerAddressStreet2     = new StringProperty("payerAddressStreet2");
	public static final Property<String>                payerAddressZip         = new StringProperty("payerAddressZip");
	public static final Property<String>                payerAddressCity        = new StringProperty("payerAddressCity");
	public static final Property<String>                payerAddressCountry     = new StringProperty("payerAddressCountry");
	public static final Property<String>                payer                   = new StringProperty("payer");
	public static final Property<String>                payerBusiness           = new StringProperty("payerBusiness");

	public static final View publicView = new View(PaymentNode.class, "public",
		descriptionProperty, items, currencyProperty, tokenProperty, stateProperty, billingAgreementId, note, billingAddressName,
		billingAddressStreet1, billingAddressStreet2, billingAddressZip, billingAddressCity, billingAddressCountry, invoiceId,
		payerAddressName, payerAddressStreet1, payerAddressStreet2, payerAddressZip, payerAddressCity, payerAddressCountry, payer, payerBusiness
	);

	public static final View uiView = new View(PaymentNode.class, "ui",
		descriptionProperty, items, currencyProperty, tokenProperty, stateProperty, billingAgreementId, note, billingAddressName,
		billingAddressStreet1, billingAddressStreet2, billingAddressZip, billingAddressCity, billingAddressCountry, invoiceId,
		payerAddressName, payerAddressStreet1, payerAddressStreet2, payerAddressZip, payerAddressCity, payerAddressCountry, payer, payerBusiness
	);

	GraphObject beginCheckout(final String providerName, final String successUrl, final String cancelUrl) throws FrameworkException;
	void cancelCheckout(final String providerName, final String token) throws FrameworkException;
	GraphObject confirmCheckout(final String providerName, final String notifyUrl, final String token, final String payerId) throws FrameworkException;
}
