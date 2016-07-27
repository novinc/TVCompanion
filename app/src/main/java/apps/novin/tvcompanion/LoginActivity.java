package apps.novin.tvcompanion;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.AccessToken;

import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;

import java.io.IOException;

import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    TraktV2 traktV2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        traktV2 = new TraktV2(BuildConfig.API_KEY, BuildConfig.CLIENT_SECRET, "tvcompanion.novin.apps://oauthredirect");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getIntent() != null) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    Uri uri = getIntent().getData();
                    if (uri != null && uri.toString().startsWith("tvcompanion.novin.apps://oauthredirect")) {
                        String code = uri.getQueryParameter("code");
                        String state = uri.getQueryParameter("state");
                        if (state.equals("Logged Out")) {
                            Response<AccessToken> accessTokenResponse;
                            try {
                                accessTokenResponse = traktV2.exchangeCodeForAccessToken(code);
                            } catch (IOException e) {
                                e.printStackTrace();
                                Log.d("Login", "couldn't exchange code for token");
                                return;
                            }

                            AccessToken accessToken = accessTokenResponse.body();
                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putBoolean("loggedIn", true);
                            editor.putString("access_token", accessToken.access_token);
                            editor.putString("refresh_token", accessToken.refresh_token);
                            editor.apply();
                            Log.d("Login", "logged in successfully");
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            intent.putExtra("from_login", true);
                            startActivity(intent);
                            finish();
                        } else {
                            Log.d("Login", "FORGED. This shouldn't happen");
                        }
                    }
                }
            });
        }
    }

    public void onLogIn(View view) throws OAuthSystemException {
        OAuthClientRequest request = traktV2.buildAuthorizationRequest("Logged Out");
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse(request.getLocationUri()));
        startActivity(intent);
    }
}
