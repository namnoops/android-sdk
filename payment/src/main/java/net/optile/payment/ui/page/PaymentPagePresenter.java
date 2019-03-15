/*
 * Copyright (c) 2019 optile GmbH
 * https://www.optile.net
 *
 * This file is open source and available under the MIT license.
 * See the LICENSE file for more information.
 */

package net.optile.payment.ui.page;

import java.net.URL;
import java.util.Map;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import net.optile.payment.R;
import net.optile.payment.core.PaymentError;
import net.optile.payment.core.PaymentException;
import net.optile.payment.form.Operation;
import net.optile.payment.model.ErrorInfo;
import net.optile.payment.model.Interaction;
import net.optile.payment.model.InteractionCode;
import net.optile.payment.model.InteractionReason;
import net.optile.payment.model.OperationResult;
import net.optile.payment.ui.PaymentResult;
import net.optile.payment.ui.PaymentUI;
import net.optile.payment.ui.dialog.MessageDialogFragment;
import net.optile.payment.ui.dialog.ThemedDialogFragment;
import net.optile.payment.ui.dialog.ThemedDialogFragment.ThemedDialogListener;
import net.optile.payment.ui.model.PaymentCard;
import net.optile.payment.ui.model.PaymentSession;
import net.optile.payment.ui.widget.FormWidget;
import net.optile.payment.validation.Validator;

/**
 * The PaymentPagePresenter implementing the presenter part of the MVP
 */
final class PaymentPagePresenter {
    private final PaymentPageView view;
    private final PaymentPageService service;
    private Validator validator;
    private PaymentSession session;
    private String listUrl;
    private Interaction reloadInteraction;
    private Context context;
    private Operation operation;

    /**
     * Create a new PaymentPagePresenter
     *
     * @param view The PaymentPageView displaying the payment list
     */
    PaymentPagePresenter(PaymentPageView view) {
        this.view = view;
        this.service = new PaymentPageService(this);
    }

    /**
     * Notify this presenter that it should stop and cleanup its resources
     */
    void onStop() {
        service.stop();
    }

    /**
     * Let the Presenter handle the back press, i.e. if the presenter is currently performing an operation, the presenter may disable the back button press.
     *
     * @return true when this presenter handles the back press, false otherwise
     */
    boolean onBackPressed() {
        if (service.isPerformingOperation()) {
            view.showSnackbar(view.getStringRes(R.string.pmsend_interrupted));
            return true;
        }
        return false;
    }

    /**
     * Load the PaymentSession from the Payment API. once loaded, populate the View with the newly loaded groups of payment methods.
     * If a previous session with the same listUrl is available then reuse the existing one.
     *
     * @param context context in which this presenter is running
     * @param listUrl the url pointing to the ListResult in the Payment API
     */

    void load(Context context, String listUrl) {

        if (service.isActive()) {
            return;
        }
        this.listUrl = listUrl;
        this.context = context;

        if (validator != null && session != null && session.isListUrl(listUrl)) {
            // show the cached payment session
            view.showPaymentSession(session);
            return;
        }
        view.showProgress(true, PaymentProgressView.LOAD);

        if (validator == null) {
            service.loadValidator();
        } else {
            loadPaymentSession(listUrl);
        }
    }

    /**
     * Get the context in which this presenter is running.
     *
     * @return context
     */
    Context getContext() {
        return this.context;
    }

    /**
     * Notify this presenter that the user has clicked the action button in the PaymentCard.
     * The presenter will validate if the operation is supported and then post it to the Payment API.
     *
     * @param card the PaymentCard containing the operation URL
     * @param widgets containing the user input data
     */
    void onActionClicked(PaymentCard card, Map<String, FormWidget> widgets) {

        if (service.isActive()) {
            return;
        }
        if (session.presetCard == card) {
            PaymentResult result = new PaymentResult("Same presetAccount selected");
            closeSessionWithOkCode(result);
            return;
        }
        switch (session.getOperationType()) {
            case Operation.CHARGE:
            case Operation.PRESET:
                postOperation(card, widgets);
                break;
            default:
                Log.w("pay_Presenter", "OperationType not supported");
        }
    }

    /**
     * Return the Validator stored in this presenter.
     *
     * @return validator validator used to validate user input values
     */
    Validator getValidator() {
        return validator;
    }

    /**
     * Callback from the service when the validator has been successfully loaded
     *
     * @param validator that has been loaded
     */
    void onValidatorSuccess(Validator validator) {

        if (!view.isActive()) {
            return;
        }
        this.validator = validator;
        loadPaymentSession(this.listUrl);
    }

    /**
     * Callback from the service that the validator could not be loaded
     *
     * @param cause containing the error
     */
    void onValidatorError(Throwable cause) {
        closeSessionWithErrorCode(R.string.pmdialog_error_unknown, cause);
    }

    /**
     * Callback from the service that the PaymentSession has successfully been loaded
     *
     * @param session that has been loaded from the Payment API
     */
    void onPaymentSessionSuccess(PaymentSession session) {
        Interaction interaction = session.listResult.getInteraction();

        switch (interaction.getCode()) {
            case InteractionCode.PROCEED:
                handleLoadInteractionProceed(session);
                break;
            default:
                PaymentResult result = new PaymentResult(session.listResult.getResultInfo(), interaction);
                closeSessionWithCanceledCode(result);
        }
    }

    /**
     * Callback from the service that the PaymentSession failed to load
     *
     * @param cause containing the reason why the loading failed
     */
    void onPaymentSessionError(Throwable cause) {
        if (cause instanceof PaymentException) {
            handleLoadPaymentError((PaymentException) cause);
            return;
        }
        closeSessionWithErrorCode(R.string.pmdialog_error_unknown, cause);
    }

    /**
     * Callback from the service that the operation request was successful.
     *
     * @param operation containing the result of the operation
     */
    void onOperationSuccess(OperationResult operation) {
        PaymentResult result = new PaymentResult(operation);

        switch (operation.getInteraction().getCode()) {
            case InteractionCode.PROCEED:
                closeSessionWithOkCode(result);
                break;
            default:
                handleOperationInteractionError(result);
        }
    }

    /**
     * Callback from the service that the operation request failed
     *
     * @param cause containing the details why the operation failed
     */
    void onOperationError(Throwable cause) {

        if (cause instanceof PaymentException) {
            handleOperationPaymentError((PaymentException) cause);
            return;
        }
        closeSessionWithErrorCode(R.string.pmdialog_error_unknown, cause);
    }

    private void handleLoadInteractionProceed(PaymentSession session) {
        this.session = session;

        if (reloadInteraction != null) {
            showInteractionMessage(reloadInteraction);
            reloadInteraction = null;
        }
        view.showPaymentSession(session);
    }

    private void handleLoadPaymentError(PaymentException cause) {
        PaymentError error = cause.error;
        ErrorInfo info = error.errorInfo;

        if (info != null) {
            closeSessionWithCanceledCode(new PaymentResult(info.getResultInfo(), info.getInteraction()));
        } else if (error.isError(PaymentError.CONN_ERROR)) {
            handleLoadConnError(cause);
        } else {
            closeSessionWithErrorCode(R.string.pmdialog_error_unknown, cause);
        }
    }

    private void postOperation(PaymentCard card, Map<String, FormWidget> widgets) {
        URL url = card.getOperationLink();
        Operation operation = new Operation(url);

        try {
            boolean error = false;
            for (FormWidget widget : widgets.values()) {

                if (widget.validate()) {
                    widget.putValue(operation);
                } else {
                    error = true;
                }
                widget.clearFocus();
            }
            if (!error) {
                postOperation(operation);
            }
        } catch (PaymentException e) {
            closeSessionWithErrorCode(R.string.pmdialog_error_unknown, e);
        }
    }

    private void reloadPaymentSession(PaymentResult result) {
        this.reloadInteraction = result.getInteraction();
        loadPaymentSession(this.listUrl);
    }

    private void handleOperationPaymentError(PaymentException cause) {
        PaymentError error = cause.error;
        ErrorInfo info = error.errorInfo;

        if (info != null) {
            handleOperationInteractionError(new PaymentResult(info.getResultInfo(), info.getInteraction()));
        } else if (error.isError(PaymentError.CONN_ERROR)) {
            handleOperationConnError();
        } else {
            closeSessionWithErrorCode(R.string.pmdialog_error_unknown, cause);
        }
    }

    private void handleOperationInteractionError(PaymentResult result) {
        Interaction interaction = result.getInteraction();

        switch (interaction.getCode()) {
            case InteractionCode.RELOAD:
            case InteractionCode.TRY_OTHER_NETWORK:
                reloadPaymentSession(result);
                break;
            case InteractionCode.RETRY:
            case InteractionCode.TRY_OTHER_ACCOUNT:
                continueSessionWithWarning(result);
                break;
            case InteractionCode.ABORT:
                handleOperationInteractionAbort(result);
                break;
            default:
                closeSessionWithCanceledCode(result);
        }
    }

    private void handleOperationInteractionAbort(PaymentResult result) {
        Interaction interaction = result.getInteraction();

        switch (interaction.getReason()) {
            case InteractionReason.DUPLICATE_OPERATION:
                closeSessionWithOkCode(result);
                break;
            default:
                closeSessionWithCanceledCode(result);
        }
    }

    private void showInteractionMessage(Interaction interaction) {
        String msg = createInteractionMessage(interaction, null);

        if (!TextUtils.isEmpty(msg)) {
            showMessage(msg);
        }
    }

    private void continueSessionWithWarning(PaymentResult result) {
        view.showPaymentSession(this.session);
        showInteractionMessage(result.getInteraction());
    }

    private void closeSessionWithOkCode(PaymentResult result) {
        view.setPaymentResult(PaymentUI.RESULT_CODE_OK, result);
        view.closePage();
    }

    private void closeSessionWithCanceledCode(PaymentResult result) {
        String msg = createInteractionMessage(result.getInteraction(), view.getStringRes(R.string.pmdialog_error_unknown));
        view.setPaymentResult(PaymentUI.RESULT_CODE_CANCELED, result);
        closePageWithMessage(msg);
    }
    
    private void closeSessionWithErrorCode(int msgResId, Throwable cause) {
        PaymentResult result;

        if (cause instanceof PaymentException) {
            PaymentException pe = (PaymentException) cause;
            result = new PaymentResult(pe.getMessage(), pe.error);
        } else {
            String resultInfo = cause.toString();
            PaymentError error = new PaymentError("PaymentPage", PaymentError.INTERNAL_ERROR, resultInfo);
            result = new PaymentResult(resultInfo, error);
        }
        view.setPaymentResult(PaymentUI.RESULT_CODE_ERROR, result);
        closePageWithMessage(view.getStringRes(msgResId));
    }

    private void loadPaymentSession(final String listUrl) {
        this.session = null;
        view.clear();
        service.loadPaymentSession(listUrl);
    }

    private void postOperation(final Operation operation) {
        this.operation = operation;
        view.showProgress(true, PaymentProgressView.SEND);
        service.postOperation(operation);
    }

    private void handleLoadConnError(final PaymentException pe) {
        MessageDialogFragment dialog = createMessageDialog(view.getStringRes(R.string.pmdialog_error_connection), true);
        PaymentResult result = new PaymentResult(pe.getMessage(), pe.error);
        view.setPaymentResult(PaymentUI.RESULT_CODE_ERROR, result);

        dialog.setListener(new ThemedDialogListener() {
            @Override
            public void onButtonClicked(ThemedDialogFragment dialog, int which) {
                switch (which) {
                    case ThemedDialogFragment.BUTTON_NEUTRAL:
                        view.closePage();
                        break;
                    case ThemedDialogFragment.BUTTON_POSITIVE:
                        loadPaymentSession(listUrl);
                }
            }

            @Override
            public void onDismissed(ThemedDialogFragment dialog) {
                view.closePage();
            }
        });
        view.showDialog(dialog);
    }

    private void handleOperationConnError() {
        view.showPaymentSession(this.session);
        MessageDialogFragment dialog = createMessageDialog(view.getStringRes(R.string.pmdialog_error_connection), true);

        dialog.setListener(new ThemedDialogListener() {
            @Override
            public void onButtonClicked(ThemedDialogFragment dialog, int which) {
                if (which == ThemedDialogFragment.BUTTON_POSITIVE) {
                    postOperation(operation);
                }
            }

            @Override
            public void onDismissed(ThemedDialogFragment dialog) {
            }
        });
        view.showDialog(dialog);
    }

    private String createInteractionMessage(Interaction interaction, String defMessage) {

        if (session == null || interaction == null) {
            return defMessage;
        }
        String msg = session.getLang().translateInteraction(interaction);
        return TextUtils.isEmpty(msg) ? defMessage : msg;
    }

    private void showMessage(String message) {
        view.showDialog(createMessageDialog(message, false));
    }

    private void closePageWithMessage(String message) {
        MessageDialogFragment dialog = createMessageDialog(message, false);
        dialog.setListener(new ThemedDialogListener() {
            @Override
            public void onButtonClicked(ThemedDialogFragment dialog, int which) {
                view.closePage();
            }

            @Override
            public void onDismissed(ThemedDialogFragment dialog) {
                view.closePage();
            }
        });
        view.showDialog(dialog);
    }

    private MessageDialogFragment createMessageDialog(String message, boolean hasRetry) {
        MessageDialogFragment dialog = new MessageDialogFragment();
        dialog.setMessage(message);
        dialog.setNeutralButton(view.getStringRes(R.string.pmdialog_cancel_button));

        if (hasRetry) {
            dialog.setPositiveButton(view.getStringRes(R.string.pmdialog_retry_button));
        }
        return dialog;
    }
}
