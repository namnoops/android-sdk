/*
 * Copyright (c) 2019 optile GmbH
 * https://www.optile.net
 *
 * This file is open source and available under the MIT license.
 * See the LICENSE file for more information.
 */

package net.optile.payment.validation;

/**
 * Class with helper methods to validate card numbers
 */
final class CardNumberValidator {

    /**
     * Checks validity of credit card based on "Luhn algorithm". This code is taken from <a
     * href="http://en.wikipedia.org/wiki/Luhn_algorithm">Wikipedia article</a>.
     *
     * @param number the credit card number to check.
     * @return <code>true</code> if the number has passed Luhn check, <code>false</code> otherwise.
     */
    public static boolean isValidLuhn(final String number) {

        if (number == null || number.length() == 0) { //check for null - i have no idea what is an initial regEx
            return false;
        }

        final int[][] sumTable = { { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, { 0, 2, 4, 6, 8, 1, 3, 5, 7, 9 } };
        int sum = 0, flip = 0;

        for (int i = number.length() - 1; i >= 0; i--) {
            char posChar = number.charAt(i);
            if (Character.isDigit(posChar)) {
                sum += sumTable[flip++ & 0x1][Character.digit(posChar, 10)];
            } else {
                //character is not a digit - Luhn-ckeck failed
                return false;
            }
        }
        return ((sum % 10) == 0);
    }
}
