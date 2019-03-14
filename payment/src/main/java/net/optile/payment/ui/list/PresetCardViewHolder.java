/*
 * Copyright (c) 2019 optile GmbH
 * https://www.optile.net
 *
 * This file is open source and available under the MIT license.
 * See the LICENSE file for more information.
 */

package net.optile.payment.ui.list;

import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import net.optile.payment.R;
import net.optile.payment.core.LanguageFile;
import net.optile.payment.model.AccountMask;
import net.optile.payment.ui.model.PaymentCard;
import net.optile.payment.ui.model.PresetCard;
import net.optile.payment.ui.theme.PageParameters;
import net.optile.payment.ui.theme.PaymentTheme;
import net.optile.payment.util.PaymentUtils;

/**
 * The PresetCardViewHolder class holding and binding views for an PresetCard
 */
final class PresetCardViewHolder extends PaymentCardViewHolder {

    private final TextView title;
    private final TextView subTitle;

    private PresetCardViewHolder(ListAdapter adapter, View parent, PresetCard presetCard) {
        super(adapter, parent);

        PaymentTheme theme = adapter.getPaymentTheme();
        PageParameters params = theme.getPageParameters();

        this.title = parent.findViewById(R.id.text_title);
        PaymentUtils.setTextAppearance(title, params.getPresetCardTitleStyle());
        this.subTitle = parent.findViewById(R.id.text_subtitle);
        PaymentUtils.setTextAppearance(subTitle, params.getPresetCardSubtitleStyle());

        addLogoView(parent, presetCard.getCode(), theme);
        addButtonWidget(theme);
        addLabelWidget(theme);
    }

    static ViewHolder createInstance(ListAdapter adapter, PresetCard presetCard, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.list_item_presetcard, parent, false);
        return new PresetCardViewHolder(adapter, view, presetCard);
    }

    void onBind(PaymentCard paymentCard) {

        if (!(paymentCard instanceof PresetCard)) {
            throw new IllegalArgumentException("Expected PresetCard in onBind");
        }
        super.onBind(paymentCard);
        PresetCard card = (PresetCard) paymentCard;
        AccountMask mask = card.getMaskedAccount();
        bindMaskedTitle(title, mask, card.getPaymentMethod());
        bindMaskedSubTitle(subTitle, mask);
        bindLogoView(card.getCode(), card.getLink("logo"), true);

        LanguageFile lang = adapter.getPageLanguageFile();
        bindLabelWidget(lang.translate("networks.preset.text", null));
    }

}
