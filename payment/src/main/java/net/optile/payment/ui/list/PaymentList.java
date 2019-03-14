/*
 * Copyright (c) 2019 optile GmbH
 * https://www.optile.net
 *
 * This file is open source and available under the MIT license.
 * See the LICENSE file for more information.
 */

package net.optile.payment.ui.list;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import net.optile.payment.R;
import net.optile.payment.core.LanguageFile;
import net.optile.payment.ui.dialog.DialogHelper;
import net.optile.payment.ui.model.AccountCard;
import net.optile.payment.ui.model.NetworkCard;
import net.optile.payment.ui.model.PaymentSession;
import net.optile.payment.ui.page.PaymentPageActivity;
import net.optile.payment.validation.ValidationResult;

/**
 * The PaymentList showing available payment methods and accounts in a list
 */
public final class PaymentList {

    private final PaymentPageActivity activity;
    private final ListAdapter adapter;
    private final RecyclerView recyclerView;
    private final TextView emptyMessage;
    private final List<ListItem> items;

    private PaymentSession session;
    private int selIndex;
    private int viewType;

    public PaymentList(PaymentPageActivity activity, RecyclerView recyclerView, TextView emptyMessage) {
        this.activity = activity;
        this.items = new ArrayList<>();
        this.adapter = new ListAdapter(this, items);
        this.emptyMessage = emptyMessage;
        this.recyclerView = recyclerView;
        this.recyclerView.setAdapter(adapter);
        this.recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();

        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
    }

    public int getSelected() {
        return selIndex;
    }

    public void clear() {
        this.session = null;
        this.selIndex = -1;
        this.items.clear();

        emptyMessage.setText("");
        adapter.notifyDataSetChanged();
    }

    public void showPaymentSession(PaymentSession session, int cachedListIndex) {

        if (this.session == session) {
            setVisible(true);
            return;
        }
        this.session = session;
        setPaymentListItems(session, cachedListIndex);
        String msg = "";

        if (session.getApplicableNetworkSize() == 0) {
            msg = activity.getString(R.string.pmpage_error_empty);
        } else if (session.networks.size() == 0) {
            msg = activity.getString(R.string.pmpage_error_notsupported);
        } else {
            int startIndex = session.presetCard != null ? 0 : selIndex;
            recyclerView.scrollToPosition(startIndex);
        }
        emptyMessage.setText(msg);
        adapter.notifyDataSetChanged();
        setVisible(true);
    }

    public void setVisible(boolean visible) {
        emptyMessage.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        recyclerView.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);

        if (imm != null) {
            View curFocus = activity.getCurrentFocus();
            IBinder binder = curFocus != null ? curFocus.getWindowToken() : recyclerView.getWindowToken();
            imm.hideSoftInputFromWindow(binder, 0);
        }
    }

    public void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);

        if (imm != null) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
    }

    Context getContext() {
        return activity;
    }

    PaymentSession getPaymentSession() {
        return this.session;
    }

    void showDialogFragment(DialogFragment dialog, String tag) {
        dialog.show(activity.getSupportFragmentManager(), tag);
    }

    void onHintClicked(int position, String type) {
        ListItem item = items.get(position);

        if (item.hasPaymentCard()) {
            String button = session.getLang().getButtonLabel(LanguageFile.BUTTON_BACK);
            DialogFragment dialog = DialogHelper.createHintDialog(item.getPaymentCard(), type, button);
            showDialogFragment(dialog, "hint_dialog");
        }
    }

    void onActionClicked(int position) {
        ListItem item = items.get(position);
        PaymentCardViewHolder holder = (PaymentCardViewHolder) recyclerView.findViewHolderForAdapterPosition(position);

        if (holder != null && item.hasPaymentCard()) {
            hideKeyboard();
            activity.onActionClicked(item.getPaymentCard(), holder.widgets);
        }
    }

    void onItemClicked(int position) {
        ListItem item = items.get(position);

        if (!item.hasPaymentCard()) {
            return;
        }
        hideKeyboard();
        if (position == this.selIndex) {
            this.selIndex = -1;
            collapseViewHolder(position);
        } else {
            int curIndex = this.selIndex;
            this.selIndex = position;
            collapseViewHolder(curIndex);
            expandViewHolder(position);
        }
    }

    ValidationResult validate(int position, String type, String value1, String value2) {
        ListItem item = items.get(position);
        if (!item.hasPaymentCard()) {
            return null;
        }
        return activity.validate(item.getPaymentCard(), type, value1, value2);
    }

    boolean isHidden(String code, String type) {
        return activity.isHidden(code, type);
    }

    int getMaxLength(String code, String type) {
        return activity.getMaxLength(code, type);
    }

    private int nextViewType() {
        return viewType++;
    }

    private void setPaymentListItems(PaymentSession session, int cachedListIndex) {
        items.clear();
        this.selIndex = cachedListIndex;

        int index = 0;
        int accountSize = session.accounts.size();
        int networkSize = session.networks.size();
        LanguageFile lang = session.getLang();
        
        if (session.presetCard != null) {
            items.add(new HeaderItem(nextViewType(), lang.translate("networks.preset.title")));
            items.add(new PaymentCardItem(nextViewType(), session.presetCard));
            this.selIndex = 1;
            index += 2;
        }

        if (accountSize > 0) {
            items.add(new HeaderItem(nextViewType(), lang.translate("savedAccountsLabel")));
            index++;
        }
        for (AccountCard card : session.accounts) {
            items.add(new PaymentCardItem(nextViewType(), card));
            if (this.selIndex == -1 && card.isPreselected()) {
                this.selIndex = index;
            }
            index++;
        }
        if (networkSize > 0) {
            // NOTE: the network only header still comes from the strings.xml unless added to the
            // translation files.
            String header = accountSize == 0 ? activity.getString(R.string.pmlist_networkonly_header) :
                lang.translate("addNewAccountLabel");
            items.add(new HeaderItem(nextViewType(), header));
        }
        for (NetworkCard card : session.networks) {
            items.add(new PaymentCardItem(nextViewType(), card));
            if (this.selIndex == -1 && card.isPreselected()) {
                this.selIndex = index;
            }
            index++;
        }
    }

    private void collapseViewHolder(int position) {
        PaymentCardViewHolder holder = (PaymentCardViewHolder) recyclerView.findViewHolderForAdapterPosition(position);

        if (holder != null) {
            holder.expand(false);
            adapter.notifyItemChanged(position);
        }
    }

    private void expandViewHolder(int position) {
        PaymentCardViewHolder holder = (PaymentCardViewHolder) recyclerView.findViewHolderForAdapterPosition(position);

        if (holder != null) {
            holder.expand(true);
            adapter.notifyItemChanged(position);
            smoothScrollToPosition(position);
        }
    }

    private void smoothScrollToPosition(int position) {
        RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(activity) {
            @Override
            protected int getVerticalSnapPreference() {
                return LinearSmoothScroller.SNAP_TO_START;
            }
        };
        smoothScroller.setTargetPosition(position);
        RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();

        if (manager != null) {
            manager.startSmoothScroll(smoothScroller);
        }
    }
}
