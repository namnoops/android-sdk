/*
 * Copyright (c) 2019 optile GmbH
 * https://www.optile.net
 *
 * This file is open source and available under the MIT license.
 * See the LICENSE file for more information.
 */

package net.optile.payment.ui.list;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import net.optile.payment.R;
import net.optile.payment.core.LanguageFile;
import net.optile.payment.core.PaymentInputType;
import net.optile.payment.model.AccountMask;
import net.optile.payment.model.InputElement;
import net.optile.payment.model.PaymentMethod;
import net.optile.payment.ui.model.PaymentCard;
import net.optile.payment.ui.theme.PaymentTheme;
import net.optile.payment.ui.theme.WidgetParameters;
import net.optile.payment.ui.widget.ButtonWidget;
import net.optile.payment.ui.widget.DateWidget;
import net.optile.payment.ui.widget.FormWidget;
import net.optile.payment.ui.widget.LabelWidget;
import net.optile.payment.ui.widget.SelectWidget;
import net.optile.payment.ui.widget.TextInputWidget;
import net.optile.payment.ui.widget.WidgetInflater;
import net.optile.payment.ui.widget.WidgetPresenter;
import net.optile.payment.util.ImageHelper;
import net.optile.payment.util.PaymentUtils;
import net.optile.payment.validation.ValidationResult;

/**
 * The PaymentCardViewHolder holding the header and input widgets
 */
abstract class PaymentCardViewHolder extends RecyclerView.ViewHolder {

    final static String BUTTON_WIDGET = "buttonWidget";
    final static String LABEL_WIDGET = "labelWidget";

    final static float ALPHA_SELECTED = 1f;
    final static float ALPHA_DESELECTED = 0.4f;
    final static int ANIM_DURATION = 200;
    final static int COLUMN_SIZE_LANDSCAPE = 3;
    final static int COLUMN_SIZE_PORTRAIT = 2;

    final ViewGroup formLayout;
    final ListAdapter adapter;
    final WidgetPresenter presenter;
    final Map<String, FormWidget> widgets;
    final TableLayout logoLayout;
    final Map<String, ImageView> logos;

    PaymentCardViewHolder(ListAdapter adapter, View parent) {
        super(parent);
        this.adapter = adapter;

        this.formLayout = parent.findViewById(R.id.layout_form);
        this.widgets = new LinkedHashMap<>();
        this.logoLayout = parent.findViewById(R.id.tablelayout_logo);
        this.logos = new HashMap<>();

        View view = parent.findViewById(R.id.layout_header);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.onItemClicked(getAdapterPosition());
            }
        });

        this.presenter = new WidgetPresenter() {
            @Override
            public void onActionClicked() {
                adapter.onActionClicked(getAdapterPosition());
            }

            @Override
            public void onHintClicked(String type) {
                adapter.onHintClicked(getAdapterPosition(), type);
            }

            @Override
            public void hideKeyboard() {
                adapter.hideKeyboard(getAdapterPosition());
            }

            @Override
            public void showKeyboard() {
                adapter.showKeyboard(getAdapterPosition());
            }

            @Override
            public void showDialogFragment(DialogFragment dialog, String tag) {
                adapter.showDialogFragment(getAdapterPosition(), dialog, tag);
            }

            @Override
            public ValidationResult validate(String type, String value1, String value2) {
                return adapter.validate(getAdapterPosition(), type, value1, value2);
            }

            @Override
            public void onTextInputChanged(String type, String text) {
                adapter.onTextInputChanged(getAdapterPosition(), type, text);
            }
        };
    }

    void addButtonWidget(PaymentTheme theme) {
        FormWidget widget = WidgetInflater.inflateButtonWidget(BUTTON_WIDGET, formLayout, theme);
        addWidget(widget);
    }

    void addLabelWidget(PaymentTheme theme) {
        FormWidget widget = WidgetInflater.inflateLabelWidget(LABEL_WIDGET, formLayout, theme);
        addWidget(widget);
    }

    void addElementWidgets(PaymentCard card, PaymentTheme theme) {
        String code = card.getCode();
        List<InputElement> elements = card.getInputElements();
        DateWidget dateWidget = null;
        boolean containsExpiryDate = PaymentUtils.containsExpiryDate(elements);

        for (InputElement element : elements) {
            String name = element.getName();

            if (adapter.isHidden(code, name)) {
                continue;
            }
            if (!containsExpiryDate) {
                addWidget(WidgetInflater.inflateElementWidget(element, formLayout, theme));
                continue;
            }
            switch (element.getName()) {
                case PaymentInputType.EXPIRY_MONTH:
                case PaymentInputType.EXPIRY_YEAR:
                    if (dateWidget == null) {
                        dateWidget = WidgetInflater.inflateDateWidget(PaymentInputType.EXPIRY_DATE, formLayout, theme);
                        addWidget(dateWidget);
                    }
                    break;
                default:
                    addWidget(WidgetInflater.inflateElementWidget(element, formLayout, theme));
            }
        }
    }

    void addWidget(FormWidget widget) {
        String name = widget.getName();

        if (widgets.containsKey(name)) {
            return;
        }
        widget.setPresenter(presenter);
        widgets.put(name, widget);
        formLayout.addView(widget.getRootView());
    }

    void addLogoView(View parent, String name, PaymentTheme theme) {
        addLogoViews(parent, Collections.singletonList(name), theme);
    }

    void addLogoViews(View parent, List<String> names, PaymentTheme theme) {
        int logoBackground = theme.getPageParameters().getPaymentLogoBackground();
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        Context context = parent.getContext();

        TableRow row = null;
        Resources res = context.getResources();
        int border = res.getDimensionPixelSize(R.dimen.pmborder_xsmall);
        int columnsPerRow = PaymentUtils.isLandscape(context) ? COLUMN_SIZE_LANDSCAPE : COLUMN_SIZE_PORTRAIT;
        int columnIndex = 0;
        int rowCount = 0;

        for (String name : names) {

            if (columnIndex % columnsPerRow == 0) {
                rowCount++;
                columnIndex = 0;
                row = new TableRow(context);
                logoLayout.addView(row);
            }
            int marginTop = rowCount > 1 ? border : 0;
            int marginRight = ++columnIndex < columnsPerRow ? border : 0;

            ImageView view = (ImageView) inflater.inflate(R.layout.list_item_logo, row, false);
            LayoutParams params = (LayoutParams) view.getLayoutParams();

            params.setMargins(0, marginTop, marginRight, 0);
            view.setLayoutParams(params);
            PaymentUtils.setImageBackground(view, logoBackground);
            logos.put(name, view);
            row.addView(view);
        }
    }

    void expand(boolean expand) {
        formLayout.setVisibility(expand ? View.VISIBLE : View.GONE);

        for (FormWidget widget : widgets.values()) {
            widget.setValidation();
        }
    }

    FormWidget getFormWidget(String name) {
        return widgets.get(name);
    }

    void onBind(PaymentCard paymentCard) {
        bindElementWidgets(paymentCard);
        bindDateWidget(paymentCard);
        bindButtonWidget(paymentCard);
    }

    void bindLabelWidget(String label) {
        LabelWidget widget = (LabelWidget) getFormWidget(LABEL_WIDGET);

        if (widget != null) {
            widget.setLabel(label);
        }
    }

    void bindElementWidgets(PaymentCard card) {
        FormWidget widget;
        LanguageFile lang = card.getLang();

        for (InputElement element : card.getInputElements()) {
            widget = getFormWidget(element.getName());

            if (widget instanceof SelectWidget) {
                bindSelectWidget((SelectWidget) widget, element);
            } else if (widget instanceof TextInputWidget) {
                bindTextInputWidget((TextInputWidget) widget, card.getCode(), element, lang);
            }
        }
    }

    void bindMaskedTitle(TextView title, AccountMask mask, String method) {
        switch (method) {
            case PaymentMethod.CREDIT_CARD:
            case PaymentMethod.DEBIT_CARD:
                title.setText(mask.getNumber());
                break;
            default:
                title.setText(mask.getDisplayLabel());
        }
    }

    void bindMaskedSubTitle(TextView subTitle, AccountMask mask) {
        int expiryMonth = PaymentUtils.toInt(mask.getExpiryMonth());
        int expiryYear = PaymentUtils.toInt(mask.getExpiryYear());

        if (expiryMonth > 0 && expiryYear > 0) {
            String format = subTitle.getContext().getString(R.string.pmlist_subtitle_date);
            subTitle.setText(String.format(format, expiryMonth, expiryYear));
            subTitle.setVisibility(View.VISIBLE);
        } else {
            subTitle.setVisibility(View.GONE);
        }
    }


    void bindSelectWidget(SelectWidget widget, InputElement element) {
        bindIconResource(widget);
        widget.setLabel(element.getLabel());
        widget.setSelectOptions(element.getOptions());
    }

    void bindTextInputWidget(TextInputWidget widget, String code, InputElement element, LanguageFile lang) {
        WidgetParameters params = adapter.getPaymentTheme().getWidgetParameters();
        int hintDrawable = params.getHintDrawable();
        boolean visible = hintDrawable != 0 && lang.containsAccountHint(widget.getName());

        bindIconResource(widget);
        widget.setLabel(element.getLabel());
        widget.setInputElementType(element.getType());
        widget.setMaxLength(adapter.getMaxLength(code, element.getName()));
        widget.setHint(visible, hintDrawable);
        widget.setValidation();
    }

    void bindIconResource(FormWidget widget) {
        WidgetParameters params = adapter.getPaymentTheme().getWidgetParameters();
        widget.setIconResource(params.getInputTypeIcon(widget.getName()));
    }

    void bindDateWidget(PaymentCard card) {
        String name = PaymentInputType.EXPIRY_DATE;
        DateWidget widget = (DateWidget) getFormWidget(name);

        if (widget == null) {
            return;
        }
        LanguageFile pageLang = adapter.getPageLanguageFile();
        bindIconResource(widget);
        widget.setMonthInputElement(card.getInputElement(PaymentInputType.EXPIRY_MONTH));
        widget.setYearInputElement(card.getInputElement(PaymentInputType.EXPIRY_YEAR));

        widget.setLabel(card.getLang().getAccountLabel(name));
        widget.setDialogButtonLabel(pageLang.getButtonLabel(LanguageFile.BUTTON_UPDATE));
    }

    void bindButtonWidget(PaymentCard card) {
        ButtonWidget widget = (ButtonWidget) getFormWidget(BUTTON_WIDGET);

        if (widget == null) {
            return;
        }
        LanguageFile pageLang = adapter.getPageLanguageFile();
        widget.setLabel(pageLang.translate(card.getButton()));
    }

    void bindLogoView(String name, URL url, boolean selected) {
        ImageView view = logos.get(name);

        if (view == null || url == null) {
            return;
        }
        ImageHelper.getInstance().loadImage(view, url);
        float alpha = selected ? ALPHA_SELECTED : ALPHA_DESELECTED;
        ObjectAnimator.ofFloat(view, "alpha", alpha).setDuration(ANIM_DURATION).start();
    }

    void setLastImeOptions() {
        List<String> keys = new ArrayList<>(widgets.keySet());
        Collections.reverse(keys);

        for (String key : keys) {
            FormWidget widget = widgets.get(key);
            if (widget != null && widget.setLastImeOptionsWidget()) {
                break;
            }
        }
    }
}
