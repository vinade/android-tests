package com.example.vinade.myapplication;

/**
 * Created by vinade on 24/09/17.
 */

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.NetworkOnMainThreadException;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class RequestController {

    // constants
    public static final int UNKNOWN = 10;
    public static final int SERVER_ERROR = 500;
    public static final int SERVER_LOGOUT = 500;
    public static final int NOT_FOUND = 404;
    public static final int NOT_ALLOWED = 403;
    private static final String COOKIES_HEADER = "Set-Cookie";
    public static final String TAG = "RequestController";

    public static final int NENHUM = 0;

    // statics
    private static Map<Integer, String> ErrorMessages = new HashMap<Integer, String>();
    private static java.net.CookieManager cookieManager = new java.net.CookieManager();
    public static Map<String, JSONObject> Cache = new HashMap<String, JSONObject>();
    public static String _csrf = null;

    // self
    private RequestController requestController = null;

    private Activity activity = null;
    private Context context;
    public static String host;

    public RequestController(String host, Activity activity){
        this.setActivity(activity);
        this.host = host;
        this.init();
    }

    public RequestController(String host, Context context){
        this.context = context;
        this.host = host;
        this.init();
    }

    private void init(){
        if ( ErrorMessages.size() == 0 ){
            // Mensagens default
            // this.setMessages(...);
        }
        this.requestController = this;
    }

    public void setActivity(Activity activity){
        if (activity != null){
            this.activity = activity;
            this.context = activity;
        }
    }

    public void setMessages(
            String serverError,
            String serverLogout,
            String notFound,
            String notAllowed,
            String unknownError
    ){
        if (serverError != null && !serverError.equals(""))
            ErrorMessages.put(this.SERVER_ERROR, serverError);
        if (serverLogout != null && !serverLogout.equals(""))
            ErrorMessages.put(this.SERVER_LOGOUT, serverLogout);
        if (notFound != null && !notFound.equals(""))
            ErrorMessages.put(this.NOT_FOUND, notFound);
        if (notAllowed != null && !notAllowed.equals(""))
            ErrorMessages.put(this.NOT_ALLOWED, notAllowed);
        if (unknownError != null && !unknownError.equals(""))
            ErrorMessages.put(this.UNKNOWN, unknownError);
    }

    public String getHost(){
        return this.host;
    }

    public void showMessage(final String message){
        if (this.activity != null){
            this.activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast toast = Toast.makeText(requestController.context, message, Toast.LENGTH_LONG);
                    toast.show();
                }
            });
        }
    }

    public void showMessage(final Integer id){
        if (this.activity != null){
            this.activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast toast = Toast.makeText(requestController.context, id, Toast.LENGTH_LONG);
                    toast.show();
                }
            });
        }
    }

    public JSONObject error(Integer code ){
        JSONObject data = new JSONObject();

        try {
            data.put("code", code.toString());
            if (RequestController.ErrorMessages.containsKey(code)){
                data.put("message", RequestController.ErrorMessages.get(code));
            } else {
                data.put("message", "");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return data;
    }

    public String createUrl(String action, JSONObject queryParams)
            throws JSONException, UnsupportedEncodingException {

        String queryString = "";
        ArrayList<String> queryStringParts = new ArrayList<String>();
        String urlString = this.host;
        urlString = urlString + action;

        if (queryParams == null){
            return urlString;
        }

        Iterator<?> keys = queryParams.keys();
        while(keys.hasNext()) {
            String key = (String)keys.next();
            queryStringParts.add(key.toString() + "=" + URLEncoder.encode(queryParams.get(key).toString(), "UTF-8"));
        }
        queryString = TextUtils.join("&", queryStringParts);
        if (!queryString.equals("")) {
            urlString = urlString + "&" + queryString;
        }
        return urlString;
    }

    public void clearCache(){
        Log.d(TAG, "Clearing cache");
        this.Cache.clear();
        this.Cache = new HashMap<>();
    }

    protected void request(String action, JSONObject queryParams, JSONObject postParams, AsyncResponse onResponse) {
        this.request(action, queryParams, postParams, onResponse, true);
    }

    protected void request(String action, JSONObject queryParams, JSONObject postParams, AsyncResponse onResponse, boolean withCache) {
        RequestTask reqTask = new RequestTask(action, queryParams, postParams, onResponse, withCache);
        reqTask.execute((Void) null);
    }

    /**
     *
     *
     *       R E Q U E S T   T A S K
     *
     *
     */


    public class RequestTask extends AsyncTask<Void, Void, Boolean> {

        String action;
        JSONObject queryParams;
        JSONObject postParams;
        AsyncResponse onResponse;
        String cacheKey = "";
        boolean withCache;

        RequestTask(String action, JSONObject queryParams, JSONObject postParams, AsyncResponse onResponse, boolean withCache) {
            Log.d(TAG, "Starting a request");
            this.action = action;
            this.queryParams = queryParams;
            this.postParams = postParams;
            this.onResponse = onResponse;
            this.withCache = withCache;
            this.cacheKey = action + "{q}";
            if (queryParams != null){
                this.cacheKey = this.cacheKey + queryParams.toString();
            }
            this.cacheKey = this.cacheKey + "{p}";
            if (postParams != null){
                this.cacheKey = this.cacheKey + postParams.toString();
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            if (this.withCache && requestController.Cache.containsKey(this.cacheKey)){
                JSONObject cache = requestController.Cache.get(this.cacheKey);
                if (cache != null){
                    Log.d(TAG, "Cached response");
                    onResponse.onSuccess(cache);
                    return true;
                }
            }

            String urlString = null;
            try {
                urlString = requestController.createUrl(this.action, this.queryParams);
            } catch (JSONException e) {
                e.printStackTrace();
                return false;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return false;
            }
            StringBuilder sb = new StringBuilder();
            URL url;
            HttpURLConnection urlConn = null;
            DataOutputStream printout;
            DataInputStream input;

            try {
                Log.d(TAG, "Connecting");
                url = new URL (urlString);
                urlConn = (HttpURLConnection)url.openConnection();
            } catch (IOException e) {
                Log.d(TAG, "Error: Couldn't connect with the server");
                e.printStackTrace();
                triggerError(requestController.error(requestController.NOT_FOUND), onResponse, true);
                return false;
            }

            if (urlConn != null) {
                urlConn.setDoInput(true);
                urlConn.setDoOutput(true);
                urlConn.setUseCaches(false);
                urlConn.setRequestProperty("Content-Type", "application/json");
                if (cookieManager.getCookieStore().getCookies().size() > 0) {
                    urlConn.setRequestProperty("Cookie", TextUtils.join(";",  cookieManager.getCookieStore().getCookies()));
                }

                if (postParams == null ){
                    postParams = new JSONObject();
                }
                if (requestController._csrf != null){
                    Log.d(TAG, "csrf: " + requestController._csrf);
                    try {
                        postParams.put("_csrf", requestController._csrf);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                Log.d(TAG, "COOKIES: " + TextUtils.join(";",  cookieManager.getCookieStore().getCookies()).toString());

                try {
                    urlConn.connect();
                } catch (IOException e) {
                    Log.d(TAG, "Error connecting: "+e.getMessage());
                    e.printStackTrace();
                    triggerError(requestController.error(requestController.NOT_FOUND), onResponse, true);
                    return false;
                } catch (NetworkOnMainThreadException n) {
                    Log.d(TAG, "Error connecting: "+n.getMessage());
                    n.printStackTrace();
                    triggerError(requestController.error(requestController.NOT_FOUND), onResponse, true);
                    return false;
                }
                Log.d(TAG, "post parameters: "+postParams.toString());

                try {
                    printout = new DataOutputStream(urlConn.getOutputStream ());
                    printout.write(postParams.toString().getBytes());
                    printout.flush();
                    printout.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                int HttpResult = 0;
                try {
                    HttpResult = urlConn.getResponseCode();
                    if(HttpResult == HttpURLConnection.HTTP_OK){
                        BufferedReader br = new BufferedReader(new InputStreamReader(
                                urlConn.getInputStream(),"utf-8"));
                        String line = null;
                        while ((line = br.readLine()) != null) {
                            sb.append(line + "\n");
                        }
                        br.close();
                        Log.d(TAG, "Response: "+sb.toString());

                        // RESPONSE BODY
                        JSONObject response = null;
                        try {
                            response = new JSONObject(sb.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                            requestController.clearCache();
                            if (requestController.activity != null){
                                requestController.activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                    triggerError(requestController.error(RequestController.SERVER_ERROR), onResponse, true);
                                    }
                                });
                            }
                            return false;
                        }

                        // SET-COOKIES
                        Map<String, List<String>> headerFields = urlConn.getHeaderFields();
                        List<String> cookiesHeader = headerFields.get(COOKIES_HEADER);
                        if (cookiesHeader != null) {
                            for (String cookie : cookiesHeader) {
                                requestController.cookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
                            }
                        }

                        if (response == null){
                            Log.d(TAG, "Response: null");
                            triggerError(null, onResponse, true);
                            return false;
                        }

                        if (response.getInt("status") != 0){
                            Log.d(TAG, "status: " + response.getInt("status"));
                            Log.d(TAG, "error: " + response.get("error").toString());
                            triggerError(response.getJSONObject("error"), onResponse, false);
                            return false;
                        }

                        JSONObject data = response.getJSONObject("data");

                        if ( data != null ){
                            if (data.has("_csrf")) {
                                String _csrf = data.getString("_csrf");
                                if (_csrf != null) {
                                    requestController._csrf = _csrf;
                                }
                            }
                        }

                        requestController.Cache.put(this.cacheKey, data);
                        onResponse.onSuccess(data);
                        return true;
                    }else{
                        Log.d(TAG, "error response: " + urlConn.getResponseMessage());
                        triggerError(requestController.error(RequestController.SERVER_ERROR), onResponse, true);
                        return false;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    triggerError(null, onResponse, true);
                    return false;
                } catch (JSONException e) {
                    e.printStackTrace();
                    triggerError(null, onResponse, true);
                    return false;
                }

            } else {
                Log.d(TAG, "open connect: " + "urlConn == null");
            }

            Log.d(TAG, "end");
            triggerError(requestController.error(requestController.NOT_FOUND), onResponse, true);
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
        }

        @Override
        protected void onCancelled() {
        }

        private void triggerError(JSONObject error, AsyncResponse onResponse, Boolean system) {
            Integer errorCode = 0;
            String errorMessage = "";

            Log.d(TAG, "Error on response");

            if (error != null){
                Log.d(TAG, "Error: " + error.toString());
                try {
                    errorCode = error.getInt("code");
                    errorMessage = error.getString("message");
                } catch (JSONException e) {
                    e.printStackTrace();
                    errorCode = RequestController.UNKNOWN;
                }
            } else {
                Log.d(TAG, "Error: null");
                error = new JSONObject();
            }

            try {
                error.put("system", system);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (errorCode == RequestController.UNKNOWN && errorMessage.equals("")){
                Log.d(TAG, "Unknown error");
                if (requestController.activity != null) {
                    requestController.activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (RequestController.ErrorMessages.containsKey(RequestController.UNKNOWN)) {
                                showMessage(RequestController.ErrorMessages.get(RequestController.UNKNOWN));
                            }
                        }
                    });
                }
            }

            if (!errorMessage.equals("")) {
                Log.d(TAG, errorMessage);
                final String finalErrorMessage = errorMessage;
                if (requestController.activity != null) {
                    requestController.activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showMessage(finalErrorMessage);
                        }
                    });
                }
            }

            onResponse.onError(error);
        }
    }
}
