package com.example.ungdungchiasecaycanh.ui.auth;

import android.content.Context;

import androidx.activity.ComponentActivity;

import com.example.ungdungchiasecaycanh.R;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import java.util.Arrays;

/**
 * Facebook Login: the access token goes to Firebase, like Google's ID token. The button stays
 * hidden until a Meta app ID is filled in res/values/facebook.xml (see docs/DATA_MODEL.md).
 */
final class FacebookSignInHelper {
    private final CallbackManager callbacks = CallbackManager.Factory.create();

    FacebookSignInHelper(AuthViewModel viewModel) {
        LoginManager.getInstance().registerCallback(callbacks, new FacebookCallback<LoginResult>() {
            @Override public void onSuccess(LoginResult result) {
                viewModel.signInWithFacebook(result.getAccessToken().getToken());
            }

            @Override public void onCancel() {
                viewModel.facebookFailed(0);
            }

            @Override public void onError(FacebookException error) {
                viewModel.facebookFailed(R.string.auth_facebook_error);
            }
        });
    }

    /** Both values are needed to sign in: a numeric app ID and a real client token (no placeholder). */
    static boolean isConfigured(Context context) {
        String id = context.getString(R.string.facebook_app_id);
        String token = context.getString(R.string.facebook_client_token);
        return !id.isEmpty() && id.chars().allMatch(Character::isDigit)
                && !token.isEmpty() && !token.startsWith("REPLACE");
    }

    void signIn(ComponentActivity activity) {
        // Auto-initialisation is off in the manifest (no app events); start the SDK only when used.
        FacebookSdk.fullyInitialize();
        LoginManager.getInstance().logIn(activity, callbacks, Arrays.asList("email", "public_profile"));
    }
}
