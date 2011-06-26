/*
 *  Copyright 2010 Felix Schulze
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package de.felixschulze.acra;

import static org.acra.ACRA.LOG_TAG;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.acra.ACRA;
import org.acra.CrashReportData;
import org.acra.ReportField;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;
import org.acra.util.HttpRequest;

import android.net.Uri;
import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * An JSON ReportSender for ACRA 4 (http://code.google.com/p/acra/)
 *
 * @author Felix Schulze
 */
public class JsonSender implements ReportSender {
    private Uri mFormUri = null;
    private Map<ReportField, String> mMapping = null;
    private static final String CONTENT_TYPE;

    static {
        CONTENT_TYPE = "application/json";
    }

    /**
     * <p>
     * Create a new HttpPostSender instance.
     * </p>
     *
     * @param formUri The URL of your server-side crash report collection script.
     * @param mapping If null, POST parameters will be named with
     *                {@link org.acra.ReportField} values converted to String with
     *                .toString(). If not null, POST parameters will be named with
     *                the result of mapping.get(ReportField.SOME_FIELD);
     */
    public JsonSender(String formUri, Map<ReportField, String> mapping) {
        mFormUri = Uri.parse(formUri);
        mMapping = mapping;
    }

    public void send(CrashReportData report) throws ReportSenderException {

        try {
            URL reportUrl;
            reportUrl = new URL(mFormUri.toString());
            Log.d(LOG_TAG, "Connect to " + reportUrl.toString());

            JSONObject json = createJSON(report);

            sendHttpPost(json.toString(), reportUrl, ACRA.getConfig().formUriBasicAuthLogin(), ACRA.getConfig().formUriBasicAuthPassword());

        } catch (Exception e) {
            throw new ReportSenderException("Error while sending report to Http Post Form.", e);
        }

    }

    private static boolean isNull(String aString) {
        return aString == null || aString == ACRA.NULL_VALUE;
    }

    private JSONObject createJSON(Map<ReportField, String> report) {
        JSONObject json = new JSONObject();

        ReportField[] fields = ACRA.getConfig().customReportContent();
        if (fields.length == 0) {
            fields = ACRA.DEFAULT_REPORT_FIELDS;
        }
        for (ReportField field : fields) {
            try {
                if (mMapping == null || mMapping.get(field) == null) {
                    json.put(field.toString(), report.get(field));
                } else {
                    json.put(mMapping.get(field), report.get(field));
                }
            } catch (JSONException e) {
                Log.e("JSONException", "There was an error creating JSON", e);
            }
        }

        return json;
    }

    //TODO: login + password
    //(isNull(login) ? null : login, isNull(password) ? null : password);
    private void sendHttpPost(String data, URL url, String login, String password) {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpPost httPost = new HttpPost(url.toString());

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

            nameValuePairs.add(new BasicNameValuePair("json", data));

            httPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));

            HttpResponse httpResponse = httpClient.execute(httPost);

            Log.d(LOG_TAG, "Server Status: " + httpResponse.getStatusLine());
            Log.d(LOG_TAG, "Server Response: " + EntityUtils.toString(httpResponse.getEntity()));


        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }
}