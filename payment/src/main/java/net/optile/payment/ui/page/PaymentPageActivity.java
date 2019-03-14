/*
 * Copyright (c) 2019 optile GmbH
 * https://www.optile.net
 *
 * This file is open source and available under the MIT license.
 * See the LICENSE file for more information.
 */

package net.optile.payment.ui.page;

import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.TextView;
import net.optile.payment.R;
import net.optile.payment.ui.PaymentResult;
import net.optile.payment.ui.PaymentUI;
import net.optile.payment.ui.dialog.DialogHelper;
import net.optile.payment.ui.dialog.ThemedDialogFragment;
import net.optile.payment.ui.list.PaymentList;
import net.optile.payment.ui.model.PaymentCard;
import net.optile.payment.ui.model.PaymentSession;
import net.optile.payment.ui.theme.PaymentTheme;
import net.optile.payment.ui.widget.FormWidget;
import net.optile.payment.util.PaymentUtils;
import net.optile.payment.validation.ValidationResult;
import net.optile.payment.validation.Validator;

/**
 * The PaymentPageActivity showing available payment methods
 */
public final class PaymentPageActivity extends AppCompatActivity implements PaymentPageView {

    private static final String EXTRA_LISTURL = "extra_listurl";
    private PaymentPagePresenter presenter;
    private String listUrl;
    private boolean active;
    private PaymentList paymentList;
    private int cachedListIndex;
    private PaymentProgressView progress;

    /**
     * Create the start intent for this Activity
     *
     * @param context Context to create the intent
     * @return newly created start intent
     */
    public static Intent createStartIntent(Context context, String listUrl) {
        final Intent intent = new Intent(context, PaymentPageActivity.class);
        intent.putExtra(EXTRA_LISTURL, listUrl);
        return intent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PaymentResult result = new PaymentResult("Inializing Payment Page.");
        setActivityResult(PaymentUI.RESULT_CODE_CANCELED, result);
        this.cachedListIndex = -1;

        if (savedInstanceState != null) {
            this.listUrl = savedInstanceState.getString(EXTRA_LISTURL);
        } else {
            Intent intent = getIntent();
            this.listUrl = intent.getStringExtra(EXTRA_LISTURL);
        }
        PaymentTheme theme = PaymentUI.getInstance().getPaymentTheme();
        initPageTheme(theme);

        setContentView(R.layout.activity_paymentpage);
        setRequestedOrientation(PaymentUI.getInstance().getOrientation());

        initActionBar(getString(R.string.pmpage_title), true);
        initList(theme);

        this.progress = new PaymentProgressView(this, theme);
        this.presenter = new PaymentPagePresenter(this);
    }

    private void initPageTheme(PaymentTheme theme) {
        int pageTheme = theme.getPageParameters().getPageTheme();

        if (pageTheme != 0) {
            setTheme(pageTheme);
        }
    }

    private void initList(PaymentTheme theme) {
        TextView empty = findViewById(R.id.label_empty);
        PaymentUtils.setTextAppearance(empty, theme.getPageParameters().getEmptyListLabelStyle());
        this.paymentList = new PaymentList(this, findViewById(R.id.recyclerview_paymentlist), empty);
    }

    private void initActionBar(String title, boolean homeEnabled) {
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setTitle(title);
            actionBar.setHomeButtonEnabled(homeEnabled);
            actionBar.setDisplayHomeAsUpEnabled(homeEnabled);
            actionBar.setDisplayShowHomeEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        this.cachedListIndex = paymentList.getSelected();
        savedInstanceState.putString(EXTRA_LISTURL, listUrl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPause() {
        super.onPause();
        this.active = false;
        this.presenter.onStop();
        this.progress.onStop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume() {
        super.onResume();
        this.active = true;
        presenter.load(this, this.listUrl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                setUserClosedPageResult();
                supportFinishAfterTransition();
                return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onBackPressed() {
        if (presenter.onBackPressed()) {
            return;
        }
        setUserClosedPageResult();
        super.onBackPressed();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isActive() {
        return this.active;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStringRes(int resId) {
        return getString(resId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        if (!isActive()) {
            return;
        }
        paymentList.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showPaymentSession(PaymentSession session) {

        if (!isActive()) {
            return;
        }
        initActionBar(getString(R.string.pmpage_title), true);
        progress.setVisible(false);
        paymentList.showPaymentSession(session, cachedListIndex);
        this.cachedListIndex = -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showProgress(boolean show, int style) {
        if (!isActive()) {
            return;
        }
        if (show) {

            if (style == PaymentProgressView.SEND) {
                initActionBar(getString(R.string.pmprogress_sendtitle), false);
            }
            paymentList.setVisible(false);
            progress.setStyle(style);
            progress.setVisible(true);
        } else {
            initActionBar(getString(R.string.pmpage_title), true);
            paymentList.setVisible(true);
            progress.setVisible(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closePage() {
        if (!isActive()) {
            return;
        }
        finish();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPaymentResult(int resultCode, PaymentResult result) {
        if (!isActive()) {
            return;
        }
        setActivityResult(resultCode, result);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showDialog(ThemedDialogFragment dialog) {
        dialog.show(getSupportFragmentManager(), "paymentpage_dialog");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showSnackbar(String message) {

        if (TextUtils.isEmpty(message)) {
            return;
        }
        Snackbar snackbar = DialogHelper.createSnackbar(findViewById(R.id.layout_paymentpage), message);
        snackbar.show();
    }

    public void onActionClicked(PaymentCard item, Map<String, FormWidget> widgets) {
        presenter.onActionClicked(item, widgets);
    }

    /**
     * Validate the input type value for the given PaymentCard
     *
     * @param card payment card containing the input type
     * @param type type of the input field, i.e. number
     * @param value1 mandatory value1
     * @param value2 optional value2, mainly used to validate expiry month and year at the same time
     * @return result of the validation
     */
    public ValidationResult validate(PaymentCard card, String type, String value1, String value2) {
        Validator validator = presenter.getValidator();
        ValidationResult result = validator.validate(card.getPaymentMethod(), card.getCode(), type, value1, value2);

        if (!result.isError()) {
            return result;
        }
        String msg = card.getLang().getError(result.getError());

        if (TextUtils.isEmpty(msg)) {
            msg = getString(R.string.pmpage_error_validation);
        }
        result.setMessage(msg);
        return result;
    }

    /**
     * Check if the input type is hidden for the given payment card
     *
     * @param code the code of the payment method the input type belongs to, i.e. SEPA
     * @param type of the input field, i.e. bic
     * @return true when the input field should be hidden, false otherwise
     */
    public boolean isHidden(String code, String type) {
        Validator validator = presenter.getValidator();
        return validator.isHidden(code, type);
    }

    /**
     * Get the max length for the given input type.
     *
     * @param code the code of the payment method the input type belongs to, i.e. SEPA
     * @param type of the input field, i.e. number
     * @return maxLength of the input field
     */
    public int getMaxLength(String code, String type) {
        Validator validator = presenter.getValidator();
        return validator.getMaxLength(code, type);
    }

    private void setUserClosedPageResult() {
        PaymentResult result = new PaymentResult("Payment Page closed by user.");
        setActivityResult(PaymentUI.RESULT_CODE_CANCELED, result);
    }
    
    private void setActivityResult(int resultCode, PaymentResult result) {
        Intent intent = new Intent();
        intent.putExtra(PaymentUI.EXTRA_PAYMENT_RESULT, result);
        setResult(resultCode, intent);
    }
}
