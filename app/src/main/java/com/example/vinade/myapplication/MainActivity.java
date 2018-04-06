package com.example.vinade.myapplication;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    final private static String TAG = "Main";
    ApiController apiController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        apiController = new ApiController("http://10.51.1.21:5000/api", this);

        // exemplo 1 de uso
        apiController.getUsers(new AsyncResponse() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d(TAG, "getUsers - onSuccess");
                if (response != null){
                    Log.d(TAG, response.toString());
                }
            }

            @Override
            public void onError(JSONObject error) {
                Log.d(TAG, "getUsers - onError");
                if (error != null){
                    Log.d(TAG, error.toString());
                }
            }
        });

        // exemplo 2 de uso
        apiController.saveUser("Sérgio", 57, new AsyncResponse() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d(TAG, "saveUser - onSuccess");
                if (response != null){
                    Log.d(TAG, response.toString());
                }
            }

            @Override
            public void onError(JSONObject error) {
                Log.d(TAG, "saveUser - onError");
                if (error != null){
                    Log.d(TAG, error.toString());
                }
            }
        });
    }
}
