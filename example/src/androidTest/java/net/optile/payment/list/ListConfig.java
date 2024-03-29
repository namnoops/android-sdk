/*
 * Copyright (c) 2019 optile GmbH
 * https://www.optile.net
 *
 * This file is open source and available under the MIT license.
 * See the LICENSE file for more information.
 */

package net.optile.payment.list;

import org.json.JSONException;
import org.json.JSONObject;

public class ListConfig {

    private final JSONObject source;

    public ListConfig(JSONObject source) {
        this.source = source;
    }

    public void setPrice(String price) throws JSONException {
        JSONObject payment = source.getJSONObject("payment");
        payment.put("amount", price);
    }

    public void setLanguage(String lang) throws JSONException {
        JSONObject language = source.getJSONObject("style");
        language.put("language", lang);
    }

    public String toJsonString() {
        return source.toString();

    }
}
