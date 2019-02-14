
Introduction
============

The optile Android SDK makes it easy to integrate with optile
and provides a great looking payment experience in your Android app. The
SDK comes with a ready made, easy to use Payment Page which takes care
of showing supported payment methods and handling payments. The SDK also
provide low-level packages that can be used to build your own custom
payment experience in your app.

Supported Features
==================

Android Version
---------------

Android API versions 19 - 28 (Kitkat 4.4 - Pie 9.0) are supported by the
Android Payment SDK. TLS1.2 is enabled for Android version 19 (Kitkat).

Proguard
--------

If you intend to obfuscate your mobile app then please make sure to exclude the optile Android SDK classes from being obfuscated as well. Excluding the Android SDK from being obfuscated can be done by adding the following to your proguard-rules.pro file:

::

    -keep class net.optile.** { *; }
    
The Android SDK uses the following third-party libraries, please make sure to add the proper rules for these libraries in your proguard-rules.pro file if needed.

::

    implementation "com.google.code.gson:gson:${rootProject.gsonVersion}"
    implementation "com.github.bumptech.glide:glide:${rootProject.bumptechGlideVersion}"
 
Payment Methods
---------------

All “direct” payment methods are supported, this includes Credit, Debit
cards and Sepa. Payments that require “redirects” (external WebView) like
Paypal and Sofort are not supported by this SDK. The option “presetFirst”
is also supported and provides the option to show a summary page to your users
before finalizing the payment.

Integration Scenario
--------------------

The SDK requires payment sessions created using the DISPLAY_NATIVE
integration scenario. Below is a sample list request object that can be
used to create a payment session that is supported by the Android
Payment SDK.

Example list request Json body:

::

   {
       "transactionId": "tr1",
       "integration": "DISPLAY_NATIVE",
       "presetFirst": "false",
       "country": "DE",
       "customer": {
           "number": "1",
           "email": "john.doe@example.com"
       },
       "payment": {
           "amount": 9.99,
           "currency": "EUR",
           "reference": "Shop optile/03-12-2018"
       },
       "style": {
           "language": "en_US"
       },
       "callback": {
           "returnUrl": "https://example.com/shop/success.html",
           "summaryUrl": "https://example.com/shop/summary.html",
           "cancelUrl": "https://example.com/shop/cancel.html",
           "notificationUrl": "https://example.com/shop/notify.html"
       }
   }

Registration
------------

There are two kind of registrations: Regular and Recurring. Both types
are supported and depending on registration settings in the List Result
a checkbox may show for either type of registration. Please see
documentation at `optile.io <https://optile.io>`_ for more information 
about the registration types.

Your first payment
==================

In order to make a successful payment you must complete the following
steps:

1. Create a payment session on your server and retrieve the list URL in
   your app
2. Install Android SDK in your app
3. Initialise and show the Payment Page with the list URL

1 - Create payment session
--------------------------

The documentation at `optile.io <https://optile.io>`_ will guide you through optile’s Open
Payment Gateway (OPG) features for frontend checkout and backend use
cases. It provides important information about integration scenarios,
testing possibilities, and references. The documentation will help you
create a payment session that can be used by the Android Payment SDK.

After you have created a payment session on your server you will receive
a response containing the List Result in Json format. This List Result
contains a “self” URL which is used to initialise the Payment Page.

Part of the list result containing the “self” URL:

::

   {
     "links": {
       "self": "https://api.integration.oscato.com/pci/v1/5c17b47e7862056fa0755e66lrui4dvavak9ehlvh4n3abcde9",
       "customer": "https://api.integration.oscato.com/api/customers/123456789862053ccf15479eu"
     },
     "timestamp": "2018-12-17T14:36:46.105+0000",
     "operation": "LIST",
     ...

2 - Install Android SDK
-----------------------

Installing the Android SDK is easy and requires only adding the optile
Android SDK module to your build.gradle file. 

    Note: the Android SDK is currently only available through optile internal Nexus repository.
    
::

   implementation "com.oscato.mobile:android-sdk:1.1.0"

3 - Show Payment Page
---------------------

The Android SDK provides a class called PaymentUI which is used to initialise and launch the Payment Page.

Code sample how to initialise and display the Payment Page:

::

   // Request code to identify the response in onActivityResult()
   int PAYMENT_REQUEST_CODE = 1;

   // list URL obtained from your backend
   String listUrl = "<https://...>";

   // Show the Payment Page
   PaymentUI paymentUI = PaymentUI.getInstance();
   paymentUI.setListUrl(listUrl);
   paymentUI.showPaymentPage(this, PAYMENT_REQUEST_CODE);

Payment Result
==============

Payment results are returned through the onActivityResult() method in your Activity. When the payment page is closed, the Android SDK returns the result status of the last request performed. The last request is either a request to load the list or performing an operation (Charge/Preset).

Code sample how to obtain the PaymentResult inside the onActivityResult() method:

::

   @Override
   public void onActivityResult(int requestCode, int resultCode, Intent data) {

       if (requestCode != PAYMENT_REQUEST_CODE) {
           return;
       }
       PaymentResult result = null;

       if (data != null && data.hasExtra(PaymentUI.EXTRA_PAYMENT_RESULT)) {
           result = data.getParcelableExtra(PaymentUI.EXTRA_PAYMENT_RESULT);
       }
       if (resultCode == PaymentUI.RESULT_CODE_OK) {
           // Operation request has been made and "result" contains
           // an Interaction and optional OperationResult describing the operation result
       } 
       if (resultCode == PaymentUI.RESULT_CODE_CANCELED) {
           // 1. "result" is null if user closed the payment page without making an operation request. 
           // 2. "result" contains an Interaction and optional OperationResult. 
       }
       if (resultCode == PaymentUI.RESULT_CODE_ERROR) {
           // "result" contains a PaymentError explaining the error that occurred i.e. connection error.
       }
   }

Successful
----------

The RESULT_CODE_OK code indicates that the operation request was successful, there are two situations when this result is returned:

1. InteractionCode is PROCEED - the PaymentResult contains an OperationResult with detailed information about the operation. 

2. InteractionCode is ABORT and InteractionReason is DUPLICATE_OPERATION, this means that a previous operation on the same List has already been performed. This may happen if there was a network error during the first operation and the Android SDK was unable to receive a proper response from the Payment API.

Cancelled
---------

The RESULT_CODE_CANCELED code indicates that the Payment Page did not perform a successful operation. This may happen for different reasons, i.e. the user clicked the back button. The PaymentResult may contain an OperationResult with details about the failed operation.
    
Error
-----

The RESULT_CODE_ERROR code indicates that an unrecoverable error has occurred, i.e. a SecurityException has been thrown inside the Android SDK. The PaymentResult contains a PaymentError Object with the error details.
    
Customise Payment Page
======================

The look & feel of the Payment Page may be customised, i.e. colors, font
style and icons can be changed so that it matches the look & feel of your
mobile app.

Page Orientation
----------------

By default the orientation of the Payment Page will be locked based on
the orientation in which the Payment Page was opened. I.e. if the mobile
app is shown in landscape mode the Payment Page will also be opened in
landscape mode but cannot be changed anymore by rotating the phone.

Code sample how to set the fixed orientation mode:

::

   //
   // Orientation modes supported by the Payment Page
   // ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
   // ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
   // ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
   // ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
   //
   PaymentUI paymentUI = PaymentUI.getInstance();
   paymentUI.setOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
   paymentUI.showPaymentPage(this, PAYMENT_REQUEST_CODE);

Page Theming
------------

Theming of the Payment Page is done using the PaymentTheme class and in
order for theming to take effect, the customised PaymentTheme instance
must be set in the PaymentUI class prior to opening the Payment Page.

Code sample how to create and set a custom PaymentTheme:

::

   PaymentTheme.Builder builder = PaymentTheme.createBuilder();
   ...  
   PaymentUI paymentUI = PaymentUI.getInstance();
   paymentUI.setPaymentTheme(builder.build());
   paymentUI.showPaymentPage(this, PAYMENT_REQUEST_CODE);

The PaymentTheme contains a set of parameters defining the customised
theming. When a parameter name ends with Style, the parameter holds a
TextAppearance style resource id used for TextView elements. If the
parameter name ends with Theme then the parameter holds a theme resource
id and is applied during inflation of the UI element.

PageParameters
~~~~~~~~~~~~~~

The PageParameters class contains a collection of parameters used to
theme the page and list.

Code sample how to set the PageParameters in the PaymentTheme:

::

   PageParameters pageParams = PageParameters.createBuilder().
   setPageTheme(R.style.CustomThemePaymentPage).
   ...
   build();
   builder.setPageParameters(pageParams);

Table explaining each page parameter:

+--------------------------+--------------------------------------------+
| Name                     | Purpose                                    |
+==========================+============================================+
| pageTheme                | Main theme of the Payment Page Activity.   |
+--------------------------+--------------------------------------------+
| emptyListLabelStyle      | TextAppearance of label shown when the     |
|                          | list of payment methods is empty           |
+--------------------------+--------------------------------------------+
| sectionHeaderLabelStyle  | TextAppearance of section header label in  |
|                          | the list, i.e. “Saved accounts”            |
+--------------------------+--------------------------------------------+
| networkCardTitleStyle    | TextAppearance of network card title,      |
|                          | i.e. “Visa”                                |
+--------------------------+--------------------------------------------+
| accountCardTitleStyle    | TextAppearance of account card title,      |
|                          | i.e. “41 \**\* 1111”                       |
+--------------------------+--------------------------------------------+
| accountCardSubtitleStyle | TextAppearance of account card subtitle,   |
|                          | i.e. the expiry date “01 / 2032”           |
+--------------------------+--------------------------------------------+
| paymentLogoBackground    | Background resource ID drawn behind        |
|                          | payment method images                      |
+--------------------------+--------------------------------------------+

WidgetParameters
~~~~~~~~~~~~~~~~

The WidgetParameters contains a collection of parameters used to theme
widgets. Widgets are UI elements handling user input, i.e. TextInput,
CheckBoxes Select options. Below is a table explaining each parameter.

The WidgetParameters class allow setting individual drawable resource
ids for icons by using the putInputTypeIcon() method, use the
setDefaultIconMapping() method to use the icons provided by the Payment
SDK.

Code sample how to set the WidgetParameters in the PaymentTheme:

::

   WidgetParameters widgetParams = WidgetParameters.createBuilder().
   setTextInputTheme(R.style.CustomThemeTextInput).
   ...
   build();
   builder.setWidgetParameters(widgetParams);

Table explaining each widget parameter:

+-----------------------------+--------------------------------------------+
| Name                        | Purpose                                    |
+=============================+============================================+
| textInputTheme              | Theme for TextInputLayout elements         |
+-----------------------------+--------------------------------------------+
| buttonTheme                 | Theme for action button in each payment    |
|                             | card                                       |
+-----------------------------+--------------------------------------------+
| buttonLabelStyle            | TextAppearance of label inside the action  |
|                             | button                                     |
+-----------------------------+--------------------------------------------+
| buttonBackground            | Background resource ID of action button    |
+-----------------------------+--------------------------------------------+
| checkBoxTheme               | Theme for checkBox UI element              |
+-----------------------------+--------------------------------------------+
| checkBoxLabelCheckedStyle   | TextAppearance of label when checkBox is   |
|                             | checked                                    |
+-----------------------------+--------------------------------------------+
| checkBoxLabelUncheckedStyle | TextAppearance of label when checkBox is   |
|                             | unchecked                                  |
+-----------------------------+--------------------------------------------+
| selectLabelStyle            | TextAppearance of label shown above        |
|                             | SelectBox                                  |
+-----------------------------+--------------------------------------------+
| validationColorOk           | Color resource ID indicating successful    |
|                             | validation state                           |
+-----------------------------+--------------------------------------------+
| validationColorUnknown      | Color resource ID indicating unknown       |
|                             | validation state                           |
+-----------------------------+--------------------------------------------+
| validationColorError        | Color resource ID indicating error         |
|                             | validation state                           |
+-----------------------------+--------------------------------------------+
| hintDrawable                | Drawable resource ID of the hint icon for  |
|                             | verification codes                         |
+-----------------------------+--------------------------------------------+

DialogParameters
~~~~~~~~~~~~~~~~

The DialogParameters in the PaymentTheme holds parameters to theme popup
dialog windows. The SDK contain two different dialogs, the
DateDialog for setting expiry dates and MessageDialog to show warnings
and errors.

Code sample how to set the DialogParameters in the PaymentTheme:

::

   DialogParameters dialogParams = DialogParameters.createBuilder().
   setDateTitleStyle(R.style.CustomText_Medium).
   ...
   build();
   builder.setDialogParameters(dialogParams);

Table explaining each dialog parameter:

+-----------------------------+--------------------------------------------+
| Name                        | Purpose                                    |
+=============================+============================================+
| dialogTheme                 | Theme for Dialogs, i.e. message and date   |
|                             | dialogs                                    |
+-----------------------------+--------------------------------------------+
| dateTitleStyle              | TextAppearance of title in DateDialog      |
+-----------------------------+--------------------------------------------+
| dateSubtitleStyle           | TextAppearance of subtitle in DateDialog   |
+-----------------------------+--------------------------------------------+
| messageTitleStyle           | TextAppearance of title in MessageDialog   |
+-----------------------------+--------------------------------------------+
| messageDetailsStyle         | TextAppearance of message in MessageDialog |
+-----------------------------+--------------------------------------------+
| messageDetailsNoTitleStyle  | TextAppearance of message in MessageDialog |
|                             | without title                              |
+-----------------------------+--------------------------------------------+
| buttonLabelStyle            | TextAppearance of action button for Date   |
|                             | and MessageDialogs                         |
+-----------------------------+--------------------------------------------+
| imageLabelStyle             | TextAppearance of the image prefix &       |
|                             | suffix labels in MessageDialog             |
+-----------------------------+--------------------------------------------+
| snackbarTextStyle           | TextAppearance of the text label inside a  |
|                             | Snackbar                                   |
+-----------------------------+--------------------------------------------+

ProgressParameters
~~~~~~~~~~~~~~~~~~

The ProgressParameters in the PaymentTheme hold parameters to theme
progress animations shown when loading lists or sending charge/preset requests
to the Payment API.

Code sample how to set the ProgressParameters in the PaymentTheme:

::

   ProgressParameters progressParams = ProgressParameters.createBuilder().
   setLoadProgressBarColor(R.color.customColorPrimary).
   ...
   build();
   builder.setProgressParameters(progressParams);

Table explaining each progress parameter:

+---------------------------+--------------------------------------------+
| Name                      | Purpose                                    |   
+===========================+============================================+
| loadBackground            | Background resource ID of the loading page |
+---------------------------+--------------------------------------------+
| loadProgressBarColor      | Indeterminate ProgressBar color resource   |
|                           | ID                                         | 
+---------------------------+--------------------------------------------+
| sendBackground            | Background resource ID of the loading page |
+---------------------------+--------------------------------------------+
| sendProgressBarColorFront | Determinate ProgressBar front color        |
|                           | resource ID                                | 
+---------------------------+--------------------------------------------+
| sendProgressBarColorBack  | Determinate ProgressBar back color         |
|                           | resource ID                                | 
+---------------------------+--------------------------------------------+
| headerStyle               | TextAppearance of header in the send       |
|                           | progress screen                            | 
+---------------------------+--------------------------------------------+
| infoStyle                 | TextAppearance of info in the send         |
|                           | progress screen                            | 
+---------------------------+--------------------------------------------+

Grouping of Payment Methods
===========================

The SDK supports grouping of payment methods within a card in the payment page. 
By default the SDK supports one group which contains the payment methods Visa, 
Mastercard and American Express.
The default grouping of payment methods in the Payment SDK is defined in `groups.json <./payment/src/main/res/raw/groups.json>`_.

Customise grouping
------------------

The SDK allow customisation which payment methods are grouped
together in a card. Customisation is done by setting the resource ID of
a grouping Json settings file in the SDK prior to showing the payment
page. Payment methods can only be grouped together when they
have the same set of InputElements. If InputElements of grouped
Payment Methods differ then each Payment Method will be shown in its own
card in the payment page. The following example shows how to create two
groups, first group contains Mastercard and Amex and the second group
contains Visa and Visa Electron.

Example customgroups.json file:

::

   [
       {
           "items": [
               {
                   "code": "MASTERCARD",
                   "regex": "^5[0-9]*$"
               },
               {
                   "code": "AMEX",
                   "regex": "^3[47][0-9]*$"
               }
           ]
       },
       {
           "items": [
               {
                   "code": "VISA",
                   "regex": "^4[0-9]*$"
               },
               {
                   "code": "VISAELECTRON",
                   "regex": "^4[0-9]*$"
               }
           ]
       }
   ]

Code sample how to set a customgroups.json file:

::

   PaymentUI paymentUI = PaymentUI.getInstance();
   paymentUI.setGroupResId(R.raw.customgroups);
   paymentUI.showPaymentPage(this, PAYMENT_REQUEST_CODE);

Disable grouping
----------------

If each payment method should be placed in a separate card then this can
be achieved by providing a grouping Json settings file with an empty
array.

Example disablegroups.json file:

::

   []

Code sample how to set the disabledgroups.json file:

::

   PaymentUI paymentUI = PaymentUI.getInstance();
   paymentUI.setGroupResId(R.raw.disablegroups);
   paymentUI.showPaymentPage(this, PAYMENT_REQUEST_CODE);

Smart Selection
---------------

The choice which payment method in a group is displayed and used for
charge requests is done by “Smart Selection”. Each payment method in a
group contains a Regular Expression that is used to “smart select” this
method based on the partially entered card number. While the
user types the number, the SDK will validate the partial number with the
regular expression. When one or more payment methods match the number
input they will be highlighted.

Table containing the rules of Smart Selection:

+-------------------------+--------------------------------------------+
| Name                    | Purpose                                    |
+=========================+============================================+
| No payment method regex | The first payment method in the group is   |
| match the number input  | displayed and is used to validate input    |
| value.                  | values and perform Charge/Preset requests. |
+-------------------------+--------------------------------------------+
| Two or more payment     | The first matching payment method is       |
| method regex match the  | displayed and is used to validate input    |
| number input value      | values and perform Charge/Preset requests. |
+-------------------------+--------------------------------------------+
| One payment method      | This payment method is displayed and is    |
| regex match the number  | used to validate input values and          |
| input value.            | perform Charge/Preset requests.            |
+-------------------------+--------------------------------------------+

Input Validation
================

The Android SDK validates all input values provided by the user before all charge/preset requests. 
The file `validations.json <./payment/src/main/res/raw/validations.json>`_ contains the regular expression
definitions that the Payment SDK uses to validate numbers, verificationCodes, bankCodes and holderNames. 
Validations for other input values i.e. expiryMonth and expiryYear are defined by the `Validator.java <./payment/src/main/java/net/optile/payment/validation/Validator.java>`_.

Customise validations
---------------------

The Payment SDK allow customisation of validations applied to certain input types. 

- Validation for number, bankCode, holderName and verificationCode can be customised with the "regex" parameter.
- Input fields can be hidden by setting the "hide" parameter is true.
- The maximum input length can be set with the "maxLength" parameter.

Customised validations can be set by providing the resource ID of the validation Json file to the
PaymentUI class prior to showing the payment page. The default validation provided by the Android Payment SDK are sufficient in most cases.

Example customvalidations.json file:

::

    [{
        "code": "VISA",
        "items": [
            {
                "type": "number",
                "regex": "^4(?:[0-9]{12}|[0-9]{15}|[0-9]{18})$"
            },
            {
                "type": "verificationCode",
                "regex": "^[0-9]{3}$",
                "maxLength": 3
            }
        ]
    },
    {
        "code": "SEPADD",
        "items": [
             {
                 "type": "bic",
                 "hide": true
             }
         ]
     }
    ...
   ]

Code sample how to set the customvalidations.json file:

::

   PaymentUI paymentUI = PaymentUI.getInstance();
   paymentUI.setValidationResId(R.raw.customvalidations);
   paymentUI.showPaymentPage(this, PAYMENT_REQUEST_CODE);