package com.example.vinade.myapplication;

import android.app.Activity;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by vinade on 05/04/18.
 */

public class ApiController extends RequestController {

    public ApiController(String host, Context context) {
        super(host, context);
    }

    public ApiController(String host, Activity activity) {
        super(host, activity);
    }

    /***
     *
     *  MÉTODOS
     *
     */

    public void saveUser(String name, Integer age, AsyncResponse onResponse){
        JSONObject data = new JSONObject();
        try {
            data.put("name", name);
            data.put("age", age);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        this.request("/save-user", null, data, onResponse);
    }

    public void getUsers(AsyncResponse onResponse){
        this.request("/get-users", null, null, onResponse);
    }
}
