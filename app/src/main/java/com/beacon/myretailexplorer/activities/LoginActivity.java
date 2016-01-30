package com.beacon.myretailexplorer.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.beacon.myretailexplorer.PrefManager;
import com.beacon.myretailexplorer.R;
import com.facebook.CallbackManager;
import com.facebook.FacebookException;
import com.facebook.AccessToken;
import com.facebook.FacebookCallback;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 0;

    LoginButton fbLoginButton;
    CallbackManager callbackManager;
    GoogleApiClient mGoogleApiClient;
    SignInButton googleSignInButton;
    PrefManager pref;
    CoordinatorLayout coordinatorLayout;
    Button btn_continue_retailer;

    private static final String LOGIN_URL = "http://192.168.1.105/myretailexplorer/create_user.php";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_NAME = "name";
    public static final String KEY_GENDER = "gender";
    public static final MediaType FORM_DATA_TYPE = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(this.getApplicationContext());
        setContentView(R.layout.activity_login);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        pref = new PrefManager(this);
        if (pref.isLoggedIn()){
            Intent i = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(i);
            finish();
        }

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.main_coordinator_layout);


        callbackManager = CallbackManager.Factory.create();
        fbLoginButton = (LoginButton) findViewById(R.id.fb_login_btn);
        ArrayList<String> fbPermissions = new ArrayList<>();
        fbPermissions.add("email"); fbPermissions.add("public_profile"); fbPermissions.add("user_location");
        fbLoginButton.setReadPermissions(fbPermissions);
        fbLoginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                AccessToken accessToken = AccessToken.getCurrentAccessToken();
                GraphRequest request = GraphRequest.newMeRequest(
                        accessToken,
                        new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(JSONObject object, GraphResponse response) {

                                try {

                                    if (object.has("email")){

                                        String email = object.getString("email");

                                        new LoginAsyncTask().execute(object.getString("email"),
                                                object.getString("name"), object.getString("gender"));

                                        pref.createLoginSession(object.getString("name"), email, pref.FB_LOGIN_SESSION, Settings.Secure.getString(getApplicationContext().getContentResolver(),
                                                Settings.Secure.ANDROID_ID));

                                        Intent i = new Intent(LoginActivity.this, MainActivity.class);
                                        startActivity(i);
                                        finish();
                                    }else {
                                        Snackbar snackbar = Snackbar
                                                .make(coordinatorLayout, "Cannot Log in without email!", Snackbar.LENGTH_LONG);
                                        snackbar.show();
                                    }

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                            }
                        });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,email,location,gender");
                request.setParameters(parameters);
                request.executeAsync();

            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException e) {

            }
        });




        // Configure sign-in to request the user's ID, email address, and basic profile. ID and
        // basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        // Build a GoogleApiClient with access to GoogleSignIn.API and the options above.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        googleSignInButton = (SignInButton) findViewById(R.id.google_signin_btn);
        googleSignInButton.setSize(SignInButton.SIZE_STANDARD);
        googleSignInButton.setScopes(gso.getScopeArray());
        googleSignInButton.setOnClickListener(this);


        btn_continue_retailer = (Button) findViewById(R.id.btn_continue_retailer);
        btn_continue_retailer.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.google_signin_btn:
                signInwithGoogle();
                break;

            case R.id.btn_continue_retailer:
                break;
        }
    }

    private void signInwithGoogle() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        callbackManager.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from
        //   GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                GoogleSignInAccount acct = result.getSignInAccount();
                // Get account information

                Person person = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);

                String gender = "";

                if (person.getGender() == 0){gender = "male";}
                if (person.getGender() == 1){gender = "female";}


                new LoginAsyncTask().execute(acct.getEmail(),
                        acct.getDisplayName(), gender);

                pref.createLoginSession(acct.getDisplayName(), acct.getEmail(), pref.GOOGLE_LOGIN_SESSION, Settings.Secure.getString(getApplicationContext().getContentResolver(),
                        Settings.Secure.ANDROID_ID));

                Intent i = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(i);
                finish();

            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private class LoginAsyncTask extends AsyncTask<String, String, Integer> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(String... params) {

            JSONObject jsonObject = new JSONObject();

            int returnCode = 88;
            Boolean result = true;
            String postBody = "";

            try {
                //all values must be URL encoded to make sure that special characters like & | ",etc.
                //do not cause problems
                postBody =    KEY_EMAIL + "=" + URLEncoder.encode(params[0],"UTF-8") +
                        "&" + KEY_NAME + "=" + URLEncoder.encode(params[1],"UTF-8") +
                        "&" + KEY_GENDER + "=" + URLEncoder.encode(params[0],"UTF-8");
                Log.d("postBody: ", postBody);
            } catch (UnsupportedEncodingException ex) {
                result=false;
            }

            /*
            //If you want to use HttpRequest class from http://stackoverflow.com/a/2253280/1261816
            try {
			HttpRequest httpRequest = new HttpRequest();
			httpRequest.sendPost(url, postBody);
		}catch (Exception exception){
			result = false;
		}
            */

            try{
                //Create OkHttpClient for sending request
                OkHttpClient client = new OkHttpClient();
                //Create the request body with the help of Media Type
                RequestBody body = RequestBody.create(FORM_DATA_TYPE, postBody);
                Request request = new Request.Builder()
                        .url(LOGIN_URL)
                        .post(body)
                        .build();
                //Send the request
                Response response = client.newCall(request).execute();

                JSONObject jsonResponse = new JSONObject(response.body().string());

                String jsonData = response.body().string();
                JSONObject responseJSON = new JSONObject(jsonData);
                String message = responseJSON.getString("message");

                returnCode = responseJSON.getInt("returnCode");

                Log.d("POST Response:", message);

            }catch (IOException e){
                Log.d("IOExcptn", e.getMessage());
                result=false;
            } catch (JSONException e) {
                Log.d("JSONExcptn", e.getMessage());
                e.printStackTrace();
            }
            return returnCode;



            /*try {
                jsonObject.put("email", params[0]);
                jsonObject.put("name", params[1]);
                jsonObject.put("gender", params[2]);

                String url = LOGIN_URL;
                String data = jsonObject.toString();
                String result;

                String requestParams =
                                KEY_EMAIL + "=" + params[0]
                        + "&" + KEY_NAME + "=" + params[1]
                        + "&" + KEY_GENDER + "=" + params[2];

                Log.d(TAG, requestParams);

                HttpURLConnection httpcon = (HttpURLConnection) new URL(url).openConnection();
                httpcon.setDoOutput(true);
                httpcon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                httpcon.setRequestProperty("Accept", "application/json");
                httpcon.setRequestMethod("POST");
                httpcon.connect();

                //write
                OutputStream os = httpcon.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(requestParams);
                writer.close();
                os.close();

                //Read
                BufferedReader br = new BufferedReader(new InputStreamReader(httpcon.getInputStream(),"UTF-8"));

                String line = null;
                StringBuilder sb = new StringBuilder();

                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }

                br.close();
                result = sb.toString();

                JSONObject response = new JSONObject(result);
                httpcon.disconnect();

                returnCode = response.getInt("returnCode");
            } catch (JSONException | IOException e) {
                e.printStackTrace();
                Log.d("IOexcptn", e.getMessage());
            }
            Log.d(TAG, "Login returnCode = " + returnCode);
            return returnCode;
            */

        }

        @Override
        protected void onPostExecute(Integer s) {
            super.onPostExecute(s);
        }


    }

}
