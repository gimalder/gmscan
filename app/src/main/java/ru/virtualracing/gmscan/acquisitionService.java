package ru.virtualracing.gmscan;

import org.prowl.torque.remote.ITorqueService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.app.AlertDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.util.TypedValue;
import android.widget.ScrollView;
import android.widget.TextView;

public class acquisitionService extends Service {

    private ITorqueService torqueService;
    private int lock = -7;
    private boolean initialized = false;
    private boolean dbg = false;
    
    public acquisitionService() {
    }


    final String LOG_TAG = "myLogs";
    ExecutorService es;
    Object someRes;
    private Handler handler;

    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "MyService onCreate");
        es = Executors.newFixedThreadPool(1);
        someRes = new Object();
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "MyService onDestroy");
        someRes = null;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "MyService onStartCommand");
        int time = intent.getIntExtra("time", 1);
        MyRun mr = new MyRun(time, startId);
        es.execute(mr);


        // Bind to the torque service
        Intent intentTorque = new Intent();
        intent.setClassName("org.prowl.torque", "org.prowl.torque.remote.TorqueService");

        boolean successfulBind = bindService(intentTorque, connection, 0);

        if (successfulBind) {

            // Not really anything to do here.  Once you have bound to the service, you can start calling
            // methods on torqueService.someMethod()  - look at the aidl file for more info on the calls
            // textView.setText("Connected to Torque plugin service");

        } else {
            Log.d(LOG_TAG, "Unable to connect to Torque plugin service");
        }

        return super.onStartCommand(intent, flags, startId);
    }



    
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    class MyRun implements Runnable {

        int time;
        int startId;

        public MyRun(int time, int startId) {
            this.time = time;
            this.startId = startId;
            Log.d(LOG_TAG, "MyRun#" + startId + " create");
        }

        public void run() {
            Log.d(LOG_TAG, "MyRun#" + startId + " start, time = " + time);
            try {
                TimeUnit.SECONDS.sleep(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                Log.d(LOG_TAG, "MyRun#" + startId + " someRes = " + someRes.getClass() );
            } catch (NullPointerException e) {
                Log.d(LOG_TAG, "MyRun#" + startId + " error, null pointer");
            }
            stop();
        }

        void stop() {
            Log.d(LOG_TAG, "MyRun#" + startId + " end, stopSelf(" + startId + ")");
            stopSelf(startId);
        }
    }



    private String mySendCommand(String command){
        try {

            if (!torqueService.hasFullPermissions()) {
                if (dbg)
                    Log.d(LOG_TAG, "no full permission");
                return "no full permission";
            }
            if ((lock != 0) && (lock != -2)) {
                if (dbg)
                    Log.d(LOG_TAG, "no exclusive lock");
                lock = torqueService.requestExclusiveLock("torquescan");
            }
            if ((lock != 0) && (lock != -2)) {
                if (dbg)
                    Log.d(LOG_TAG, "still no exclusive lock");
                return "no exclusive lock";
            }

            //if (!torqueService.isConnectedToECU()) {
            //    Log.d(LOG_TAG, "not connected to ECU");
            //    return "not connected to ECU";
            //}

            String[] response;
            //Log.d(LOG_TAG, "sending: " + command);
            response = torqueService.sendCommandGetResponse("", command);
            if (response != null) {
                //Log.d(LOG_TAG, TextUtils.join(", ", response));
                return TextUtils.join(", ", response);
            }
            else
                return "no response";

        } catch (RemoteException e) {
            return "exception";
        }
    }

    public String lockdescription(int lock) {
        String ans = "";
        switch (lock) {
            /*   0 = Lock gained OK - you now have exclusive access
             *  -1 = Lock failed due to unknown reason
             *  -2 = Lock failed due to another lock already being present (from your, or another plugin)
             *  -3 = Lock failed due to not being connected to adapter
             *  -4 = reserved
             *  -5 = No permissions to access in this manner (enable the ticky-box in the plugin settings for 'full access')
             *  -6 = Lock failed due to timeout (10 second limit hit) */
            case  0: ans = "Lock gained OK"; break;
            case -1: ans = "Lock failed due to unknown reason"; break;
            case -2: ans = "Lock failed due to another lock already being present"; break;
            case -3: ans = "Lock failed due to not being connected to adapter"; break;
            case -4: ans = ""; break;
            case -5: ans = "No permissions to access in this manner"; break;
            case -6: ans = "Lock failed due to timeout (10 second limit hit)"; break;
        }
        return ans;

    }

    private boolean initialize() {
        if (mySendCommand("ATZ").contains("ELM327") == false) return false;
        if (mySendCommand("ATAL").contains("OK") == false) return  false;
        if (mySendCommand("ATE0").contains("OK") == false) return false;
        if (mySendCommand("ATSH8218F1").contains("OK") == false) return false;
        //final Button button3 = (Button) findViewById(R.id.button3);
        //button3.setVisibility(View.VISIBLE);;
        return true;
    }    
    
    private ServiceConnection connection = new ServiceConnection() {

        public void onServiceConnected(ComponentName arg0, IBinder service) {
            torqueService = ITorqueService.Stub.asInterface(service);

            try {

                if (torqueService.getVersion() < 19) {

                    Log.d(LOG_TAG, "Incorrect version You are using an old version of Torque with this plugin.\n\nThe plugin needs the latest version of Torque to run correctly.\n\nPlease upgrade to the latest version of Torque from Google Play");
                    return;

                }

                Log.d(LOG_TAG,"Torque API version " + Integer.toString(torqueService.getVersion()));

            } catch(RemoteException e) {

            }

            try {

                if (torqueService.hasFullPermissions()) {

                    Log.d(LOG_TAG, "Full permissions granted for plugin");

                    lock = torqueService.requestExclusiveLock("torquescan");
                    Log.d(LOG_TAG, "Exclusive lock: " + Integer.toString(lock) + " (" + lockdescription(lock) + ")");
                    initialized = initialize();
                    if (initialized)
                        Log.d(LOG_TAG, "Initialized OK");
                    else
                        Log.d(LOG_TAG, "Can not inititalze adapter");

                    if (torqueService.isConnectedToECU()) {

                        Log.d(LOG_TAG, "Connect to ECU successfull");

                    } else {

                        Log.d(LOG_TAG, "Not connected to ECU");

                    }

                } else {

                    Log.d(LOG_TAG, "Plugin has not full permissions");

                }

            } catch (RemoteException e){

            }
            //setupList();
            //setContentView(list);
        }

        public void onServiceDisconnected(ComponentName name) {
            try {
                if ((lock == 0) || (lock == -2))
                    torqueService.releaseExclusiveLock("torquescan", true);

            } catch (RemoteException e) {

            }

            torqueService = null;
        }
    };

}
