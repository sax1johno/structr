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

import java.util.LinkedList;
import java.util.List;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.EndNodes;
import org.structr.core.property.EnumProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.payment.api.APIError;
import org.structr.payment.api.APIResponse;
import org.structr.payment.api.BeginCheckoutResponse;
import org.structr.payment.api.CheckoutState;
import org.structr.payment.api.ConfirmCheckoutResponse;
import org.structr.payment.api.Payment;
import org.structr.payment.api.PaymentItem;
import org.structr.payment.api.PaymentProvider;
import org.structr.payment.api.PaymentState;
import org.structr.payment.impl.paypal.PayPalErrorToken;
import org.structr.payment.impl.paypal.PayPalPaymentProvider;
import org.structr.payment.impl.stripe.StripePaymentProvider;
import org.structr.schema.SchemaService;

/**
 *
 */
public interface PaymentNode extends NodeInterface, Payment {

	static class Impl { static { SchemaService.registerMixinType(PaymentNode.class); }}

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

	@Export
	default GraphObject beginCheckout(final String providerName, final String successUrl, final String cancelUrl) throws FrameworkException {

		final PaymentProvider provider = getPaymentProvider(providerName);
		if (provider != null) {

			final BeginCheckoutResponse response = provider.beginCheckout(this, successUrl, cancelUrl);
			if (CheckoutState.Success.equals(response.getCheckoutState())) {

				final GraphObjectMap data  = new GraphObjectMap();

				data.put(tokenProperty, response.getToken());

				return data;

			} else {

				throwErrors("Unable to begin checkout.", response);
			}

		} else {

			throw new FrameworkException(422, "Payment provider " + providerName + " not found.");
		}

		return null;
	}

	@Export
	default void cancelCheckout(final String providerName, final String token) throws FrameworkException {

		final PaymentProvider provider = getPaymentProvider(providerName);
		if (provider != null) {

			provider.cancelCheckout(this);

		} else {

			throw new FrameworkException(422, "Payment provider " + providerName + " not found");
		}
	}

	@Export
	default GraphObject confirmCheckout(final String providerName, final String notifyUrl, final String token, final String payerId) throws FrameworkException {

		final PaymentProvider provider = getPaymentProvider(providerName);
		if (provider != null) {

			final ConfirmCheckoutResponse response = provider.confirmCheckout(this, notifyUrl, token, payerId);
			if (CheckoutState.Success.equals(response.getCheckoutState())) {

				// no return value neccessary, will result in code 200
				return null;

			} else {

				throwErrors("Unable to confirm checkout", response);
			}

		} else {

			throw new FrameworkException(422, "Payment provider " + providerName + " not found");
		}

		return null;
	}

	// ----- interface Payment -----
	@Override
	default List<PaymentItem> getItems() {
		return new LinkedList<>(getProperty(items));
	}

	@Override
	default int getTotal() {

		int total = 0;

		for (final PaymentItem item : getItems()) {
			total += item.getAmount() * item.getQuantity();
		}

		return total;
	}

	@Override
	default String getCurrencyCode() {
		return getProperty(currencyProperty);
	}

	@Override
	default String getToken() {
		return getProperty(tokenProperty);
	}

	@Override
	default void setToken(String token) throws FrameworkException {
		setProperty(tokenProperty, token);
	}

	default PaymentProvider getPaymentProvider(final String providerName) {

		switch (providerName) {

			case "paypal":
				return new PayPalPaymentProvider();

			case "stripe":
				return new StripePaymentProvider();
		}

		return null;
	}

	default void throwErrors(final String cause, final APIResponse response) throws FrameworkException {

		final ErrorBuffer errorBuffer = new ErrorBuffer();

		for (final APIError error : response.getErrors()) {

			errorBuffer.add(new PayPalErrorToken(PaymentNode.class.getSimpleName(), AbstractNode.base, error.getErrorCode(), error.getLongMessage()));
		}

		throw new FrameworkException(422, cause, errorBuffer);
	}

	@Override
	default String getDescription() {
		return getProperty(descriptionProperty);
	}

	@Override
	default void setDescription(final String description) throws FrameworkException {
		setProperty(descriptionProperty, description);
	}

	@Override
	default PaymentState getPaymentState() {
		return getProperty(stateProperty);
	}

	@Override
	default void setPaymentState(final PaymentState state) throws FrameworkException {
		setProperty(stateProperty, state);
	}

	@Override
	default String getBillingAddressName() {
		return getProperty(billingAddressName);
	}

	@Override
	default void setBillingAddressName(final String billingAddressName) throws FrameworkException {
		setProperty(PaymentNode.billingAddressName, billingAddressName);
	}

	@Override
	default String getBillingAddressStreet1() {
		return getProperty(PaymentNode.billingAddressStreet1);
	}

	@Override
	default void setBillingAddressStreet1(final String billingAddressStreet1) throws FrameworkException {
		setProperty(PaymentNode.billingAddressStreet1, billingAddressStreet1);
	}

	@Override
	default String getBillingAddressStreet2() {
		return getProperty(PaymentNode.billingAddressStreet2);
	}

	@Override
	default void setBillingAddressStreet2(final String billingAddressStreet2) throws FrameworkException {
			setProperty(PaymentNode.billingAddressStreet2, billingAddressStreet2);
	}

	@Override
	default String getBillingAddressZip() {
		return getProperty(PaymentNode.billingAddressZip);
	}

	@Override
	default void setBillingAddressZip(final String billingAddressZip) throws FrameworkException {
		setProperty(PaymentNode.billingAddressZip, billingAddressZip);
	}

	@Override
	default String getBillingAddressCity() {
		return getProperty(PaymentNode.billingAddressCity);
	}

	@Override
	default void setBillingAddressCity(final String billingAddressCity) throws FrameworkException {
		setProperty(PaymentNode.billingAddressCity, billingAddressCity);
	}

	@Override
	default String getBillingAddressCountry() {
		return getProperty(PaymentNode.billingAddressCountry);
	}

	@Override
	default void setBillingAddressCountry(final String billingAddressCountry) throws FrameworkException {
		setProperty(PaymentNode.billingAddressCountry, billingAddressCountry);
	}

	@Override
	default String getPayer() {
		return getProperty(PaymentNode.payer);
	}

	@Override
	default void setPayer(final String payer) throws FrameworkException {
			setProperty(PaymentNode.payer, payer);
	}

	@Override
	default String getPayerBusiness() {
		return getProperty(PaymentNode.payerBusiness);
	}

	@Override
	default void setPayerBusiness(final String payerBusiness) throws FrameworkException {
		setProperty(PaymentNode.payerBusiness, payerBusiness);
	}

	@Override
	default String getPayerAddressName() {
		return getProperty(PaymentNode.payerAddressName);
	}

	@Override
	default void setPayerAddressName(final String payerAddressName) throws FrameworkException {
			setProperty(PaymentNode.payerAddressName, payerAddressName);
	}

	@Override
	default String getPayerAddressStreet1() {
		return getProperty(PaymentNode.payerAddressStreet1);
	}

	@Override
	default void setPayerAddressStreet1(final String payerAddressStreet1) throws FrameworkException {
			setProperty(PaymentNode.payerAddressStreet1, payerAddressStreet1);
	}

	@Override
	default String getPayerAddressStreet2() {
		return getProperty(PaymentNode.payerAddressStreet2);
	}

	@Override
	default void setPayerAddressStreet2(final String payerAddressStreet2) throws FrameworkException {
			setProperty(PaymentNode.payerAddressStreet2, payerAddressStreet2);
	}

	@Override
	default String getPayerAddressZip() {
		return getProperty(PaymentNode.payerAddressZip);
	}

	@Override
	default void setPayerAddressZip(final String payerAddressZip) throws FrameworkException {
			setProperty(PaymentNode.payerAddressZip, payerAddressZip);
	}

	@Override
	default String getPayerAddressCity() {
		return getProperty(PaymentNode.payerAddressCity);
	}

	@Override
	default void setPayerAddressCity(final String payerAddressCity) throws FrameworkException {
			setProperty(PaymentNode.payerAddressCity, payerAddressCity);
	}

	@Override
	default String getPayerAddressCountry() {
		return getProperty(PaymentNode.payerAddressCountry);
	}

	@Override
	default void setPayerAddressCountry(final String payerAddressCountry) throws FrameworkException {
		setProperty(PaymentNode.payerAddressCountry, payerAddressCountry);
	}

	@Override
	default String getBillingAgreementId() {
		return getProperty(PaymentNode.billingAgreementId);
	}

	@Override
	default void setBillingAgreementId(final String billingAgreementId) throws FrameworkException {
		setProperty(PaymentNode.billingAgreementId, billingAgreementId);
	}

	@Override
	default String getNote() {
		return getProperty(PaymentNode.note);
	}

	@Override
	default void setNote(final String note) throws FrameworkException {
		setProperty(PaymentNode.note, note);
	}

	@Override
	default String getInvoiceId() {
		return getProperty(PaymentNode.invoiceId);
	}

	@Override
	default void setInvoiceId(final String invoiceId) throws FrameworkException {
		setProperty(PaymentNode.invoiceId, invoiceId);
	}
}
