/*
 * Copyright (c) 2019 optile GmbH
 * https://www.optile.net
 *
 * This file is open source and available under the MIT license.
 * See the LICENSE file for more information.
 */

package net.optile.payment.core;

import java.util.Properties;

import android.text.TextUtils;
import net.optile.payment.model.Interaction;

/**
 * Class holding the language entries for the payment page, ApplicableNetwork or AccountRegistration
 */
public final class LanguageFile {

    public final static String BUTTON_UPDATE = "update";
    public final static String BUTTON_BACK = "back";
    public final static String TITLE = "title";
    public final static String TEXT = "text";
    private final Properties lang;

    /**
     * Construct an empty LanguageFile
     */
    public LanguageFile() {
        this.lang = new Properties();
    }

    public String translate(String key) {
        return translate(key, null);
    }

    public String translate(String key, String defValue) {
        return key != null ? lang.getProperty(key, defValue) : defValue;
    }

    public String translateInteraction(Interaction interaction) {
        String key = "interaction." + interaction.getCode() + "." + interaction.getReason();
        return translate(key);
    }

    public String getError(String key) {
        return translate("error." + key);
    }

    public String getButtonLabel(String key) {
        return translate("button." + key + ".label");
    }
    
    public String getAccountLabel(String key) {
        return translate("account." + key + ".label");
    }

    public String getAccountHint(String key, String type) {
        return translate("account." + key + ".hint.where." + type);
    }

    public boolean containsAccountHint(String key) {
        String val = getAccountHint(key, TITLE);
        return !TextUtils.isEmpty(val);
    }

    public Properties getProperties() {
        return lang;
    }
}
