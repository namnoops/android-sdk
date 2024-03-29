/*
 * Copyright (c) 2019 optile GmbH
 * https://www.optile.net
 *
 * This file is open source and available under the MIT license.
 * See the LICENSE file for more information.
 */

package net.optile.payment.model;

import java.net.URL;

/**
 * Form data to pre-fill network form. Not all data could be provided- it depends what data we know already and what network should been used.
 */
public class FormData {
    /** account-related data to pre-fill a form */
    private AccountFormData account;
    /** customer-related data to pre-fill a form */
    private CustomerFormData customer;
    /** installments plans data */
    private Installments installments;
    /** An URL to the data privacy consent document */
    private URL dataPrivacyConsentUrl;

    /**
     * Gets account-related data to pre-fill a form.
     *
     * @return Account -related form data.
     */
    public AccountFormData getAccount() {
        return account;
    }

    /**
     * Sets account-related data to pre-fill a form.
     *
     * @param account Account-related form data.
     */
    public void setAccount(final AccountFormData account) {
        this.account = account;
    }

    /**
     * Gets installments data.
     *
     * @return Installments data.
     */
    public Installments getInstallments() {
        return installments;
    }

    /**
     * Sets installments data.
     *
     * @param installments Installments data.
     */
    public void setInstallments(final Installments installments) {
        this.installments = installments;
    }

    /**
     * Gets customer-related data to pre-fill a form.
     *
     * @return Customer -related data to pre-fill a form.
     */
    public CustomerFormData getCustomer() {
        return customer;
    }

    /**
     * Sets customer-related data to pre-fill a form.
     *
     * @param customer Customer-related data to pre-fill a form.
     */
    public void setCustomer(final CustomerFormData customer) {
        this.customer = customer;
    }

    /**
     * Gets an URL to the data privacy consent document.
     *
     * @return URL object.
     */
    public URL getDataPrivacyConsentUrl() {
        return dataPrivacyConsentUrl;
    }

    /**
     * Sets an URL to the data privacy consent document.
     *
     * @param dataPrivacyConsentUrl URL object.
     */
    public void setDataPrivacyConsentUrl(final URL dataPrivacyConsentUrl) {
        this.dataPrivacyConsentUrl = dataPrivacyConsentUrl;
    }
}
