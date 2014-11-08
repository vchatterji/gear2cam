package com.gear2cam.official;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.facebook.FacebookRequestError;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;
import com.gear2cam.official.util.SystemUiHider;
import com.parse.LogInCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.accessory.SA;

import java.util.Arrays;
import java.util.List;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class InfoActivity extends FragmentActivity implements OnLoginClickedListener {
    //The TAG for logging
    private static final String TAG = "InfoActivity";

    private boolean isLoggingIn = false;
    private boolean isAskingPermissions = false;

    private boolean isEulaVisible = false;

    Fragment currentFragment = null;
    Activity mainActivity;

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(new CalligraphyContextWrapper(newBase));
    }

    @Override
    protected void onResume() {
        super.onResume();
        setFragment();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen

        /*
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
        }
        */
        setFragment();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try
        {
            if(isLoggingIn) {
                ParseFacebookUtils.finishAuthentication(requestCode, resultCode, data);
            }
            else if(isAskingPermissions) {
                Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
                ParseFacebookUtils.getSession().refreshPermissions();
            }
        }
        catch (Exception ex)
        {
            //Do not cause any crashes if session does not exist for example
        }
    }

    @Override
    public void onLoginClicked() {
        List<String> permissions = Arrays.asList("email");
        AppAnalytics.trackAppEvent("facebooklogin");
        ParseFacebookUtils.logIn(permissions, this, new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException err) {
                boolean hasPublishPermissions;
                if (user == null) {
                    AlertDialog dlg = new AlertDialog.Builder(InfoActivity.this)
                            .setTitle(getString(R.string.facebook_login_failed_title))
                            .setMessage(getString(R.string.face_login_failed_content))
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            })
                            .create();
                    dlg.show();
                    AppAnalytics.trackAppEvent("facebookloginfailed");
                } else if (user.isNew()) {
                    AppAnalytics.trackAppEvent("facebookloginsuccess");
                    Log.d(TAG,
                            "User signed up and logged in through Facebook!");
                    hasPublishPermissions = ParseFacebookUtils.getSession().isPermissionGranted("publish_actions");
                    if(hasPublishPermissions) {
                        AppAnalytics.trackAppEvent("facebookpublishenabled");
                        Settings.setPublishingEnabled(getApplicationContext(), true);
                        Settings.setPublishDialogDisabled(getApplicationContext(), true);
                    }
                    setFragment();
                } else {
                    AppAnalytics.trackAppEvent("facebookloginsuccess");
                    Log.d(TAG,
                            "User logged in through Facebook!");
                    hasPublishPermissions = ParseFacebookUtils.getSession().isPermissionGranted("publish_actions");
                    if(hasPublishPermissions) {
                        AppAnalytics.trackAppEvent("facebookpublishenabled");
                        Settings.setPublishingEnabled(getApplicationContext(), true);
                        Settings.setPublishDialogDisabled(getApplicationContext(), true);
                    }
                    setFragment();
                }
            }
        });
        isLoggingIn = true;
    }

    private void unAuthenticate() {
        ParseUser.logOut();
        Settings.setUserEmail(getApplicationContext(), "");
        Settings.setPublishingEnabled(getApplicationContext(), false);
        Settings.setPublishDialogDisabled(getApplicationContext(), false);
        setFragment();
    }

    private void checkLoggedIn() {
        isLoggingIn = false;
        makeMeRequest();
    }

    public void getPublishPermissions() {
        isAskingPermissions = true;

        Session.NewPermissionsRequest permissionsRequest =
                new Session.NewPermissionsRequest(InfoActivity.this,
                        Arrays.asList("publish_actions"));

        Session session = ParseFacebookUtils.getSession();


        session.addCallback(new Session.StatusCallback()
        {
            @Override
            public void call(
                    Session session,
                    SessionState state,
                    Exception exception)
            {
                if (exception != null
                        || state.equals(SessionState.CLOSED)
                        || state.equals(SessionState.CLOSED_LOGIN_FAILED))
                {
                    // didn't get required permissions
                    if(state.equals(SessionState.CLOSED_LOGIN_FAILED)) {
                        session.close();
                    }

                    session.removeCallback(this);

                    Settings.setPublishingEnabled(getApplicationContext(), false);
                    Settings.setPublishDialogDisabled(getApplicationContext(), true);
                    AppAnalytics.trackAppEvent("facebookpublishdisabled");
                    isAskingPermissions = false;
                    setFragment();

                }
                else if (state.equals(SessionState.OPENED_TOKEN_UPDATED))
                {
                    // got required permissions

                    session.removeCallback(this);

                    isAskingPermissions = false;
                    //ParseFacebookUtils.getSession().refreshPermissions();
                    ParseFacebookUtils.saveLatestSessionData(ParseUser.getCurrentUser());
                    if(ParseFacebookUtils.getSession().getPermissions().contains("publish_actions"))
                    {
                        //granted permission
                        Log.v(TAG, "Granted permission");
                        AppAnalytics.trackAppEvent("facebookpublishenabled");
                        Settings.setPublishingEnabled(getApplicationContext(), true);
                        Settings.setPublishDialogDisabled(getApplicationContext(), true);
                        isAskingPermissions = false;
                        setFragment();
                    }
                    else
                    {
                        //not granted permission
                        Log.v(TAG,"Not granted permission");
                        AppAnalytics.trackAppEvent("facebookpublishdisabled");
                        Settings.setPublishingEnabled(getApplicationContext(), false);
                        Settings.setPublishDialogDisabled(getApplicationContext(), true);
                        isAskingPermissions = false;
                        setFragment();
                    }
                }
            }
        });

        session.requestNewPublishPermissions(permissionsRequest);
    }

    private void showPermissionsDialog() {
        if(isAskingPermissions) {
            return;
        }

        AlertDialog.Builder adb = new AlertDialog.Builder(this);

        adb.setTitle(getString(R.string.permission_title));
        adb.setMessage(Html.fromHtml(getString(R.string.permission_message)));
        adb.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                getPublishPermissions();
                return;
            }
        });

        adb.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Settings.setPublishDialogDisabled(getApplicationContext(), true);
                Settings.setPublishingEnabled(getApplicationContext(), false);
                isAskingPermissions = false;
                checkLoggedIn();
            }
        });
        isAskingPermissions = true;
        adb.show();
    }

    private void makeMeRequest() {
        Session session = ParseFacebookUtils.getSession();
        if(session != null) {
            boolean hasPublishPermissions = session.isPermissionGranted("publish_actions");
            if(!Settings.isPublishingEnabled(getApplicationContext()) && !hasPublishPermissions &&
                    !Settings.isPublishDialogDisabled(getApplicationContext())) {
                //Ask user for permission to publish photos
                showPermissionsDialog();
                return;
            }

            Request request = Request.newMeRequest(session,
                    new Request.GraphUserCallback() {
                        @Override
                        public void onCompleted(GraphUser user, Response response) {
                            if (user != null) {
                                GraphObject go = response.getGraphObject();
                                if(go != null) {
                                    String email = (String) go.getProperty("email");
                                    ParseUser puser = ParseUser.getCurrentUser();
                                    if(puser != null && email != null) {
                                        if(puser.getEmail() == null || !puser.getEmail().equalsIgnoreCase(email.toLowerCase())) {
                                            puser.setEmail(email);
                                            puser.saveEventually();
                                            Settings.setUserEmail(getApplicationContext(), email);
                                            AppAnalytics.trackAppEvent("emailobtained");
                                        }
                                    }
                                    else {
                                        AppAnalytics.trackAppEvent("emailrejected");
                                    }
                                }
                                // handle success
                            } else if (response.getError() != null) {
                                if ((response.getError().getCategory() ==
                                        FacebookRequestError.Category.AUTHENTICATION_RETRY) ||
                                        (response.getError().getCategory() ==
                                                FacebookRequestError.Category.AUTHENTICATION_REOPEN_SESSION))
                                {
                                    Log.d(TAG,
                                            "The facebook session was invalidated.");
                                    // Log the user out

                                    AppAnalytics.trackAppEvent("facebookrevoked");
                                    unAuthenticate();
                                } else {
                                    Log.d(TAG,
                                            "Some other error: "
                                                    + response.getError()
                                                    .getErrorMessage());
                                }
                            }
                        }
                    });
            request.executeAsync();
        }
    }

    private void setFragment() {
        mainActivity = this;
        Fragment fragment;

        // Check if there is a currently logged in user
        // and they are linked to a Facebook account.
        ParseUser currentUser = ParseUser.getCurrentUser();

        //(
        if ((currentUser != null) && ParseFacebookUtils.isLinked(currentUser)) {
            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                fragment = AuthenticatedFragmentPortrait.newInstance();
            }
            else {
                fragment = AuthenticatedFragmentLandscape.newInstance();
            }
            checkLoggedIn();
            //sendBroadcast(new Intent(Intents.INTENT_CONNECT));
        }
        else {
            if(Settings.isGearAbsent(getApplicationContext())) {
                if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    fragment = GearAbsentFragmentPortrait.newInstance();
                }
                else {
                    fragment = GearAbsentFragmentLandscape.newInstance();
                }
            }
            else {
                if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    fragment = LoginFragmentPortrait.newInstance();
                }
                else {
                    fragment = LoginFragmentLandscape.newInstance();
                }
            }
        }

        if(fragment != null) {
            FragmentManager fm = getFragmentManager();
            FragmentTransaction transaction = fm.beginTransaction();

            if(currentFragment == null) {
                transaction.add(R.id.frame_layout, fragment);
            }
            else {
                // Replace whatever is in the fragment_container view with this fragment,
                // and add the transaction to the back stack
                transaction.replace(R.id.frame_layout, fragment);
                transaction.addToBackStack(null);
            }

            transaction.commitAllowingStateLoss();

            currentFragment = fragment;
        }

        if(!Settings.isEulaAccepted(this) && !isEulaVisible) {
            isEulaVisible = true;
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.eula_title))
                    .setMessage(getString(R.string.eula))
                    .setPositiveButton(R.string.accept,
                            new Dialog.OnClickListener() {

                                @Override
                                public void onClick(
                                        DialogInterface dialogInterface, int i) {
                                    // Mark this version as read.
                                    Settings.setEulaAccepted(mainActivity, true);
                                    // Close dialog
                                    dialogInterface.dismiss();
                                }
                            })
                    .setNegativeButton(android.R.string.cancel,
                            new Dialog.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    // Close the activity as they have declined
                                    // the EULA
                                    isEulaVisible = false;
                                    mainActivity.finish();
                                }
                            });
            builder.create().show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Track app opens
        ParseAnalytics.trackAppOpened(getIntent());
        //ParseUser.logOut();


        if (savedInstanceState == null) {
            final View controlsView = findViewById(R.id.fullscreen_content_controls);
            final View contentView = findViewById(R.id.frame_layout);

            // Set up an instance of SystemUiHider to control the system UI for
            // this activity.
            mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
            mSystemUiHider.setup();
            mSystemUiHider
                    .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                        // Cached values.
                        int mControlsHeight;
                        int mShortAnimTime;

                        @Override
                        @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                        public void onVisibilityChange(boolean visible) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                                // If the ViewPropertyAnimator API is available
                                // (Honeycomb MR2 and later), use it to animate the
                                // in-layout UI controls at the bottom of the
                                // screen.
                                if (mControlsHeight == 0) {
                                    mControlsHeight = controlsView.getHeight();
                                }
                                if (mShortAnimTime == 0) {
                                    mShortAnimTime = getResources().getInteger(
                                            android.R.integer.config_shortAnimTime);
                                }
                                controlsView.animate()
                                        .translationY(visible ? 0 : mControlsHeight)
                                        .setDuration(mShortAnimTime);
                            } else {
                                // If the ViewPropertyAnimator APIs aren't
                                // available, simply show or hide the in-layout UI
                                // controls.
                                controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                            }

                            if (visible && AUTO_HIDE) {
                                // Schedule a hide().
                                delayedHide(AUTO_HIDE_DELAY_MILLIS);
                            }
                        }
                    });

            // Set up the user interaction to manually show or hide the system UI.
            contentView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (TOGGLE_ON_CLICK) {
                        mSystemUiHider.toggle();
                    } else {
                        mSystemUiHider.show();
                    }
                }
            });


            // Upon interacting with UI controls, delay any scheduled hide()
            // operations to prevent the jarring behavior of controls going away
            // while interacting with the UI.
            findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);
            findViewById(R.id.dummy_button).setOnClickListener(mReportProblemListener);

            //Check for Samsung accessory framework
            SA mAccessory = new SA();
            try {
                mAccessory.initialize(getApplicationContext());
                Settings.setGearAbsent(getApplicationContext(), false);
            } catch (SsdkUnsupportedException e) {
                // Error Handling
                Log.e(TAG, "Samsung SDK not present");
                Settings.setGearAbsent(getApplicationContext(), true);
            } catch (Exception e1) {
                Log.e(TAG, "Cannot initialize Accessory package.");
                e1.printStackTrace();
                //
                //  Your application can not use Accessory package of Samsung
                //  Mobile SDK. You application should work smoothly without using
                //  this SDK, or you may want to notify user and close your app
                //  gracefully (release resources, stop Service threads, close UI
                //  thread, etc.)
                //
                Settings.setGearAbsent(getApplicationContext(), true);
            }



            CalligraphyConfig.initDefault("fonts/OpenSans-Regular.ttf");

            //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            setFragment();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    public final static boolean isValidEmail(CharSequence target) {
        if (TextUtils.isEmpty(target)) {
            return false;
        } else {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
        }
    }

    private void saveComment(ParseUser user, String emailText, String commentText) {
        ParseObject userComment = new ParseObject("UserComment");
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        userComment.put("Product", Build.PRODUCT);
        userComment.put("Model", Build.MODEL);
        userComment.put("Android", Build.VERSION.SDK_INT);
        userComment.put("Installation", installation);
        if(user != null) {
            userComment.put("User", user);
        }

        userComment.put("Email", emailText);
        userComment.put("Comment", commentText);
        userComment.saveEventually();
    }

    boolean isSupportDialogShown = false;
    View.OnClickListener mReportProblemListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(!isSupportDialogShown) {
                isSupportDialogShown = true;
                final View dialogview = getLayoutInflater().inflate(R.layout.report_problem, null);
                AlertDialog.Builder dialogbuilder = new AlertDialog.Builder(mainActivity);
                dialogbuilder.setTitle(getString(R.string.support_prompt));
                dialogbuilder.setView(dialogview);
                final AlertDialog dialogDetails = dialogbuilder.create();
                dialogDetails.setIcon(R.drawable.ic_launcher);

                String supportEmail = Settings.getSupportEmail(mainActivity);
                if(!supportEmail.equalsIgnoreCase("")) {
                    EditText emailEditText = (EditText) dialogview.findViewById(R.id.email);
                    emailEditText.setText(supportEmail);
                }



                dialogview.findViewById(R.id.btn_send).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        EditText email = (EditText) dialogview.findViewById(R.id.email);
                        final String emailText = email.getText().toString().trim();

                        EditText comment = (EditText) dialogview.findViewById(R.id.problem);
                        final String commentText = comment.getText().toString().trim();
                        if(!isValidEmail(emailText)) {
                            Toast.makeText(getApplicationContext(), getString(R.string.invalid_email),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if(commentText.equals("")) {
                            Toast.makeText(getApplicationContext(), getString(R.string.no_comment),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Settings.setSupportEmail(mainActivity, emailText);

                        ParseUser user = ParseUser.getCurrentUser();
                        saveComment(user, emailText, commentText);
                        dialogDetails.dismiss();

                        Toast.makeText(getApplicationContext(), getString(R.string.comment_thanks),
                                    Toast.LENGTH_LONG).show();
                    }
                });
                dialogview.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialogDetails.dismiss();
                    }
                });

                dialogDetails.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        isSupportDialogShown = false;
                    }
                });

                dialogDetails.show();
            }
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}
