package apps.novin.tvcompanion;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.AccessToken;

import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    TraktV2 traktV2;
    @BindView(R.id.login_bg)
    ImageView imageView;
    @BindView(R.id.log_in_button)
    Button button;
    @BindView(R.id.log_in_prompt)
    TextView prompt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.kodi);
        Bitmap blurred = blurRenderScript(this,bitmap, 5);
        imageView.setImageBitmap(blurred);
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
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                button.setEnabled(false);
                                prompt.setText(R.string.please_wait);
                            }
                        });
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
                            editor.putBoolean(SyncPreferences.KEY_LOGGED_IN, true);
                            editor.putString(SyncPreferences.KEY_ACCESS_TOKEN, accessToken.access_token);
                            editor.putString(SyncPreferences.KEY_REFRESH_TOKEN, accessToken.refresh_token);
                            editor.apply();
                            Log.d("Login", "logged in successfully");
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            intent.putExtra("from_login", true);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            setResult(RESULT_OK);
                            startActivity(intent);
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
        startActivityForResult(intent, 0);
    }

    // used to blur the background image
    @SuppressLint("NewApi")
    public static Bitmap blurRenderScript(Context context, Bitmap smallBitmap, int radius) {
        try {
            smallBitmap = RGB565toARGB888(smallBitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Bitmap bitmap = Bitmap.createBitmap(
                smallBitmap.getWidth(), smallBitmap.getHeight(),
                Bitmap.Config.ARGB_8888);

        RenderScript renderScript = RenderScript.create(context);

        Allocation blurInput = Allocation.createFromBitmap(renderScript, smallBitmap);
        Allocation blurOutput = Allocation.createFromBitmap(renderScript, bitmap);

        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(renderScript,
                Element.U8_4(renderScript));
        blur.setInput(blurInput);
        blur.setRadius(radius); // radius must be 0 < r <= 25
        blur.forEach(blurOutput);

        blurOutput.copyTo(bitmap);
        renderScript.destroy();

        return bitmap;

    }

    private static Bitmap RGB565toARGB888(Bitmap img) throws Exception {
        int numPixels = img.getWidth() * img.getHeight();
        int[] pixels = new int[numPixels];

        //Get JPEG pixels.  Each int is the color values for one pixel.
        img.getPixels(pixels, 0, img.getWidth(), 0, 0, img.getWidth(), img.getHeight());

        //Create a Bitmap of the appropriate format.
        Bitmap result = Bitmap.createBitmap(img.getWidth(), img.getHeight(), Bitmap.Config.ARGB_8888);

        //Set RGB pixels.
        result.setPixels(pixels, 0, result.getWidth(), 0, 0, result.getWidth(), result.getHeight());
        return result;
    }
}
