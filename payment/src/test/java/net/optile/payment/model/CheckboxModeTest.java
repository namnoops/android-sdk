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
 * The CheckboxMode test
 */
public class CheckboxModeTest {

    @Test
    public void isCheckboxMode_invalidValue_false() {
        assertFalse(CheckboxMode.isValid("foo"));
    }

    @Test
    public void isCheckboxMode_validValue_true() {
        assertTrue(CheckboxMode.isValid(CheckboxMode.OPTIONAL));
        assertTrue(CheckboxMode.isValid(CheckboxMode.OPTIONAL_PRESELECTED));
        assertTrue(CheckboxMode.isValid(CheckboxMode.REQUIRED));
        assertTrue(CheckboxMode.isValid(CheckboxMode.REQUIRED_PRESELECTED));
        assertTrue(CheckboxMode.isValid(CheckboxMode.FORCED));
        assertTrue(CheckboxMode.isValid(CheckboxMode.FORCED_DISPLAYED));
    }
}
