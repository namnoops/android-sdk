/*
 * Copyright (c) 2019 optile GmbH
 * https://www.optile.net
 *
 * This file is open source and available under the MIT license.
 * See the LICENSE file for more information.
 */

package net.optile.payment.ui.page;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import net.optile.payment.R;
import net.optile.payment.ui.theme.PaymentTheme;
import net.optile.payment.ui.theme.ProgressParameters;
import net.optile.payment.util.PaymentUtils;

/**
 * View managing the different style of progress animations.
 */
class PaymentProgressView {

    public final static int LOAD = 0x00;
    public final static int SEND = 0x01;

    private final static int SEND_MIN = 0;
    private final static int SEND_MAX = 1000;
    private final static int SEND_TIMEOUT = 30000 / SEND_MAX;

    private final PaymentPageActivity activity;
    private final View loadLayout;
    private final View sendLayout;
    private final TextView sendHeader;
    private final TextView sendInfo;
    private final Handler sendHandler;
    private final ProgressBar loadProgressBar;
    private final ProgressBar sendProgressBar;
    private int style;

    /**
     * Construct a new loading view given the parent view that holds the Views for the loading animations
     *
     * @param activity controls the loading animation
     * @param theme contains the ProgressParameters for theming the progress views
     */
    PaymentProgressView(PaymentPageActivity activity, PaymentTheme theme) {
        this.activity = activity;
        ProgressParameters params = theme.getProgressParameters();

        // setup the ProgressBar for loading
        loadLayout = activity.findViewById(R.id.layout_load);
        loadProgressBar = loadLayout.findViewById(R.id.progressbar_load);
        applyLoadTheming(params);

        // setup the ProgressBar for sending
        sendLayout = activity.findViewById(R.id.layout_send);
        sendHeader = sendLayout.findViewById(R.id.text_sendheader);
        sendInfo = sendLayout.findViewById(R.id.text_sendinfo);
        sendProgressBar = sendLayout.findViewById(R.id.progressbar_send);
        applySendTheming(params);

        sendHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Set the style of progress that this view should show
     *
     * @param style to be shown when made visible
     */
    public void setStyle(int style) {
        this.style = style;
    }

    /**
     * Show the loading view with the given style
     *
     * @param visible when true, show the loading animation, hide it otherwise
     */
    public void setVisible(boolean visible) {

        if (!visible) {
            loadLayout.setVisibility(View.GONE);
            sendLayout.setVisibility(View.GONE);
            stopSendProgress();
            return;
        }
        switch (style) {
            case SEND:
                setSendVisible();
                break;
            default:
                setLoadVisible();
        }
    }

    /**
     * Notify that this Progress view should be stopped
     */
    public void onStop() {
        stopSendProgress();
    }

    private void setLoadVisible() {
        sendLayout.setVisibility(View.GONE);
        loadLayout.setVisibility(View.VISIBLE);
    }

    private void setSendVisible() {
        loadLayout.setVisibility(View.GONE);
        sendLayout.setVisibility(View.VISIBLE);
        sendHeader.setText(activity.getString(R.string.pmsend_header));
        sendInfo.setText(activity.getString(R.string.pmsend_info));
        startSendProgress();
    }

    private void stopSendProgress() {
        sendHandler.removeCallbacksAndMessages(null);
    }

    private void startSendProgress() {
        sendProgressBar.setProgress(SEND_MIN);
        sendProgressBar.setMax(SEND_MAX);
        sendHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int progress = sendProgressBar.getProgress() + 1;
                if (progress > SEND_MAX) {
                    progress = SEND_MIN;
                }
                sendProgressBar.setProgress(progress);
                sendHandler.postDelayed(this, SEND_TIMEOUT);
            }
        }, SEND_TIMEOUT);
    }

    private void applyLoadTheming(ProgressParameters params) {
        loadLayout.setBackgroundResource(params.getLoadBackground());
        setColorFilter(loadProgressBar.getIndeterminateDrawable(), params.getLoadProgressBarColor());
    }

    private void applySendTheming(ProgressParameters params) {
        sendLayout.setBackgroundResource(params.getSendBackground());
        PaymentUtils.setTextAppearance(sendHeader, params.getHeaderStyle());
        PaymentUtils.setTextAppearance(sendInfo, params.getInfoStyle());

        LayerDrawable layer = (LayerDrawable) sendProgressBar.getProgressDrawable();
        setColorFilter(layer.getDrawable(0), params.getSendProgressBarColorBack());
        setColorFilter(layer.getDrawable(1), params.getSendProgressBarColorFront());
    }

    private void setColorFilter(Drawable drawable, int resId) {
        if (drawable == null || resId == 0) {
            return;
        }
        drawable.setColorFilter(ContextCompat.getColor(activity, resId), PorterDuff.Mode.SRC_IN);
    }
}
