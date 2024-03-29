/*
 * Copyright (c) 2019 optile GmbH
 * https://www.optile.net
 *
 * This file is open source and available under the MIT license.
 * See the LICENSE file for more information.
 */

package net.optile.payment.ui.page;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;
import net.optile.payment.core.LanguageFile;
import net.optile.payment.core.PaymentError;
import net.optile.payment.core.PaymentException;
import net.optile.payment.core.WorkerSubscriber;
import net.optile.payment.core.WorkerTask;
import net.optile.payment.core.Workers;
import net.optile.payment.form.Operation;
import net.optile.payment.model.AccountRegistration;
import net.optile.payment.model.ApplicableNetwork;
import net.optile.payment.model.ListResult;
import net.optile.payment.model.Networks;
import net.optile.payment.model.OperationResult;
import net.optile.payment.model.PresetAccount;
import net.optile.payment.network.ListConnection;
import net.optile.payment.network.PaymentConnection;
import net.optile.payment.resource.PaymentGroup;
import net.optile.payment.resource.ResourceLoader;
import net.optile.payment.ui.PaymentUI;
import net.optile.payment.ui.model.AccountCard;
import net.optile.payment.ui.model.NetworkCard;
import net.optile.payment.ui.model.PaymentNetwork;
import net.optile.payment.ui.model.PaymentSession;
import net.optile.payment.ui.model.PresetCard;
import net.optile.payment.validation.Validator;

/**
 * The PaymentPageService providing asynchronize initializing of the PaymentPage and communication with the Payment API .
 * This service makes callbacks in the presenter to notify of request completions.
 */
final class PaymentPageService {

    private final static String TAG = "pay_Service";
    private final PaymentPagePresenter presenter;
    private final ListConnection listConnection;
    private final PaymentConnection paymentConnection;

    private WorkerTask<OperationResult> operationTask;
    private WorkerTask<PaymentSession> loadTask;
    private WorkerTask<Validator> validatorTask;

    /**
     * Create a new PaymentPageService, this service is used to communicate with the Payment API
     */
    PaymentPageService(PaymentPagePresenter presenter) {
        this.presenter = presenter;
        this.listConnection = new ListConnection();
        this.paymentConnection = new PaymentConnection();
    }

    /**
     * Stop and unsubscribe from tasks that are currently active in this service.
     */
    void stop() {

        if (loadTask != null) {
            loadTask.unsubscribe();
            loadTask = null;
        }
        if (operationTask != null) {
            operationTask.unsubscribe();
            operationTask = null;
        }
        if (validatorTask != null) {
            validatorTask.unsubscribe();
            validatorTask = null;
        }
    }

    boolean isPerformingOperation() {
        return operationTask != null && operationTask.isSubscribed();
    }

    boolean isActive() {
        return validatorTask != null || loadTask != null || operationTask != null;
    }

    void loadValidator() {

        if (validatorTask != null) {
            throw new IllegalStateException("Already loading validator, stop first");
        }
        validatorTask = WorkerTask.fromCallable(new Callable<Validator>() {
            @Override
            public Validator call() throws PaymentException {
                return asyncLoadValidator();
            }
        });
        validatorTask.subscribe(new WorkerSubscriber<Validator>() {
            @Override
            public void onSuccess(Validator validator) {
                validatorTask = null;
                presenter.onValidatorSuccess(validator);
            }

            @Override
            public void onError(Throwable cause) {
                validatorTask = null;
                presenter.onValidatorError(cause);
            }
        });
        Workers.getInstance().forNetworkTasks().execute(validatorTask);
    }

    void loadPaymentSession(final String listUrl) {

        if (loadTask != null) {
            throw new IllegalStateException("Already loading payment session, stop first");
        }
        loadTask = WorkerTask.fromCallable(new Callable<PaymentSession>() {
            @Override
            public PaymentSession call() throws PaymentException {
                return asyncLoadPaymentSession(listUrl);
            }
        });
        loadTask.subscribe(new WorkerSubscriber<PaymentSession>() {
            @Override
            public void onSuccess(PaymentSession paymentSession) {
                loadTask = null;
                presenter.onPaymentSessionSuccess(paymentSession);
            }

            @Override
            public void onError(Throwable cause) {
                Log.w(TAG, cause);
                loadTask = null;
                presenter.onPaymentSessionError(cause);
            }
        });
        Workers.getInstance().forNetworkTasks().execute(loadTask);
    }

    void postOperation(final Operation operation) {

        if (operationTask != null) {
            throw new IllegalStateException("Already posting operation, stop first");
        }
        operationTask = WorkerTask.fromCallable(new Callable<OperationResult>() {
            @Override
            public OperationResult call() throws PaymentException {
                return asyncPostOperation(operation);
            }
        });
        operationTask.subscribe(new WorkerSubscriber<OperationResult>() {
            @Override
            public void onSuccess(OperationResult result) {
                operationTask = null;
                presenter.onOperationSuccess(result);
            }

            @Override
            public void onError(Throwable cause) {
                operationTask = null;
                presenter.onOperationError(cause);
            }
        });
        Workers.getInstance().forNetworkTasks().execute(operationTask);
    }

    /**
     * Load the Validator in the background including the validations settings file.
     *
     * @return the validator
     */
    private Validator asyncLoadValidator() throws PaymentException {
        int validationResId = PaymentUI.getInstance().getValidationResId();
        Resources res = presenter.getContext().getResources();
        return new Validator(ResourceLoader.loadValidations(res, validationResId));
    }

    /**
     * Load the PaymentSession from the Payment API
     *
     * @param listUrl unique list url of the payment session
     * @return the payment session obtained from the Payment API
     */
    private PaymentSession asyncLoadPaymentSession(String listUrl) throws PaymentException {
        ListResult listResult = listConnection.getListResult(listUrl);
        Map<String, PaymentNetwork> networks = loadPaymentNetworks(listResult);

        PresetCard presetCard = createPresetCard(listResult, networks);
        List<AccountCard> accountCards = createAccountCards(listResult, networks);
        List<NetworkCard> networkCards = createNetworkCards(networks);

        PaymentSession session = new PaymentSession(listResult, presetCard, accountCards, networkCards);
        session.setLang(loadPaymentPageLanguageFile(networks));
        return session;
    }

    /**
     * Post an Operation to the Payment API
     *
     * @param operation the object containing the operation details
     * @return operation result containing information about the operation request
     */
    private OperationResult asyncPostOperation(Operation operation) throws PaymentException {
        return paymentConnection.postOperation(operation);
    }

    private List<NetworkCard> createNetworkCards(Map<String, PaymentNetwork> networks) throws PaymentException {
        Map<String, PaymentGroup> groups = loadPaymentGroups();
        Map<String, NetworkCard> cards = new LinkedHashMap<>();

        NetworkCard card;
        PaymentGroup group;
        String code;

        for (PaymentNetwork network : networks.values()) {
            code = network.getCode();

            if ((group = groups.get(code)) == null) {
                addNetworkCard(cards, code, network);
                continue;
            }
            network.setSmartSelectionRegex(group.getSmartSelectionRegex(code));
            card = cards.get(group.getId());

            if (card == null) {
                addNetworkCard(cards, group.getId(), network);
            } else if (!card.addPaymentNetwork(network)) {
                addNetworkCard(cards, code, network);
            }
        }
        return new ArrayList<>(cards.values());
    }

    private void addNetworkCard(Map<String, NetworkCard> cards, String cardId, PaymentNetwork network) {
        NetworkCard card = new NetworkCard();
        card.addPaymentNetwork(network);
        cards.put(cardId, card);
    }

    private List<AccountCard> createAccountCards(ListResult listResult, Map<String, PaymentNetwork> networks) {
        List<AccountCard> cards = new ArrayList<>();
        List<AccountRegistration> accounts = listResult.getAccounts();

        if (accounts == null || accounts.size() == 0) {
            return cards;
        }
        for (AccountRegistration account : accounts) {
            PaymentNetwork pn = networks.get(account.getCode());

            if (pn != null) {
                cards.add(createAccountCard(account, pn));
            }
        }
        return cards;
    }

    private PresetCard createPresetCard(ListResult listResult, Map<String, PaymentNetwork> networks) {
        PresetAccount account = listResult.getPresetAccount();

        if (account == null) {
            return null;
        }
        PaymentNetwork pn = networks.get(account.getCode());
        if (pn == null) {
            return null;
        }
        PresetCard card = new PresetCard(account, pn.network);
        card.setLang(pn.getLang());
        return card;
    }

    private Map<String, PaymentNetwork> loadPaymentNetworks(ListResult listResult) throws PaymentException {
        LinkedHashMap<String, PaymentNetwork> items = new LinkedHashMap<>();
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
                items.put(network.getCode(), loadPaymentNetwork(network));
            }
        }
        return items;
    }

    private PaymentNetwork loadPaymentNetwork(ApplicableNetwork network) throws PaymentException {
        PaymentNetwork paymentNetwork = new PaymentNetwork(network);
        URL langUrl = paymentNetwork.getLink("lang");

        if (langUrl == null) {
            throw createPaymentException("Missing 'lang' link in ApplicableNetwork", null);
        }
        paymentNetwork.setLang(listConnection.loadLanguageFile(langUrl, new LanguageFile()));
        return paymentNetwork;
    }

    private AccountCard createAccountCard(AccountRegistration registration, PaymentNetwork paymentNetwork) {
        AccountCard card = new AccountCard(registration, paymentNetwork.network);
        card.setLang(paymentNetwork.getLang());
        return card;
    }

    /**
     * This method loads the payment page language file.
     * The URL for the paymentpage language file is constructed from the URL of one of the ApplicableNetwork entries.
     *
     * @param networks contains the list of PaymentNetwork elements
     * @return the properties object containing the language entries
     */
    private LanguageFile loadPaymentPageLanguageFile(Map<String, PaymentNetwork> networks) throws PaymentException {
        LanguageFile file = new LanguageFile();

        if (networks.size() == 0) {
            return file;
        }
        PaymentNetwork network = networks.entrySet().iterator().next().getValue();
        URL langUrl = network.getLink("lang");

        if (langUrl == null) {
            throw createPaymentException("Missing 'lang' link in ApplicableNetwork", null);
        }
        try {
            String newUrl = langUrl.toString().replaceAll(network.getCode(), "paymentpage");
            return listConnection.loadLanguageFile(new URL(newUrl), file);
        } catch (MalformedURLException e) {
            throw createPaymentException("Malformed language URL", e);
        }
    }

    private Map<String, PaymentGroup> loadPaymentGroups() throws PaymentException {
        int groupResId = PaymentUI.getInstance().getGroupResId();
        Resources res = presenter.getContext().getResources();
        return ResourceLoader.loadPaymentGroups(res, groupResId);
    }

    private boolean isSupported(ApplicableNetwork network) {
        String button = network.getButton();
        return (TextUtils.isEmpty(button) || !button.contains("activate")) && !network.getRedirect();
    }

    private PaymentException createPaymentException(String message, Throwable cause) {
        Log.w(TAG, cause);
        final PaymentError error = new PaymentError("PaymentPage", PaymentError.INTERNAL_ERROR, message);
        return new PaymentException(error, message, cause);
    }
}
