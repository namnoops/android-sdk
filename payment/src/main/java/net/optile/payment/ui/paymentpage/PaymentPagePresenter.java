/*
 * Copyright(c) 2012-2018 optile GmbH. All Rights Reserved.
 * https://www.optile.net
 *
 * This software is the property of optile GmbH. Distribution  of  this
 * software without agreement in writing is strictly prohibited.
 *
 * This software may not be copied, used or distributed unless agreement
 * has been received in full.
 */

package net.optile.payment.ui.paymentpage;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import net.optile.payment.R;
import net.optile.payment.core.PaymentException;
import net.optile.payment.core.PaymentError;
import net.optile.payment.core.PaymentInputType;
import net.optile.payment.core.WorkerSubscriber;
import net.optile.payment.core.WorkerTask;
import net.optile.payment.core.Workers;
import net.optile.payment.form.Charge;
import net.optile.payment.model.ApplicableNetwork;
import net.optile.payment.model.InputElement;
import net.optile.payment.model.Interaction;
import net.optile.payment.model.InteractionCode;
import net.optile.payment.model.ListResult;
import net.optile.payment.model.Networks;
import net.optile.payment.model.OperationResult;
import net.optile.payment.network.ChargeConnection;
import net.optile.payment.network.ListConnection;
import net.optile.payment.ui.PaymentResult;
import net.optile.payment.ui.widget.FormWidget;

/**
 * The PaymentPagePresenter implementing the presenter part of the MVP
 */
final class PaymentPagePresenter {

    private final static String TAG = "pay_PayPresenter";

    private final ListConnection listConnection;
    private final ChargeConnection chargeConnection;
    private final PaymentPageView view;

    private PaymentSession session;
    private int groupType;
    private WorkerTask<OperationResult> chargeTask;
    private WorkerTask<PaymentSession> loadTask;
    private String listUrl;
    private Interaction reloadInteraction;
    
    /**
     * Create a new PaymentPagePresenter
     *
     * @param view The PaymentPageView displaying the payment items
     */
    PaymentPagePresenter(PaymentPageView view) {
        this.view = view;
        this.listConnection = new ListConnection();
        this.chargeConnection = new ChargeConnection();
    }

    void onStop() {
        if (loadTask != null) {
            loadTask.unsubscribe();
            loadTask = null;
        }
        if (chargeTask != null) {
            chargeTask.unsubscribe();
            chargeTask = null;
        }
    }

    /**
     * Load the PaymentSession from the Payment API. once loaded, populate the View with the newly loaded groups of payment methods.
     * If a previous session with the same listUrl is available then reuse the existing one.
     *
     * @param listUrl the url pointing to the ListResult in the Payment API
     */
    void load(String listUrl) {

        if (loadTask != null) {
            return;
        }
        this.listUrl = listUrl;

        if (session != null && session.isListUrl(listUrl)) {
            view.showPaymentSession(session);
        } else {
            view.showLoading(true);
            loadPaymentSession(listUrl);
        }
    }

    /**
     * Make a charge request for the selected PaymentGroup with widgets
     *
     * @param widgets containing the user input data
     * @param group selected group of payment methods
     */
    void charge(Map<String, FormWidget> widgets, PaymentGroup group) {

        if (chargeTask != null) {
            return;
        }
        URL url = group.getLink("operation");
        Charge charge = new Charge();

        try {
            boolean error = false;
            for (FormWidget widget : widgets.values()) {

                if (widget.validate()) {
                    widget.putValue(charge);
                } else {
                    error = true;
                }
            }
            if (!error) {
                view.showLoading(true);
                postChargeRequest(url, charge);
            }
        } catch (PaymentException e) {
            Log.wtf(TAG, e);
            showMessageAndClosePage(R.string.paymentpage_error_unknown, e);
        }
    }

    private void callbackLoadSuccess(PaymentSession session) {
        this.loadTask = null;
        Interaction interaction = session.listResult.getInteraction();
        String resultInfo = session.listResult.getResultInfo();
        String code = interaction.getCode();

        switch (code) {
            case InteractionCode.PROCEED:
                this.session = session;

                if (reloadInteraction != null) {
                    showReloadInteractionMessage();
                    this.reloadInteraction = null;
                }
                break;
            default:
                closePage(false, resultInfo, interaction, null);
        }
    }

    private void callbackLoadError(Throwable error) {
        this.loadTask = null;

        if (error instanceof PaymentException) {
            handleLoadPaymentError((PaymentException) error);
            return;
        }
        Log.wtf(TAG, error);
        closePage(R.string.paymentpage_error_unknown, error);
    }

    private void handleLoadPaymentError(PaymentException exception) {
        PaymentError paymentError = exception.paymentError;

        if (paymentError.errorInfo != null) {
            //closePage(false, paymentError.errorInfo.getResultInfo(), paymentError.errorInfo.getInteraction(), null);
            return;
        }
        switch (paymentError.errorType) {
            case PaymentError.CONN_ERROR:
                //closePage(R.string.paymentpage_error_connerror, paymentError);
                break;
            default:
                //closePage(R.string.paymentpage_error_unknown, paymentError);
        }
        Log.e(TAG, "handlePaymentException: " + paymentError.toString());
    }

    private void callbackChargeSuccess(OperationResult result) {
        this.chargeTask = null;
        Interaction interaction = result.getInteraction();
        String resultInfo = result.getResultInfo();
        String code = interaction.getCode();

        switch (code) {
            case InteractionCode.PROCEED:
                closePage(true, resultInfo, interaction, result);
                break;
            case InteractionCode.TRY_OTHER_NETWORK:
            case InteractionCode.TRY_OTHER_ACCOUNT:
            case InteractionCode.RETRY:
            case InteractionCode.RELOAD:
                reloadPaymentSession(interaction);
                break;
            case InteractionCode.ABORT:
            default:
                closePage(false, resultInfo, interaction, result);
        }
    }

    private void reloadPaymentSession(Interaction interaction) {
        if (!view.isActive()) {
            return;
        }
        this.reloadInteraction = interaction;
        loadPaymentSession(this.listUrl);
    }

    private void callbackChargeError(Throwable error) {
        this.chargeTask = null;

        if (error instanceof PaymentException) {
            handleChargePaymentError((PaymentException) error);
            return;
        }
        //view.showError(R.string.paymentpage_error_unknown);
        Log.wtf(TAG, error);
    }

    private void handleChargePaymentError(PaymentException exception) {
        PaymentError paymentError = exception.paymentError;

        if (paymentError.errorInfo != null) {
            //closePage(false, paymentError.errorInfo.getResultInfo(), paymentError.errorInfo.getInteraction(), null);
            return;
        }
        switch (paymentError.errorType) {
            case PaymentError.CONN_ERROR:
                //view.showError(R.string.paymentpage_error_connerror);
                break;
            default:
                //view.showError(R.string.paymentpage_error_unknown);
        }
        //Log.e(TAG, "handleChargeNetworkError: " + paymentError.toString());
    }

    private void showReloadInteractionMessage() {

        String msg = session.translateInteraction(interaction);
        if (!TextUtils.isEmpty(msg)) {
            //view.showDialog(msg);
        }
    }

    
    private void closePage(int mesgResId, PaymentError paymentError) {
        //PaymentResult result = new PaymentResult(resultInfo, paymentError);
        //view.closePaymentPage(success, result);
    }
    
    private void closePage(boolean success, String resultInfo, Interaction interaction, OperationResult operationResult) {
        //PaymentResult result = new PaymentResult(resultInfo, interaction, operationResult);
        //view.closePaymentPage(success, result);
    }
    
    private int nextGroupType() {
        return groupType++;
    }

    private void loadPaymentSession(final String listUrl) {
        this.session = null;
        view.clear();

        loadTask = WorkerTask.fromCallable(new Callable<PaymentSession>() {
            @Override
            public PaymentSession call() throws PaymentException {
                return asyncLoadPaymentSession(listUrl);
            }
        });
        loadTask.subscribe(new WorkerSubscriber<PaymentSession>() {
            @Override
            public void onSuccess(PaymentSession paymentSession) {
                callbackLoadSuccess(paymentSession);
            }

            @Override
            public void onError(Throwable error) {
                callbackLoadError(error);
            }
        });
        Workers.getInstance().forNetworkTasks().execute(loadTask);
    }

    private PaymentSession asyncLoadPaymentSession(String listUrl) throws PaymentException {
        ListResult listResult = listConnection.getListResult(listUrl);
        List<PaymentItem> items = loadPaymentItems(listResult);
        List<PaymentGroup> groups = new ArrayList<>();

        int selIndex = -1;
        int index = 0;
        for (PaymentItem item : items) {
            PaymentGroup group = createPaymentGroup(item);
            if (selIndex == -1 && group.isSelected()) {
                selIndex = index;
            }
            groups.add(group);
            index++;
        }
        PaymentSession session = new PaymentSession(listResult, groups);
        session.setSelIndex(selIndex);
        session.setLanguage(loadPageLanguage(items));

        Context context = view.getContext();
        if (session.getApplicableNetworkSize() == 0) {
            session.setEmptyMessage(context.getString(R.string.paymentpage_error_empty));
        } else if (groups.size() == 0) {
            session.setEmptyMessage(context.getString(R.string.paymentpage_error_notsupported));
        }
        return session;
    }

    private List<PaymentItem> loadPaymentItems(ListResult listResult) throws PaymentException {
        List<PaymentItem> items = new ArrayList<>();
        Networks nw = listResult.getNetworks();

        if (nw == null) {
            return items;
        }
        List<ApplicableNetwork> an = nw.getApplicable();

        if (an == null || an.size() == 0) {
            return items;
        }
        for (ApplicableNetwork network : an) {
            if (isSupported(network)) {
                items.add(loadPaymentItem(network));
            }
        }
        return items;
    }

    private PaymentItem loadPaymentItem(ApplicableNetwork network) throws PaymentException {
        PaymentItem item = new PaymentItem(network);
        URL langUrl = item.getLink("lang");
        if (langUrl == null) {
            throw createPaymentException("Missing 'lang' link in ApplicableNetwork", null);
        }
        item.setLanguage(listConnection.getLanguage(langUrl, new Properties()));
        return item;
    }

    private PaymentGroup createPaymentGroup(PaymentItem item) {
        PaymentGroup group = new PaymentGroup(nextGroupType(), item, item.getInputElements());
        setExpiryDateSupport(group);
        return group;
    }

    /**
     * Determine if this PaymentGroup should combine the expiryMonth and expiryYear InputElements.
     * Only when the PaymentGroup has expiryMonth, expiryYear and valid expiryDate label the month and year may be combined in one input widget.
     *
     * @param group to set the expiryDate support
     */
    private void setExpiryDateSupport(PaymentGroup group) {
        PaymentItem item = group.getActivePaymentItem();
        String expiryDateLabel = item.translateAccountLabel(PaymentInputType.EXPIRY_DATE);

        boolean hasExpiryMonth = false;
        boolean hasExpiryYear = false;

        for (InputElement element : item.getInputElements()) {
            switch (element.getName()) {
                case PaymentInputType.EXPIRY_MONTH:
                    hasExpiryMonth = true;
                    break;
                case PaymentInputType.EXPIRY_YEAR:
                    hasExpiryYear = true;
            }
        }
        if (!TextUtils.isEmpty(expiryDateLabel) && hasExpiryMonth && hasExpiryYear) {
            group.setHasExpiryDate(true);
            group.setExpiryDateLabel(expiryDateLabel);
            group.setExpiryDateButton(view.getContext().getString(R.string.widget_date_button));
        }
    }

    /**
     * This method loads the payment page language file.
     * The URL for the paymentpage language file is constructed from the URL of one of the ApplicableNetwork entries.
     *
     * @param items contains the list of PaymentItem elements
     * @return the properties object containing the language entries
     */
    private Properties loadPageLanguage(List<PaymentItem> items) throws PaymentException {
        Properties prop = new Properties();

        if (items.size() == 0) {
            return prop;
        }
        PaymentItem item = items.get(0);
        URL langUrl = item.getLink("lang");

        if (langUrl == null) {
            throw createPaymentException("Missing 'lang' link in ApplicableNetwork", null);
        }
        try {
            String newUrl = langUrl.toString().replaceAll(item.getCode(), "paymentpage");
            langUrl = new URL(newUrl);
            return listConnection.getLanguage(langUrl, prop);
        } catch (MalformedURLException e) {
            throw createPaymentException("Malformed language URL", e);
        }
    }

    private boolean isSupported(ApplicableNetwork network) {
        String button = network.getButton();
        return (TextUtils.isEmpty(button) || !button.contains("activate")) && !network.getRedirect();
    }

    private void postChargeRequest(final URL url, final Charge charge) {
        view.showLoading(true);

        chargeTask = WorkerTask.fromCallable(new Callable<OperationResult>() {
            @Override
            public OperationResult call() throws PaymentException {
                return asyncPostChargeRequest(url, charge);
            }
        });
        chargeTask.subscribe(new WorkerSubscriber<OperationResult>() {
            @Override
            public void onSuccess(OperationResult result) {
                callbackChargeSuccess(result);
            }

            @Override
            public void onError(Throwable error) {
                callbackChargeError(error);
            }
        });
        Workers.getInstance().forNetworkTasks().execute(chargeTask);
    }

    private OperationResult asyncPostChargeRequest(URL url, Charge charge) throws PaymentException {
        return chargeConnection.createCharge(url, charge);
    }

    private PaymentException createPaymentException(String message, Throwable cause) {
        final PaymentError error = new PaymentError("PaymentPage", PaymentError.INTERNAL_ERROR, message);
        return new PaymentException(error, message, cause);
    }
}
