/*
 * Copyright (c) 2019 optile GmbH
 * https://www.optile.net
 *
 * This file is open source and available under the MIT license.
 * See the LICENSE file for more information.
 */

package net.optile.payment.model;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import android.support.annotation.StringDef;

/**
 * This class contains the interaction codes of a result.
 */
public class InteractionCode {

    public final static String PROCEED = "PROCEED";
    public final static String ABORT = "ABORT";
    public final static String TRY_OTHER_NETWORK = "TRY_OTHER_NETWORK";
    public final static String TRY_OTHER_ACCOUNT = "TRY_OTHER_ACCOUNT";
    public final static String RETRY = "RETRY";
    public final static String RELOAD = "RELOAD";

    /**
     * Check if the given type is a valid interaction code
     *
     * @param code the interaction code to validate
     * @return true when valid, false otherwise
     */
    public static boolean isValid(final String code) {

        if (code != null) {
            switch (code) {
                case PROCEED:
                case ABORT:
                case TRY_OTHER_NETWORK:
                case TRY_OTHER_ACCOUNT:
                case RETRY:
                case RELOAD:
                    return true;
            }
        }
        return false;
    }

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
        PROCEED,
        ABORT,
        TRY_OTHER_NETWORK,
        TRY_OTHER_ACCOUNT,
        RETRY,
        RELOAD
    })
    public @interface Definition { }
}



