/*
 * Copyright (c) 2019 optile GmbH
 * https://www.optile.net
 *
 * This file is open source and available under the MIT license.
 * See the LICENSE file for more information.
 */

package net.optile.payment.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class IbanValidatorTest {

    @Test
    public void isValidIban() {
        assertTrue(IbanValidator.isValidIban("DE27100777770209299700"));
        assertTrue(IbanValidator.isValidIban("AT022050302101023600"));

        assertFalse(IbanValidator.isValidIban(null));
        assertFalse(IbanValidator.isValidIban(""));
        assertFalse(IbanValidator.isValidIban("AT1234567890123456"));
    }
}
