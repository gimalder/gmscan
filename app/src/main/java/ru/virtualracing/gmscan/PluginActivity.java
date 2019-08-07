package ru.virtualracing.gmscan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import org.prowl.torque.remote.ITorqueService;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

/**
 * This is a very rough, sample plugin that displays a list of all the currently available
 * sensors in Torque (that your ECU supports).
 * 
 * Use the "Display Test Mode" in the main Torque app settings to simulate values.
 * 
 * The plugin name and icon used in Torque are that of the apk icon and name.
 * 
 * Note the intent-filter entries in the manifest - one for this code (an actual activity based plugin)
 * and one for allowing exensions of PID lists for manufacturer specific ECUs. The PID lists are a simple
 * file based resource you can throw together in a normal text-editor. 
 * 
 * The telnetactivity and scanactivity are mainly unfinished classes - feel free to ignore them
 * 
 * Have fun!
 * 
 * @author Ian Hawkins http://torque-bhp.com/
 */

public class PluginActivity extends Activity {


    private ITorqueService torqueService;

    private TextView textView;

    private TextView textView1;
    private TextView textView2;
    private TextView textView3;
    private TextView textView4;
    private TextView textView5;
    private TextView textView6;
    private TextView textView7;
    private TextView textView8;
    private TextView textView9;


    private EditText editText;
    private Handler handler;
    private NumberFormat nf;
    private Integer n = 0;
    private boolean dbg = false;
    private int lock = -7;
    private boolean initialized = false;

    private boolean serviceStatus = false;

    private Timer updateTimer;
    private static PluginActivity instance;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        instance = this;
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.main, null);
        textView = (TextView)findViewById(R.id.textview);

        textView1 = (TextView)findViewById(R.id.textView1);
        textView2 = (TextView)findViewById(R.id.textView2);
        textView3 = (TextView)findViewById(R.id.textView3);
        textView4 = (TextView)findViewById(R.id.textView4);
        textView5 = (TextView)findViewById(R.id.textView5);
        textView6 = (TextView)findViewById(R.id.textView6);
        textView7 = (TextView)findViewById(R.id.textView7);
        textView8 = (TextView)findViewById(R.id.textView8);
        textView9 = (TextView)findViewById(R.id.textView9);

        editText = (EditText)findViewById(R.id.editinput);
        view.setKeepScreenOn(true);

        // Max of 2 digits for readings.
        nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);
        n = 0;
        handler = new Handler();

        textView.setMovementMethod(new ScrollingMovementMethod());

        //addText("hello2");
        //addText("onCreate running");

        final Button button = (Button) findViewById(R.id.button);
        final Button button2 = (Button) findViewById(R.id.button2);
        final Button buttonSvc = (Button) findViewById(R.id.buttonSvc);
        final CheckBox checkbox = (CheckBox) findViewById(R.id.checkBox);
        final CheckBox checkbox2 = (CheckBox) findViewById(R.id.checkBox2);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                String response = null;
                String request = editText.getText().toString();

                response = mySendCommand(request);

                addText(response);
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
            CheckATF();

            }
        });

        buttonSvc.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                addText("Trying to enable service");
                //startService(new Intent(this, aquisitionService.class));

                if (serviceStatus == false) {
                    Intent serviceIntent = new Intent();
                    serviceIntent.setAction("ru.virtualracing.gmscan.aquisitionService.MY_SERVICE");
                    startService(serviceIntent);
                    serviceStatus = true;
                    buttonSvc.setText("Stop Service");
                }
                else
                {
                    Intent serviceIntent = new Intent();
                    serviceIntent.setAction("ru.virtualracing.gmscan.aquisitionService.MY_SERVICE");
                    stopService(serviceIntent);
                    serviceStatus = false;
                    buttonSvc.setText("Start Service");
                }
            }
        });

        checkbox.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //is chkIos checked?
                if (((CheckBox) v).isChecked()) {
                    if (updateTimer != null)
                        updateTimer.cancel();

                    EditText editText2 = (EditText) findViewById(R.id.editText2);
                    Integer pause = ((Integer.valueOf(editText2.getText().toString()) > 100) ? Integer.valueOf(editText2.getText().toString()) : 1000);

                    updateTimer = new Timer();
                    updateTimer.schedule(new TimerTask() {
                        public void run() {
                            //refreshListItems();
                            // Update the widget.
                            final String myText = Integer.toString(n++);
                            handler.post(new Runnable() {
                                public void run() {

                                    CheckATF();
                                }
                            });
                        }
                    }, pause, pause);
                }else{
                    if (updateTimer != null)
                        updateTimer.cancel();
                }

            }
        });

        checkbox2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //is chkIos checked?
                if (((CheckBox) v).isChecked()) {
                    dbg = true;
                }else{
                    dbg = false;
                }

            }
        });
/*
        button3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                //Intent intent = new Intent(PluginActivity.this, DataActivity.class);
                //EditText editText = (EditText) findViewById(R.id.edit_message);
                //String message = editText.getText().toString();
                //intent.putExtra(EXTRA_MESSAGE, message);
                //startActivity(intent);

            }
        });
*/
    }

    public void CheckATF() {
        String answer = mySendCommand("2101");
        //answer = "61 01 00 00 00 00 00 00 00 81 5C 97 00 31 00 31 03 03 20 00 00 00 04 00 80 00 00 32 32 32";
        if (dbg)
            answer = "61 01 0c d9 00 0c c0 00 01 86 7d b3 00 31 00 31 03 0C 20 00 00 00 04 00 7F 00 00 32 32 32";
        if (answer.contains("61")) {
            if (answer.contains(",")) {
                String[] separated = answer.split(",");
                answer = separated[1];
            }

            String[] data = answer.split(" ");
            answer = answer.trim();
            if (data[0].equals("61") && (data.length == 30)) {

                //addText(data[10] + ", " + Integer.toString(HexToInt(data[10]) - 40) + ", " + answer);
                textView1.setText(Integer.toString(HexToInt(data[10]) - 40));
                textView2.setText(Integer.toString(HexToInt(data[9]) - 40));
                textView3.setText(String.format("%.1f", HexToInt(data[11]) / 12.78));
                textView4.setText(Integer.toString((HexToInt(data[5]) * 256 + HexToInt(data[6])) / 4));
                textView5.setText(Integer.toString((HexToInt(data[7]) * 256 + HexToInt(data[8])) / 4));
                textView6.setText(Integer.toString(HexToInt(data[24]) * 25 - 3200));
                Integer tmpInt = HexToInt(data[17]);
                if ((tmpInt & 0x0004) != 0) {
                    textView7.setText("1");
                }
                else
                    textView7.setText("0");
                if ((tmpInt & 0x0008) != 0)
                    textView8.setText("1");
                else
                    textView8.setText("0");
                switch (HexToInt(data[22])) {
                    case 0: textView9.setText("1");
                        break;
                    case 1: textView9.setText("2");
                        break;
                    case 2: textView9.setText("3");
                        break;
                    case 3: textView9.setText("4");
                        break;
                    case 4: textView9.setText("P/N");
                        break;
                    case 5: textView9.setText("R");
                        break;
                }
            }
            else
                addText("wrong answer: " + answer + ", " + data[0]);

        }
    };

    public int HexToInt(String input) {
        try {
            return (Character.digit(input.charAt(0), 16) * 16 + Character.digit(input.charAt(1), 16));
        } catch (Exception e) {
            return 0;
        }
    }

    public static final PluginActivity getInstance() {
      return instance;
   }



    @Override
    protected void onResume() {
        super.onResume();
        //addText("onResume running");
        /*
        if (updateTimer != null)
            updateTimer.cancel();

        updateTimer = new Timer();
        updateTimer.schedule(new TimerTask() {
            public void run() {
                //refreshListItems();
                // Update the widget.
                final String myText = Integer.toString(n++);
                handler.post(new Runnable() {
                    public void run() {

                    //addText(myText);
                    }
                });
            }
        }, 1000, 1000);
        */

        //textView.setText("");

        // Bind to the torque service
     }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        //addText("onPause running");

        if (updateTimer != null)
            updateTimer.cancel();


        //unbindService(connection);
    }


    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
        addText("onStop running");
    }


    private static Vector<String> tipsShown = new Vector();
    public void tip(String tip) {
        if (!tipsShown.contains(tip)) {
            tipsShown.add(tip);
        }
    }

    public void addText(String input){
        textView.append("\n" + "[" + DateFormat.getTimeInstance().format(new Date()) + "] " + input);
    }
    
    /**
     * Quick popup message
     *
    */
    public void popupMessage(final String title, final String message, final boolean finishOnClose) {
        handler.post(new Runnable() { public void run() {
            try {
                final AlertDialog adialog = new AlertDialog.Builder(PluginActivity.this).create();

                adialog.setButton("OK", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {

                    adialog.dismiss();
                    if (finishOnClose) {
                        finish();
                    }

                }
                });


                ScrollView svMessage = new ScrollView(PluginActivity.this);
                TextView tvMessage   = new TextView(PluginActivity.this);

                SpannableString spanText = new SpannableString(message);

                Linkify.addLinks(spanText, Linkify.ALL);
                tvMessage.setText(spanText);
                tvMessage.setTextSize(TypedValue.COMPLEX_UNIT_DIP,16f);
                tvMessage.setMovementMethod(LinkMovementMethod.getInstance());

                svMessage.setPadding(14, 2, 10, 12);
                svMessage.addView(tvMessage);

                adialog.setTitle(title);
                adialog.setView(svMessage);

                adialog.show();
            } catch(Throwable e) {

            }
        }
        });
    }

}