package gyurix.bluetoothremotecontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.OutputStream;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED;

public class MainActivity extends AppCompatActivity {
    public TextView logView, stateView;
    public ScrollView scroller;
    public BluetoothAdapter adapter;
    public StringBuilder sb = new StringBuilder();
    public BluetoothDevice device;
    public String mac;
    public SharedPreferences pref;
    public BluetoothSocket bs;
    public OutputStream os;
    public Handler h;
    public boolean waiting;
    public HorizontalScrollView sv;
    public LinearLayout dl;
    public byte[] data = new byte[]{0, 0, 0, 0};

    public void reconnect() {
        while (true) {
            log("Reconnect...");
            if (!adapter.isEnabled()) {
                log("Enabling BT adapter...");
                adapter.enable();
                while (!adapter.isEnabled()) {
                    SystemClock.sleep(500);
                }
                log("Enabled.");
            }
            try {
                if (device == null && !findDevice()) {
                    continue;
                }
                bs = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                log("Connecting to " + device.getName()+"...");
                bs.connect();
                log("Connected to " + device.getName());
                os = bs.getOutputStream();
                return;
            } catch (Throwable e) {
                log("Connection failed, make sure the scoreboard is enabled.");
                SystemClock.sleep(2000);
            }
        }
    }

    private void makeDeviceButtons() {
        dl.removeAllViews();
        for (final BluetoothDevice dev : adapter.getBondedDevices()) {
            final Button but = new Button(MainActivity.this);
            but.setBackgroundColor(0XFF000080);
            but.setTextColor(mac.equals(dev.getAddress()) ? 0xFFFFFF00 : 0xFFFFFFFF);
            but.setText("  " + dev.getName() + "  ");
            but.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mac = dev.getAddress();
                    log("Selected device " + dev.getName() + " (" + mac + ")");
                    os = null;
                    device = null;
                    SharedPreferences.Editor ed = pref.edit();
                    ed.putString("mac", mac);
                    ed.apply();
                    makeDeviceButtons();
                }
            });
            dl.addView(but);
        }
    }

    private boolean findDevice() {
        log("Finding paired device...");
        h.post(new Runnable() {
            @Override
            public void run() {
                makeDeviceButtons();
            }
        });
        for (final BluetoothDevice dev : adapter.getBondedDevices()) {
            if (dev.getAddress().equals(mac)) {
                device = dev;
                log("Found selected paired device " + dev.getName()+" ("+dev.getAddress()+")");
                return true;
            }
        }
        log("Device was not found, you need to pair it before using this application.");
        return false;
    }

    public void update() {
        stateView.setText("" + data[0] + data[1] + " : " + data[2] + data[3]);
        log("Preparing " + data[0] + "-" + data[1] + "-" + data[2] + "-" + data[3]);
        if (waiting) {
            log("Waiting for connection...");
            return;
        }
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                waiting = true;
                while (true) {
                    try {
                        if (!adapter.isEnabled())
                            reconnect();
                        os.write(data[0] + 48);
                        os.write(data[1] + 48);
                        os.write(data[2] + 48);
                        os.write(data[3] + 48);
                        os.flush();
                        log("Sent " + data[0] + "-" + data[1] + "-" + data[2] + "-" + data[3]);
                        waiting = false;
                        return null;
                    } catch (Throwable e) {
                        reconnect();
                    }
                }
            }
        }.execute();
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            h = new Handler();
            pref = PreferenceManager.getDefaultSharedPreferences(this);
            setContentView(R.layout.activity_main);
            mac = pref.getString("mac", "20:16:01:20:66:55");
            sv = ((HorizontalScrollView) findViewById(R.id.deviceScroller));
            dl = (LinearLayout) sv.findViewById(R.id.deviceList);
            findViewById(R.id.Up1).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    up1(v);
                }
            });
            findViewById(R.id.Up2).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    up2(v);
                }
            });
            findViewById(R.id.Reset).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    reset(v);
                }
            });
            findViewById(R.id.Down1).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    down1(v);
                }
            });
            findViewById(R.id.Down2).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    down2(v);
                }
            });
            scroller = (ScrollView) findViewById(R.id.scrollText);
            logView = (TextView) scroller.findViewById(R.id.logView);
            stateView = (TextView) findViewById(R.id.currentState);
            adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null) {
                stateView.setTextSize(40f);
                stateView.setText("Missing BT adapter");
                return;
            }
            update();
        } catch (Throwable e) {
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Exit")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).setNegativeButton("No", null).show();
    }

    public int count(String s, char c2) {
        int i = 0;
        for (char c : s.toCharArray()) {
            if (c == c2)
                ++i;
        }
        return i;
    }

    public synchronized void log(String msg) {
        sb.append('\n').append(msg);
        final String txt = sb.toString();
        h.post(new Runnable() {
            @Override
            public void run() {
                logView.setText(txt);
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scroller.fullScroll(View.FOCUS_DOWN);
                    }
                }, 100);
            }
        });
    }

    public void error(Throwable e) {
        sb.append("\nError - ").append(e.getClass().getSimpleName()).append(" - ").append(e.getMessage());
        boolean filter = false;
        for (StackTraceElement ste : e.getStackTrace()) {
            boolean st = ste.getClassName().startsWith("gyurix.bluetoothscoreboardcontrol.");
            if (st)
                filter = true;
            if (!filter || st)
                sb.append("\nLINE ").append(ste.getLineNumber()).append(" - ").append(ste.getClassName()).append(" - ").append(ste.getMethodName());
        }
        log("-------------------------------------------------------------------");
    }

    @Override
    public void finish() {
        try {
            adapter.disable();
            System.exit(0);
        } catch (Throwable e) {
            error(e);
        }
        super.finish();
    }


    public void up1(View view) {
        try {
            data[1]++;
            if (data[1] == 10) {
                data[1] = 0;
                data[0] = (byte) ((data[0] + 1) % 10);
            }
            update();
        } catch (Throwable e) {
            error(e);
        }
    }

    public void up2(View view) {
        try {
            data[3]++;
            if (data[3] == 10) {
                data[3] = 0;
                data[2] = (byte) ((data[2] + 1) % 10);
            }
            update();
        } catch (Throwable e) {
            error(e);
        }
    }

    public void down1(View view) {
        try {
            data[1]--;
            if (data[1] == -1) {
                data[1] = 9;
                data[0] = (byte) ((data[0] + 9) % 10);
            }
            update();
        } catch (Throwable e) {
            error(e);
        }
    }

    public void reset(View view) {
        try {
            for (int i = 0; i < 4; i++)
                data[i] = 0;
            update();
        } catch (Throwable e) {
            error(e);
        }
    }

    public void down2(View view) {
        try {
            data[3]--;
            if (data[3] == -1) {
                data[3] = 9;
                data[2] = (byte) ((data[2] + 9) % 10);
            }
            update();
        } catch (Throwable e) {
            error(e);
        }
    }
}
