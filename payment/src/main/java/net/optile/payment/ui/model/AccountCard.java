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

import net.optile.payment.core.LanguageFile;
import net.optile.payment.model.AccountMask;
import net.optile.payment.model.AccountRegistration;
import net.optile.payment.model.ApplicableNetwork;
import net.optile.payment.model.InputElement;
import net.optile.payment.util.PaymentUtils;

/**
 * Class for holding the data of a AccountCard in the list
 */
public final class AccountCard implements PaymentCard {

    private final AccountRegistration account;
    private final ApplicableNetwork network;
    private LanguageFile lang;

    public AccountCard(AccountRegistration account, ApplicableNetwork network) {
        this.account = account;
        this.network = network;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URL getOperationLink() {
        return getLink("operation");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPaymentMethod() {
        return network.getMethod();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCode() {
        return account.getCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LanguageFile getLang() {
        return lang;
    }

    public void setLang(LanguageFile lang) {
        this.lang = lang;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<InputElement> getInputElements() {
        List<InputElement> elements = account.getLocalizedInputElements();
        return elements == null ? new ArrayList<>() : elements;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputElement getInputElement(String name) {
        for (InputElement element : getInputElements()) {
            if (element.getName().equals(name)) {
                return element;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isPreselected() {
        return PaymentUtils.isTrue(account.getSelected());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getButton() {
        return network.getButton();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onTextInputChanged(String type, String text) {
        return false;
    }

    public URL getLink(String name) {
        Map<String, URL> links = account.getLinks();
        return links != null ? links.get(name) : null;
    }

    public AccountMask getMaskedAccount() {
        return account.getMaskedAccount();
    }
}
