package ucla.nesl.buildsysdemo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;


/*
 * remember, copy inLoc folder under sdcard folder in Android. you can find one under
 * <projectRoot>/accessory/
 */

public class MainActivity extends Activity {
    private static final long TIME_SEND_TX_POWER_RATE = 2000;
    private static final long TIME_REQUEST_POS_UPDATE = 500;
    private static final long TIME_PERIODICALLY_SEND_AGGREGATED_RSSI = 500;

    private final int seekerStartProg = 8;

    private boolean globalWorkingFlag = true;

    private TextView textClientID;
    private TextView textTxPower;
    private SeekBar seekBarTxPower;
    private TextView textTxRate;
    private SeekBar seekBarTxRate;
    private TextView textPos;
    private RelativeLayout mainRight;

    private int txPowerInd = 0;
    private int txRateInd = 0;
    private int txRateHz = 20;

    private int clientID;

    private BluetoothAdapter bluetoothAdapter;
    private BeaconRssiMaker beaconMaker;
    private ClientIDConsulter clientIDConsulter;
    private MapPlotter mapPlotter;

    private PerminentConnection permanentConnection;

    private float userXmeter = -200f;
    private float userYmeter = -200f;

    private final int posUpdateAllowedTrials = (int)(5000L / TIME_REQUEST_POS_UPDATE) + 1;
    private int posUpdateLives = posUpdateAllowedTrials;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_nexus5);

        clientIDConsulter = new ClientIDConsulter();
        //SPECIAL!!! unless to change clientID, don't open this block.
        //clientIDConsulter.setClientID(2);

        OneShotConnection.loadMeta();

/*        byte[] testbytes = new byte[5];
        testbytes[0] = 2;
        testbytes[1] = 4;
        testbytes[2] = 2;
        testbytes[3] = 6;
        testbytes[4] = 8;
        Log.i("TEST", "len=" + testbytes.toString());*/


        clientID = clientIDConsulter.getClientID();
        textClientID = (TextView)findViewById(R.id.textClientID);
        textClientID.setText("Client ID: " + clientID);

        textTxPower = (TextView)findViewById(R.id.textTxPower);

        seekBarTxPower = (SeekBar)findViewById(R.id.seekBarTxPower);
        seekBarTxPower.setOnSeekBarChangeListener(seekBarTxPowerChanged);
        seekBarTxPower.setProgress(100);

        textTxRate = (TextView)findViewById(R.id.textTxRate);

        seekBarTxRate = (SeekBar)findViewById(R.id.seekBarTxRate);
        seekBarTxRate.setOnSeekBarChangeListener(seekBarTxRateChanged);
        seekBarTxRate.setProgress(100);

        textPos = (TextView)findViewById(R.id.textPos);

        mainRight = (RelativeLayout)findViewById(R.id.mainRight);
        mainRight.setBackgroundColor(0xffffffff);

        final BluetoothManager bluetoothManager =
                (BluetoothManager) this.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothAdapter.startLeScan(leScanCallback);

        beaconMaker = new BeaconRssiMaker(clientID, txPowerInd, txRateHz);

        mapPlotter = new MapPlotter("buildsys", "png",
                (ImageView)findViewById(R.id.imageMap),
                (ImageView)findViewById(R.id.imageSelfShadow),
                (ImageView)findViewById(R.id.imageSelf));

        permanentConnection = new PerminentConnection();

        handlerTxPowerRate.sendEmptyMessage(1);
        handlerPosUpdate.sendEmptyMessage(0);
        onDrawHandler.sendEmptyMessage(0);
        handlerPeriodicallySendAggRssi.sendEmptyMessage(0);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        globalWorkingFlag = false;
    }

    private Handler onDrawHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (globalWorkingFlag) {
                mapPlotter.onDraw(userXmeter, userYmeter);
                sendEmptyMessageDelayed(0, 20);
            }
        }
    };

    private float lastTouchDownX;
    private float lastTouchDownY;
    private double lastDistance;
    private int touchState = 0;   // 0 -> none,  1 -> single,  2 -> multi
    public boolean onTouchEvent(MotionEvent e) {
        int maskedAction = e.getActionMasked();
        if (maskedAction == MotionEvent.ACTION_DOWN) {
            lastTouchDownX = e.getX();
            lastTouchDownY = e.getY();
            if (lastTouchDownX > (float)((RelativeLayout)findViewById(R.id.mainLeft)).getWidth())
                touchState = 1;
        }
        else if (maskedAction == MotionEvent.ACTION_UP || maskedAction == MotionEvent.ACTION_CANCEL) {
            touchState = 0;
        }
        else if (maskedAction == MotionEvent.ACTION_POINTER_DOWN) {
            touchState = 2;
            //Log.i("TOUCH", "down, with count " + e.getPointerCount());
            float dx = e.getX(0) - e.getX(1);
            float dy = e.getY(0) - e.getY(1);
            lastDistance = Math.sqrt(dx * dx + dy * dy);
        }
        else if (maskedAction == MotionEvent.ACTION_POINTER_UP) {
            if (e.getPointerCount() == 2) {
                int remainIndex = 1 - e.getActionIndex();
                lastTouchDownX = e.getX(remainIndex);
                lastTouchDownY = e.getY(remainIndex);
                touchState = 1;
            }
            //Log.i("TOUCH", "up, with count " + e.getPointerCount());
        }
        else if (maskedAction == MotionEvent.ACTION_MOVE) {
            if (touchState == 1) {
                float dx = e.getX() - lastTouchDownX;
                float dy = e.getY() - lastTouchDownY;
                mapPlotter.touchMove((int)dx, (int)dy);
                lastTouchDownX = e.getX();
                lastTouchDownY = e.getY();
            }
            if (touchState == 2) {
                int[] mapLeftTop = new int[2];
                mainRight.getLocationOnScreen(mapLeftTop);
                //Log.i("LT", mapLeftTop[0] + " " + mapLeftTop[1]);
                float dx = e.getX(0) - e.getX(1);
                float dy = e.getY(0) - e.getY(1);
                float cx = (e.getX(0) + e.getX(1)) / 2f - mapLeftTop[0];
                float cy = (e.getY(0) + e.getY(1)) / 2f - mapLeftTop[1];
                double nDistance = Math.sqrt(dx * dx + dy * dy);
                mapPlotter.touchScale(nDistance / lastDistance, (int)cx ,(int)cy);
                lastDistance = nDistance;
            }
        }


        return true;
    }

    // ---- SeekBar -----------------------------------------------------------------------------------------
    private SeekBar.OnSeekBarChangeListener seekBarTxPowerChanged = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int prog, boolean fromUser) {
            txPowerInd = progToInd(prog, BeaconRssiMaker.txPowerInd2Num.length - 1);
            textTxPower.setText("Transmit Power = " + BeaconRssiMaker.txPowerInd2Num[txPowerInd] + " dbm");
            //Log.i("SEEKER", "change " + fromUser + " " + fromUser + " " + ind);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            //Log.i("SEEKER", "start");
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            //Log.i("SEEKER", "end");
            seekBar.setProgress(indToProg(txPowerInd, BeaconRssiMaker.txPowerInd2Num.length - 1));
            beaconMaker.setTxPower(txPowerInd);
            handlerTxPowerRate.sendEmptyMessage(0);
        }
    };

    private SeekBar.OnSeekBarChangeListener seekBarTxRateChanged = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int prog, boolean fromUser) {
            txRateInd = progToInd(prog, 19);
            txRateHz = 20 - txRateInd;
            textTxRate.setText("Transmit Rate = " + txRateHz + " Hz");
            //Log.i("SEEKER", "change " + fromUser + " " + fromUser + " " + ind);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            //Log.i("SEEKER", "start");
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            //Log.i("SEEKER", "end");
            seekBar.setProgress(indToProg(txRateInd, 19));
            beaconMaker.setTxRate(txRateHz);
            handlerTxPowerRate.sendEmptyMessage(0);
        }
    };

    private int progToInd(int prog, int maxValue) {
        for (int i = 0; i < maxValue; i++) {
            double boundary = (double)seekerStartProg + ((double)i + 0.5) / maxValue * (double)(100 - seekerStartProg);
            if ((double)prog < boundary)
                return maxValue - i;
        }
        return 0;
    }

    private int indToProg(int ind, int maxValue) {
        return seekerStartProg + (100 - seekerStartProg) * (maxValue - ind) / maxValue;
    }



    // ---- Bluetooth LE callback --------------------------------------------------------------------------
    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            //Log.i("Main", "bluetooth in");
            beaconMaker.signalLE(scanRecord, (byte) rssi);
        }
    };



    // ---- Network sending/receiving handler ---------------------------------------------------------------
    private Handler handlerPos = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //try {
                //ByteArrayInputStream bais = new ByteArrayInputStream((byte[]) msg.obj);
                //DataInputStream dis = new DataInputStream(bais);
                //userXmeter = dis.readFloat();
                //userYmeter = dis.readFloat();
                String[] coors = ((String) msg.obj).split(",");
                userXmeter = Float.parseFloat(coors[0]);
                userYmeter = Float.parseFloat(coors[1]);
                String showMsg = String.format("(%.2f, %.2f)", userXmeter, userYmeter);
                textPos.setText(showMsg);
                posUpdateLives = posUpdateAllowedTrials;
            //} catch (IOException e) {
            //    Log.e("MAIN", "I got an error", e);
            //}
        }
    };

    private Handler handlerTxPowerRate = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //byte[] re = beaconMaker.makeTxPowerRatePayload();
            String re = beaconMaker.makeTxPowerRatePayload();
            Log.i("TX", "intersestd " + re);
            if (re != null) {
                //Log.i("Main", "send rssi everything's set, wait for connection (len=" + re.length);
                permanentConnection.send(re, null);
            }

            if (msg.what > 0 && globalWorkingFlag)
                sendEmptyMessageDelayed(1, TIME_SEND_TX_POWER_RATE);
        }
    };

    private Handler handlerPosUpdate = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //try {
                //ByteArrayOutputStream baos = new ByteArrayOutputStream();
                //DataOutputStream dos = new DataOutputStream(baos);
                //dos.writeByte(2);
                //dos.writeByte(clientID);
                //dos.close();
                //byte[] bytes = baos.toByteArray();
                //new OneShotConnection(bytes, handlerPos);
                String msgToServer = "2," + clientID;
            permanentConnection.send(msgToServer, handlerPos);
            //} catch (IOException e) {
            //   Log.e("MAIN", "I got an error", e);
            //}
            posUpdateLives--;
            if (posUpdateLives <= 0)
                textPos.setText("Lost connection");
            if (globalWorkingFlag)
                sendEmptyMessageDelayed(0, TIME_REQUEST_POS_UPDATE);
        }
    };

    private Handler handlerPeriodicallySendAggRssi = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //for (byte[] payload: beaconMaker.makeRssiPayloads())
            for (String payload: beaconMaker.makeRssiPayloads())
                permanentConnection.send(payload, null);
            if (globalWorkingFlag)
                sendEmptyMessageDelayed(0, TIME_PERIODICALLY_SEND_AGGREGATED_RSSI);
        }
    };
}
