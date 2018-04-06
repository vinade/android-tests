package com.example.vinade.myapplication;

/**
 * Created by vinade on 05/04/18.
 */

import org.json.JSONObject;

public interface AsyncResponse {
    void onSuccess(JSONObject response);
    void onError(JSONObject error);
}
