/*
 * Copyright (c) 2019 optile GmbH
 * https://www.optile.net
 *
 * This file is open source and available under the MIT license.
 * See the LICENSE file for more information.
 */

package net.optile.payment.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The PaymentMethod test
 */
public class PaymentMethodTest {

    @Test
    public void isPaymentMethod_invalidValue_false() {
        assertFalse(PaymentMethod.isValid("foo"));
    }

    @Test
    public void isPaymentMethod_validValue_true() {
        assertTrue(PaymentMethod.isValid(PaymentMethod.BANK_TRANSFER));
        assertTrue(PaymentMethod.isValid(PaymentMethod.BILLING_PROVIDER));
        assertTrue(PaymentMethod.isValid(PaymentMethod.CASH_ON_DELIVERY));
        assertTrue(PaymentMethod.isValid(PaymentMethod.CHECK_PAYMENT));
        assertTrue(PaymentMethod.isValid(PaymentMethod.CREDIT_CARD));
        assertTrue(PaymentMethod.isValid(PaymentMethod.DEBIT_CARD));
        assertTrue(PaymentMethod.isValid(PaymentMethod.DIRECT_DEBIT));
        assertTrue(PaymentMethod.isValid(PaymentMethod.ELECTRONIC_INVOICE));
        assertTrue(PaymentMethod.isValid(PaymentMethod.GIFT_CARD));
        assertTrue(PaymentMethod.isValid(PaymentMethod.MOBILE_PAYMENT));
        assertTrue(PaymentMethod.isValid(PaymentMethod.ONLINE_BANK_TRANSFER));
        assertTrue(PaymentMethod.isValid(PaymentMethod.OPEN_INVOICE));
        assertTrue(PaymentMethod.isValid(PaymentMethod.PREPAID_CARD));
        assertTrue(PaymentMethod.isValid(PaymentMethod.TERMINAL));
        assertTrue(PaymentMethod.isValid(PaymentMethod.WALLET));
    }
}
