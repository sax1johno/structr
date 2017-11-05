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
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.entity.AbstractNode;
import org.structr.payment.api.APIError;
import org.structr.payment.api.APIResponse;
import org.structr.payment.api.BeginCheckoutResponse;
import org.structr.payment.api.CheckoutState;
import org.structr.payment.api.ConfirmCheckoutResponse;
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
public class PaymentNodeMixin extends AbstractNode implements PaymentNode {

	static {

		SchemaService.registerMixinType("PaymentNode", AbstractNode.class, PaymentNodeMixin.class);
	}

	@Export
	public GraphObject beginCheckout(final String providerName, final String successUrl, final String cancelUrl) throws FrameworkException {

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
	public void cancelCheckout(final String providerName, final String token) throws FrameworkException {

		final PaymentProvider provider = getPaymentProvider(providerName);
		if (provider != null) {

			provider.cancelCheckout(this);

		} else {

			throw new FrameworkException(422, "Payment provider " + providerName + " not found");
		}
	}

	@Export
	public GraphObject confirmCheckout(final String providerName, final String notifyUrl, final String token, final String payerId) throws FrameworkException {

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
	public List<PaymentItem> getItems() {
		return new LinkedList<>(getProperty(items));
	}

	@Override
	public int getTotal() {

		int total = 0;

		for (final PaymentItem item : getItems()) {
			total += item.getAmount() * item.getQuantity();
		}

		return total;
	}

	@Override
	public String getCurrencyCode() {
		return getProperty(currencyProperty);
	}

	@Override
	public String getToken() {
		return getProperty(tokenProperty);
	}

	@Override
	public void setToken(String token) throws FrameworkException {
		setProperty(tokenProperty, token);
	}

	// ----- private methods -----
	private PaymentProvider getPaymentProvider(final String providerName) {

		switch (providerName) {

			case "paypal":
				return new PayPalPaymentProvider();

			case "stripe":
				return new StripePaymentProvider();
		}

		return null;
	}

	private void throwErrors(final String cause, final APIResponse response) throws FrameworkException {

		final ErrorBuffer errorBuffer = new ErrorBuffer();

		for (final APIError error : response.getErrors()) {

			errorBuffer.add(new PayPalErrorToken(PaymentNodeMixin.class.getSimpleName(), AbstractNode.base, error.getErrorCode(), error.getLongMessage()));
		}

		throw new FrameworkException(422, cause, errorBuffer);
	}

	@Override
	public String getDescription() {
		return getProperty(descriptionProperty);
	}

	@Override
	public void setDescription(final String description) throws FrameworkException {
		setProperty(descriptionProperty, description);
	}

	@Override
	public PaymentState getPaymentState() {
		return getProperty(stateProperty);
	}

	@Override
	public void setPaymentState(final PaymentState state) throws FrameworkException {
		setProperty(stateProperty, state);
	}

	@Override
	public String getBillingAddressName() {
		return getProperty(billingAddressName);
	}

	@Override
	public void setBillingAddressName(final String billingAddressName) throws FrameworkException {
		setProperty(PaymentNodeMixin.billingAddressName, billingAddressName);
	}

	@Override
	public String getBillingAddressStreet1() {
		return getProperty(PaymentNodeMixin.billingAddressStreet1);
	}

	@Override
	public void setBillingAddressStreet1(final String billingAddressStreet1) throws FrameworkException {
		setProperty(PaymentNodeMixin.billingAddressStreet1, billingAddressStreet1);
	}

	@Override
	public String getBillingAddressStreet2() {
		return getProperty(PaymentNodeMixin.billingAddressStreet2);
	}

	@Override
	public void setBillingAddressStreet2(final String billingAddressStreet2) throws FrameworkException {
			setProperty(PaymentNodeMixin.billingAddressStreet2, billingAddressStreet2);
	}

	@Override
	public String getBillingAddressZip() {
		return getProperty(PaymentNodeMixin.billingAddressZip);
	}

	@Override
	public void setBillingAddressZip(final String billingAddressZip) throws FrameworkException {
		setProperty(PaymentNodeMixin.billingAddressZip, billingAddressZip);
	}

	@Override
	public String getBillingAddressCity() {
		return getProperty(PaymentNodeMixin.billingAddressCity);
	}

	@Override
	public void setBillingAddressCity(final String billingAddressCity) throws FrameworkException {
		setProperty(PaymentNodeMixin.billingAddressCity, billingAddressCity);
	}

	@Override
	public String getBillingAddressCountry() {
		return getProperty(PaymentNodeMixin.billingAddressCountry);
	}

	@Override
	public void setBillingAddressCountry(final String billingAddressCountry) throws FrameworkException {
		setProperty(PaymentNodeMixin.billingAddressCountry, billingAddressCountry);
	}

	@Override
	public String getPayer() {
		return getProperty(PaymentNodeMixin.payer);
	}

	@Override
	public void setPayer(final String payer) throws FrameworkException {
			setProperty(PaymentNodeMixin.payer, payer);
	}

	@Override
	public String getPayerBusiness() {
		return getProperty(PaymentNodeMixin.payerBusiness);
	}

	@Override
	public void setPayerBusiness(final String payerBusiness) throws FrameworkException {
		setProperty(PaymentNodeMixin.payerBusiness, payerBusiness);
	}

	@Override
	public String getPayerAddressName() {
		return getProperty(PaymentNodeMixin.payerAddressName);
	}

	@Override
	public void setPayerAddressName(final String payerAddressName) throws FrameworkException {
			setProperty(PaymentNodeMixin.payerAddressName, payerAddressName);
	}

	@Override
	public String getPayerAddressStreet1() {
		return getProperty(PaymentNodeMixin.payerAddressStreet1);
	}

	@Override
	public void setPayerAddressStreet1(final String payerAddressStreet1) throws FrameworkException {
			setProperty(PaymentNodeMixin.payerAddressStreet1, payerAddressStreet1);
	}

	@Override
	public String getPayerAddressStreet2() {
		return getProperty(PaymentNodeMixin.payerAddressStreet2);
	}

	@Override
	public void setPayerAddressStreet2(final String payerAddressStreet2) throws FrameworkException {
			setProperty(PaymentNodeMixin.payerAddressStreet2, payerAddressStreet2);
	}

	@Override
	public String getPayerAddressZip() {
		return getProperty(PaymentNodeMixin.payerAddressZip);
	}

	@Override
	public void setPayerAddressZip(final String payerAddressZip) throws FrameworkException {
			setProperty(PaymentNodeMixin.payerAddressZip, payerAddressZip);
	}

	@Override
	public String getPayerAddressCity() {
		return getProperty(PaymentNodeMixin.payerAddressCity);
	}

	@Override
	public void setPayerAddressCity(final String payerAddressCity) throws FrameworkException {
			setProperty(PaymentNodeMixin.payerAddressCity, payerAddressCity);
	}

	@Override
	public String getPayerAddressCountry() {
		return getProperty(PaymentNodeMixin.payerAddressCountry);
	}

	@Override
	public void setPayerAddressCountry(final String payerAddressCountry) throws FrameworkException {
		setProperty(PaymentNodeMixin.payerAddressCountry, payerAddressCountry);
	}

	@Override
	public String getBillingAgreementId() {
		return getProperty(PaymentNodeMixin.billingAgreementId);
	}

	@Override
	public void setBillingAgreementId(final String billingAgreementId) throws FrameworkException {
		setProperty(PaymentNodeMixin.billingAgreementId, billingAgreementId);
	}

	@Override
	public String getNote() {
		return getProperty(PaymentNodeMixin.note);
	}

	@Override
	public void setNote(final String note) throws FrameworkException {
		setProperty(PaymentNodeMixin.note, note);
	}

	@Override
	public String getInvoiceId() {
		return getProperty(PaymentNodeMixin.invoiceId);
	}

	@Override
	public void setInvoiceId(final String invoiceId) throws FrameworkException {
		setProperty(PaymentNodeMixin.invoiceId, invoiceId);
	}
}
