/*
 * Copyright (c) 2019 optile GmbH
 * https://www.optile.net
 *
 * This file is open source and available under the MIT license.
 * See the LICENSE file for more information.
 */

package net.optile.payment.ui.model;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.text.TextUtils;
import net.optile.payment.core.LanguageFile;
import net.optile.payment.model.ApplicableNetwork;
import net.optile.payment.model.InputElement;
import net.optile.payment.util.PaymentUtils;

/**
 * Class for holding the ApplicableNetwork with its localized language file
 */
public class PaymentNetwork {

    public final ApplicableNetwork network;
    private LanguageFile lang;
    private String smartSelectionRegex;

    public PaymentNetwork(ApplicableNetwork network) {
        this.network = network;
    }

    public URL getLink(String name) {
        Map<String, URL> links = network.getLinks();
        return links != null ? links.get(name) : null;
    }

    public String getPaymentMethod() {
        return network.getMethod();
    }

    public String getCode() {
        return network.getCode();
    }

    public String getLabel() {
        return network.getLabel();
    }

    public String getRecurrence() {
        return network.getRecurrence();
    }

    public String getRegistration() {
        return network.getRegistration();
    }

    public boolean isPreselected() {
        return PaymentUtils.isTrue(network.getSelected());
    }

    public List<InputElement> getInputElements() {
        List<InputElement> elements = network.getLocalizedInputElements();
        return elements == null ? new ArrayList<>() : elements;
    }

    public String getButton() {
        return network.getButton();
    }

    public LanguageFile getLang() {
        return lang;
    }

    public void setLang(LanguageFile lang) {
        this.lang = lang;
    }

    public void setSmartSelectionRegex(String regex) {
        this.smartSelectionRegex = regex;
    }

    public boolean compare(PaymentNetwork network) {
        List<InputElement> srcItems = getInputElements();
        List<InputElement> cmpItems = network.getInputElements();

        if (srcItems.size() != cmpItems.size()) {
            return false;
        }
        InputElement srcItem;
        InputElement cmpItem;

        for (int i = 0, e = srcItems.size(); i < e; i++) {
            srcItem = srcItems.get(i);
            cmpItem = cmpItems.get(i);

            if (!(srcItem.getName().equals(cmpItem.getName()) && srcItem.getType().equals(cmpItem.getType()))) {
                return false;
            }
        }
        return true;
    }

    boolean validateSmartSelected(String text) {
        if (text == null || TextUtils.isEmpty(this.smartSelectionRegex)) {
            return false;
        }
        return text.matches(smartSelectionRegex);
    }
}
