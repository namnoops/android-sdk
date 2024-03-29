/*
 * Copyright (c) 2019 optile GmbH
 * https://www.optile.net
 *
 * This file is open source and available under the MIT license.
 * See the LICENSE file for more information.
 */

package net.optile.payment.ui.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import net.optile.payment.R;

public class DialogParametersTest {

    @Test
    public void createBuilder() {
        DialogParameters.Builder builder = DialogParameters.createBuilder();
        assertNotNull(builder);
    }

    @Test
    public void createDefault() {
        DialogParameters params = DialogParameters.createDefault();
        assertNotNull(params);
        assertEquals(params.getDialogTheme(), R.style.PaymentThemeDialog);
        assertEquals(params.getDateTitleStyle(), R.style.PaymentText_Medium_Bold);
        assertEquals(params.getDateSubtitleStyle(), R.style.PaymentText_Small_Bold);
        assertEquals(params.getMessageTitleStyle(), R.style.PaymentText_Large_Bold);
        assertEquals(params.getMessageDetailsStyle(), R.style.PaymentText_Medium_Gray);
        assertEquals(params.getMessageDetailsNoTitleStyle(), R.style.PaymentText_Medium_Bold_Gray);
        assertEquals(params.getButtonLabelStyle(), R.style.PaymentText_Small_Bold_Primary);
        assertEquals(params.getImageLabelStyle(), R.style.PaymentText_Tiny);
        assertEquals(params.getSnackbarTextStyle(), R.style.PaymentText_Small_Light);
    }

    @Test
    public void getDialogTheme() {
        int value = R.style.PaymentThemeDialog;
        DialogParameters params = DialogParameters.createBuilder().
            setDialogTheme(value).build();
        assertEquals(params.getDialogTheme(), value);
    }

    @Test
    public void getDateTitleStyle() {
        int value = R.style.PaymentText_Medium_Bold;
        DialogParameters params = DialogParameters.createBuilder().
            setDateTitleStyle(value).build();
        assertEquals(params.getDateTitleStyle(), value);
    }

    @Test
    public void getDateSubtitleStyle() {
        int value = R.style.PaymentText_Small_Bold;
        DialogParameters params = DialogParameters.createBuilder().
            setDateSubtitleStyle(value).build();
        assertEquals(params.getDateSubtitleStyle(), value);
    }

    @Test
    public void getMessageTitleStyle() {
        int value = R.style.PaymentText_Large_Bold;
        DialogParameters params = DialogParameters.createBuilder().
            setMessageTitleStyle(value).build();
        assertEquals(params.getMessageTitleStyle(), value);
    }

    @Test
    public void getMessageDetailsStyle() {
        int value = R.style.PaymentText_Medium_Gray;
        DialogParameters params = DialogParameters.createBuilder().
            setMessageDetailsStyle(value).build();
        assertEquals(params.getMessageDetailsStyle(), value);
    }

    @Test
    public void getMessageDetailsNoTitleStyle() {
        int value = R.style.PaymentText_Medium_Bold_Gray;
        DialogParameters params = DialogParameters.createBuilder().
            setMessageDetailsNoTitleStyle(value).build();
        assertEquals(params.getMessageDetailsNoTitleStyle(), value);
    }

    @Test
    public void getButtonLabelStyle() {
        int value = R.style.PaymentText_Small_Bold_Primary;
        DialogParameters params = DialogParameters.createBuilder().
            setButtonLabelStyle(value).build();
        assertEquals(params.getButtonLabelStyle(), value);
    }

    @Test
    public void getImageLabelStyle() {
        int value = R.style.PaymentText_Tiny;
        DialogParameters params = DialogParameters.createBuilder().
            setImageLabelStyle(value).build();
        assertEquals(params.getImageLabelStyle(), value);
    }

    @Test
    public void getSnackbarTextStyle() {
        int value = R.style.PaymentText_Small_Light;
        DialogParameters params = DialogParameters.createBuilder().
            setSnackbarTextStyle(value).build();
        assertEquals(params.getSnackbarTextStyle(), value);
    }

}
