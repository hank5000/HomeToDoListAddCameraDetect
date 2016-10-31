package com.example.hankwu.hometodolist_detectversion;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.jwetherell.motion_detection.SensorsActivity;
import com.jwetherell.motion_detection.data.GlobalData;
import com.jwetherell.motion_detection.data.Preferences;
import com.jwetherell.motion_detection.detection.AggregateLumaMotionDetection;
import com.jwetherell.motion_detection.detection.IMotionDetection;
import com.jwetherell.motion_detection.detection.LumaMotionDetection;
import com.jwetherell.motion_detection.detection.RgbMotionDetection;
import com.jwetherell.motion_detection.image.ImageProcessing;
import com.romainpiel.titanic.library.Titanic;
import com.romainpiel.titanic.library.TitanicTextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends SensorsActivity implements EasyPermissions.PermissionCallbacks {

    /*
        use for detect
     */
    private static final String TAG = "MotionDetectionActivity";
    private static SurfaceView preview = null;
    private static SurfaceHolder previewHolder = null;
    private static Camera camera = null;
    private static boolean inPreview = false;
    private static long mReferenceTime = 0;
    private static IMotionDetection detector = null;
    private static BrightnessController brightnessController = null;
    private static volatile AtomicBoolean processing = new AtomicBoolean(false);
    private static TextToSpeech tts = null;
    private boolean bEnableCamera = false;

    /*
        use for to do list
     */
    static GoogleAccountCredential mCredential;
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { SheetsScopes.SPREADSHEETS };

    /*
        UI
     */
    private Button menuBtn = null;
    private Button accountBtn = null;
    private Button cameraStart = null;
    private TextView loginInfo = null;
    public static boolean bDetecting = false;
    private TextView tv = null;
    private LinearLayout ll = null;
    private SwipeMenuListView listView = null;
    private ToDoList toDoList = null;
    private ToDoList.Adapter mAdapter = null;
    private ArrayList<ToDoList.ToDoItem> mToDoItemList;

    private TitanicTextView ttv = null;
    private Titanic titanic = null;
    private int KEEP_TIME = 5000;
    private Handler handler = null;
    private HandlerThread handlerThread = null;
    FloatingActionButton actionB = null;
    public boolean bTTS = true;


    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if (Build.VERSION.SDK_INT < 16) {
//            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        }
//
//        View decorView = getWindow().getDecorView();
//        // Hide both the navigation bar and the status bar.
//        // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
//        // a general rule, you should design your app to hide the status bar whenever you
//        // hide the navigation bar.
//        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                | View.SYSTEM_UI_FLAG_FULLSCREEN;
//        decorView.setSystemUiVisibility(uiOptions);

        setContentView(R.layout.main);

        actionB = (FloatingActionButton) findViewById(R.id.action_b);

        final FloatingActionButton action_refreshtodo = new FloatingActionButton(getBaseContext());
        action_refreshtodo.setTitle("Refresh List");
        action_refreshtodo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new MakeRequestTask(mCredential).execute();
            }
        });

        final FloatingActionButton action_tts_switch = new FloatingActionButton(getBaseContext());
        action_tts_switch.setTitle("Speech Switch");
        action_tts_switch.setIcon(R.drawable.checked);
        action_tts_switch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bTTS = bTTS?false:true;
                if(bTTS)
                    action_tts_switch.setIcon(R.drawable.checked);
                else
                    action_tts_switch.setIcon(R.drawable.cancel);
            }
        });

        final FloatingActionButton actionC = new FloatingActionButton(getBaseContext());
        actionC.setTitle("Hide/Show Action above");
        actionC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionB.setVisibility(actionB.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
                action_tts_switch.setVisibility(action_tts_switch.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
                action_refreshtodo.setVisibility(action_refreshtodo.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
            }
        });

        final FloatingActionsMenu menuMultipleActions = (FloatingActionsMenu) findViewById(R.id.multiple_actions);

        menuMultipleActions.addButton(action_refreshtodo);
        menuMultipleActions.addButton(action_tts_switch);
        menuMultipleActions.addButton(actionC);

        final FloatingActionButton actionA = (FloatingActionButton) findViewById(R.id.action_a);
        actionA.setIcon(R.drawable.cancel);
        actionA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!bDetecting) {
                    detectStart();
                    actionA.setIcon(R.drawable.checked);
                } else {
                    detectStop();
                    ttv.setVisibility(View.INVISIBLE);
                    titanic.cancel();
                    actionA.setIcon(R.drawable.cancel);
                }
                updateDetectButton();
            }
        });

        ttv = (TitanicTextView) findViewById(R.id.titanic_tv);
        ttv.setVisibility(View.INVISIBLE);
        ttv.setTypeface(Typefaces.get(this,"Satisfy-Regular.ttf"));
        titanic = new Titanic();

        handlerThread = new HandlerThread("Titanic TextView");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        handler.post(loadingTask);

        preview = (SurfaceView) findViewById(R.id.preview);
        previewHolder = preview.getHolder();
        previewHolder.addCallback(surfaceCallback);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        if (Preferences.USE_RGB) {
            detector = new RgbMotionDetection();
        } else if (Preferences.USE_LUMA) {
            detector = new LumaMotionDetection();
        } else {
            // Using State based (aggregate map)
            detector = new AggregateLumaMotionDetection();
        }

        ll = (LinearLayout) findViewById(R.id.list);

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        tv = new TextView(this);

        final String accountName = getPreferences(Context.MODE_PRIVATE)
                .getString(PREF_ACCOUNT_NAME, null);

        if(accountName != null) {
            mCredential.setSelectedAccountName(accountName);
            if(isDeviceOnline()) {
                // request "TODOLIST"
                new MakeRequestTask(mCredential).execute();
            }
        }

        loginInfo = (TextView) findViewById(R.id.login);

        accountBtn = (Button) findViewById(R.id.account);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(accountName==null) {
                    loginInfo.setText("Please Log In Account");
                    accountBtn.setText("Set Account");
                    actionB.setTitle("Please Log In Account");
                } else {
                    loginInfo.setText("Log In Account:"+accountName);
                    accountBtn.setText("Change Account");
                    actionB.setTitle("Log In Account:"+accountName+"\nChange Account");

                }
            }
        });
        accountBtn.setVisibility(View.INVISIBLE);

        if(accountName==null) {
            actionB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    connectServiceAndChooseAccount();
                }
            });

        } else {
            actionB.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    connectServiceAndChooseAccount();
                    return false;
                }
            });
        }

        cameraStart = (Button) findViewById(R.id.Camera);
        updateDetectButton();
        cameraStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!bDetecting) {
                    detectStart();
                } else {
                    detectStop();
                    ttv.setVisibility(View.INVISIBLE);
                    titanic.cancel();
                }
                updateDetectButton();
            }
        });
        cameraStart.setVisibility(View.INVISIBLE);

        menuBtn = (Button) findViewById(R.id.menu);
        menuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MakeRequestTask(mCredential).execute();
            }
        });
        menuBtn.setVisibility(View.INVISIBLE);

        mToDoItemList = new ArrayList<>();
        mToDoItemList.add(new ToDoList.ToDoItem());
        mAdapter = new ToDoList.Adapter(this,mToDoItemList);

        toDoList = new ToDoList(this);

        listView = (SwipeMenuListView) findViewById(R.id.listView);
        listView.setMenuCreator(toDoList.getCreator());
        listView.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
                switch (index) {
                    case 0:
                        // open
                        break;
                    case 1:
                        mToDoItemList.remove(position);
                        mAdapter.notifyDataSetChanged();
                        // delete
                        break;
                }
                // false : close the menu; true : not close the menu
                return false;
            }
        });

//        // Right
//        listView.setSwipeDirection(SwipeMenuListView.DIRECTION_RIGHT);
//
//        // Left
//        listView.setSwipeDirection(SwipeMenuListView.DIRECTION_LEFT);

        listView.setAdapter(mAdapter);

        ll.addView(tv);

        if(bEnableCamera) {
            camera = Camera.open(0);
        }


        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.CHINESE);
                }
            }
        });

        brightnessController = new BrightnessController(MainActivity.this);
        brightnessController.setLightTime(KEEP_TIME);

    }

    private void detectStart() {
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        for(int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx,cameraInfo);
            if(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                camera = Camera.open(1);
                camera.setPreviewCallback(previewCallback);
                try {
                    camera.setPreviewDisplay(previewHolder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Camera.Parameters p = camera.getParameters();
                p.setPreviewSize(320,240);
                camera.setParameters(p);
                camera.startPreview();
                bDetecting = true;

                ttv.setVisibility(View.INVISIBLE);
                titanic.start(ttv);


            }
        }


    }

    private void detectStop() {

        camera.stopPreview();
        try {
            camera.setPreviewCallback(null);
            camera.setPreviewDisplay(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        camera.release();
        camera = null;
        bDetecting = false;

        ttv.setVisibility(View.INVISIBLE);
        titanic.cancel();
    }

    private void updateDetectButton() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String cameraBtnText = bDetecting?"Disable Detect":"Enable Detect";
                cameraStart.setText(cameraBtnText);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(bDetecting) {
            detectStop();
        }
        bStop = true;
        mutex.notifyAll();

    }

    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void connectServiceAndChooseAccount() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (!isDeviceOnline()) {
            tv.setText("No network connection available.");
        } else {
            chooseAccount();
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {

        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {

                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }


    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    tv.setText(
                            "This app requires Google Play Services. Please install " +
                                    "Google Play Services on your device and relaunch this app.");
                } else {
                    connectServiceAndChooseAccount();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    final String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        final String selectAccount = accountName;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loginInfo.setText("Log In Account:"+selectAccount);
                                actionB.setTitle("Log In Account:"+selectAccount +"\nChange Account");
                                actionB.setOnClickListener(null);
                                actionB.setOnLongClickListener(new View.OnLongClickListener() {
                                    @Override
                                    public boolean onLongClick(View v) {
                                        connectServiceAndChooseAccount();
                                        return false;
                                    }
                                });
                            }
                        });
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    connectServiceAndChooseAccount();
                }
                break;
        }
    }


    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {

        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPause() {
        super.onPause();

        //tts.shutdown();

    }


    static final String[] english_texts = {"Hello"};
    static final String[] chinese_texts = {"你好"};
    static final String[] texts = null;


    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume() {
        super.onResume();


        bStop = false;
    }

    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {

        /**
         * {@inheritDoc}
         */
        @Override
        public void onPreviewFrame(byte[] data, Camera cam) {
            if (data == null) return;
            Camera.Size size = cam.getParameters().getPreviewSize();
            if (size == null) return;

            if (!GlobalData.isPhoneInMotion()) {
                DetectionThread thread = new DetectionThread(data, size.width, size.height);
                thread.start();
            }
        }
    };

    private SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {

        /**
         * {@inheritDoc}
         */
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if(bEnableCamera) {
                try {
                    camera.setPreviewDisplay(previewHolder);
                    camera.setPreviewCallback(previewCallback);
                } catch (Throwable t) {
                    Log.e("PreviewDemo", "Exception in setPreviewDisplay()", t);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if(bEnableCamera) {
                Camera.Parameters parameters = camera.getParameters();
                Camera.Size size = getBestPreviewSize(width, height, parameters);
                if (size != null) {
                    parameters.setPreviewSize(size.width, size.height);
                    Log.d(TAG, "Using width=" + size.width + " height=" + size.height);
                }
                if (inPreview) {
                    inPreview = false;
                    camera.stopPreview();
                }
                camera.setParameters(parameters);
                camera.startPreview();
                inPreview = true;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if(bDetecting) {
                detectStop();
            }

            // Ignore
            if(bEnableCamera) {
                camera.setPreviewCallback(null);
                if (inPreview) camera.stopPreview();
                inPreview = false;
                camera.release();
                camera = null;
            }
        }
    };

    private static Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea > resultArea) result = size;
                }
            }
        }

        return result;
    }
    static int i =0;


//    private static void onMotionDetected() {
//        new MakeRequestTask(mCredential).execute();
//    }

    private static final class DetectionThread extends Thread {

        private byte[] data;
        private int width;
        private int height;

        public DetectionThread(byte[] data, int width, int height) {
            this.data = data;
            this.width = width;
            this.height = height;
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {

            if (!processing.compareAndSet(false, true)) return;

            // Log.d(TAG, "BEGIN PROCESSING...");
            try {
                // Previous frame
                int[] pre = null;
                if (Preferences.SAVE_PREVIOUS) pre = detector.getPrevious();

                // Current frame (with changes)
                // long bConversion = System.currentTimeMillis();
                int[] img = null;
                if (Preferences.USE_RGB) {
                    img = ImageProcessing.decodeYUV420SPtoRGB(data, width, height);
                } else {
                    img = ImageProcessing.decodeYUV420SPtoLuma(data, width, height);
                }
                // long aConversion = System.currentTimeMillis();
                // Log.d(TAG, "Converstion="+(aConversion-bConversion));

                // Current frame (without changes)
                int[] org = null;
                if (Preferences.SAVE_ORIGINAL && img != null) org = img.clone();

                if (img != null && detector.detect(img, width, height)) {
                    // The delay is necessary to avoid taking a picture while in
                    // the
                    // middle of taking another. This problem can causes some
                    // phones
                    // to reboot.
                    long now = System.currentTimeMillis();
                    if (now > (mReferenceTime + Preferences.PICTURE_DELAY)) {
                        mReferenceTime = now;

                        Bitmap previous = null;
                        if (Preferences.SAVE_PREVIOUS && pre != null) {
                            if (Preferences.USE_RGB) previous = ImageProcessing.rgbToBitmap(pre, width, height);
                            else previous = ImageProcessing.lumaToGreyscale(pre, width, height);
                        }

                        Bitmap original = null;
                        if (Preferences.SAVE_ORIGINAL && org != null) {
                            if (Preferences.USE_RGB) original = ImageProcessing.rgbToBitmap(org, width, height);
                            else original = ImageProcessing.lumaToGreyscale(org, width, height);
                        }

                        Bitmap bitmap = null;
                        if (Preferences.SAVE_CHANGES) {
                            if (Preferences.USE_RGB) bitmap = ImageProcessing.rgbToBitmap(img, width, height);
                            else bitmap = ImageProcessing.lumaToGreyscale(img, width, height);
                        }

//                        tts.speak(chinese_texts[i], TextToSpeech.QUEUE_FLUSH, null);
//                        i++;
//                        if(i==chinese_texts.length) {
//                            i = 0;
//                        }
                        brightnessController.lightThenDark();
                        synchronized (mutex) {
                            mutex.notifyAll();
                        }
                    } else {
                        //Log.i(TAG, "Not taking picture because not enough time has passed since the creation of the Surface");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                processing.set(false);
            }
            // Log.d(TAG, "END PROCESSING...");

            processing.set(false);
        }
    };

    public static Object mutex = new Object();
    boolean bStop = false;

    private Runnable loadingTask = new Runnable() {
        @Override
        public void run() {
            while(!bStop) {
                synchronized (mutex) {
                    try {
                        mutex.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ttv.setVisibility(View.INVISIBLE);
                        titanic.cancel();
                    }
                });

                try {
                    Thread.sleep(KEEP_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(bDetecting) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ttv.setVisibility(View.VISIBLE);
                            titanic.start(ttv);
                        }
                    });
                }
            }

        }
    };

    /**
     * An asynchronous task that handles the Google Sheets API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, List<List<Object>>> {
        private com.google.api.services.sheets.v4.Sheets mService = null;
        private Exception mLastError = null;

        public MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.sheets.v4.Sheets.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Sheets API Android Quickstart")
                    .build();


        }

        /**
         * Background task to call Google Sheets API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<List<Object>> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of names and majors of students in a sample spreadsheet:
         * https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
         * https://docs.google.com/spreadsheets/d/1gKAQYKyyn8pXEAQedQOvQRcXiTIcFAtN7bSfklqE5t4/edit
         * @return List of names and majors
         * @throws IOException
         */
        String sheetPath = "1o4mF8JwHMfhiVfeO1ste1VIsBMNOvKlSanjEYNfj1mE";
        String sheetRange = "A2:D";

        private List<List<Object>> getDataFromApi() throws IOException {
            String spreadsheetId = sheetPath;
            String range = sheetRange;

            ValueRange response = this.mService.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute();


            List<List<Object>> values = response.getValues();


            return values;
        }
//
//        final int STATUS = 0;
//        final int TITLE  = 1;
//        final int GROUP  = 2;
//        final int DEADLINE = 3;
//
//        private void setStatusValueToListIndex(String onoff, int index) {
//            setValueAt(onoff,index,STATUS);
//        }
//
//        private void setValueAt(final String value,final int itemIndex,final int itemTitle) {
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    List<Request> requests = new ArrayList<>();
//                    List<CellData> values = new ArrayList<CellData>();
//
//                    values.add(new CellData().setUserEnteredValue(new ExtendedValue()
//                            .setStringValue(value)));
//
//                    requests.add(new Request().setUpdateCells(
//                                    new UpdateCellsRequest()
//                                        .setStart(new GridCoordinate().setSheetId(0)
//                                            .setColumnIndex(itemTitle)
//                                            .setRowIndex(itemIndex))
//                                        .setRows(Arrays.asList(new RowData().setValues(values)))
//                                        .setFields("userEnteredValue")));
//
//                    BatchUpdateSpreadsheetRequest batchUpdateRequest = new BatchUpdateSpreadsheetRequest()
//                            .setRequests(requests);
//
//                    try {
//                        mService.spreadsheets().batchUpdate(sheetPath,batchUpdateRequest).execute();
//
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }).start();
//        }

        @Override
        protected void onPreExecute() {
            //tv.setText("");
        }

        @Override
        protected void onPostExecute(List<List<Object>> output) {
            if(output!=null) {
                mToDoItemList.clear();
                int j = 1;
                for (List<Object> item : output) {
                    if (!item.get(0).toString().toLowerCase().contains("done")
                            && !item.get(0).toString().toLowerCase().contains("complete")
                            && !item.get(0).toString().toLowerCase().contains("完成")
                            && !item.get(0).toString().toLowerCase().contains("解決")) {
                        ToDoList.ToDoItem todoItem = new ToDoList.ToDoItem();
                        todoItem.mStatus = item.get(0).toString();
                        todoItem.mTitle = item.get(1).toString();
                        todoItem.mGroup = item.get(2).toString();
                        todoItem.mDeadline = item.get(3).toString();
                        todoItem.mSheetPosition = j;
                        mToDoItemList.add(todoItem);
                        Log.d(TAG, todoItem.mStatus + "," + todoItem.mTitle + "," + todoItem.mGroup + "," + todoItem.mDeadline + "," + todoItem.mSheetPosition);
                    }
                    j++;

                    mAdapter.notifyDataSetChanged();
                }
                if(bTTS && !tts.isSpeaking()) {
                    ttsSpeakTasks(mToDoItemList);
                }
            }
        }

        boolean bWait = false;
        long waitSec = 1000*1000*10;

        private synchronized void ttsSpeakTasks (ArrayList<ToDoList.ToDoItem> list) {
            if(!bWait) {
                bWait = true;
                tts.speak("您有 " + list.size() + " 個事項", TextToSpeech.QUEUE_ADD, null);
                int i = 1;
                for (ToDoList.ToDoItem item : list) {
                    tts.speak("事項" + i, TextToSpeech.QUEUE_ADD, null);

                    String[] titles = item.mTitle.split("\"");
                    int j = 0;
                    for (String t : titles) {
                        int a = (int) t.charAt(0);
                        if (a > 256) tts.setLanguage(Locale.CHINESE);
                        else tts.setLanguage(Locale.ENGLISH);

                        tts.speak(t, TextToSpeech.QUEUE_ADD, null);
                    }

                    tts.setLanguage(Locale.CHINESE);
                    tts.speak(",狀態:" + item.mStatus + ",截止時間:" + item.mDeadline, TextToSpeech.QUEUE_ADD, null);
                    i++;
                }
                tts.speak("報告完畢", TextToSpeech.QUEUE_ADD, null);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(waitSec, 0);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        bWait = false;
                    }
                }).start();
            }
        }

        @Override
        protected void onCancelled() {
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity2.REQUEST_AUTHORIZATION);
                } else {
                    tv.setText("The following error occurred:\n"
                            + mLastError.getMessage());
                }
            } else {
                tv.setText("Request cancelled.");
            }
        }
    }

}