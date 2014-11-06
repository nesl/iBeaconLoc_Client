package ucla.nesl.buildsysdemo;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.ArrayList;

/**
 * Created by timestring on 9/18/14.
 */
public class BeaconRssiMaker {
    public static final int[] txPowerInd2Num = {-74, -90};

    private final String commonUuidString = "46A7594F672D4B6C81C1785AECDBA0D5";
    private final byte[] commonUuid = hexStringToByteArray(commonUuidString);

    private int clientId;
    private final int[][] majors = {
            { 4,  4},
            { 4,  4},
            { 4,  4},
            { 4,  4},
            { 4,  4},
            {10, 10},
    };
    private final int outwardMajor = 4;

    private final int[][] minors = {
            {  4, 14},
            { 18, 17},
            {  2, 15},
            { 11,  1},
            { 19, 20},
            {103,104},
    };
    private double[] lastTime = new double[minors.length];
    private RssiSeries[] rssiSeries = new RssiSeries[minors.length];
    private int txRateHz;
    private double txInterval;
    private int txPower;  //0 = high (-74), 1 = low (-90)

    public BeaconRssiMaker(int _clientId, int _powerInd, int _txRateHz) {
        clientId = _clientId;
        setTxPower(_powerInd);
        setTxRate(_txRateHz);
        for (int i = 0; i < rssiSeries.length; i++)
            rssiSeries[i] = new RssiSeries((byte)clientId, (short)outwardMajor, (short)i);
    }

    public void setTxRate(int _txRateHz) {
        txRateHz = _txRateHz;
        txInterval = 1.0 / txRateHz;
    }

    public void setTxPower(int powerInd) {
        txPower = powerInd;
    }

    public void signalLE(byte[] input, byte rssi) {
        if (input.length < 30) {
            Log.i("IBEACON", "short ble packet, discard");
            return;
        }

        if (input.length >= 30)
            Log.i("IBEACON", input[25] + " " + input[26] + " " + input[27] + " " + input[28]);

        for (int i = 0; i < 16; i++)
            if (input[i+9] != commonUuid[i]) {
                Log.i("IBEACON", "wrong uuid, discard");
                return;
            }
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(input, 25, 30);
            DataInputStream dis = new DataInputStream(bais);
            short tmaj = dis.readShort();
            short tmin = dis.readShort();
            byte ttx = dis.readByte();

            short outMinor = -1;
            for (short i = 0; i < minors.length; i++)
                if (majors[i][txPower] == tmaj && minors[i][txPower] == tmin)
                    outMinor = i;
            if (outMinor == -1)
                return ;

            double now = (double)System.currentTimeMillis() / 1000.0;
            double dt = now - lastTime[outMinor];
            if (dt < txInterval)
                return ;
            else if (dt < txInterval * 2.0)
                lastTime[outMinor] += txInterval;
            else
                lastTime[outMinor] = now;

            synchronized (rssiSeries) {
                //Log.i("SYNC", "sync1b");
                Log.i("COLLECT", outMinor + " tx" + ttx + " rssi" + rssi);
                rssiSeries[outMinor].addRssi(ttx, rssi);
                //Log.i("SYNC", "sync1e");
            }
        } catch (Exception e) {
            Log.e("ucla.nesl.myfirstapp.BeaconRssiMaker", "here is an exception", e);
        }
    }

    public ArrayList<String> makeRssiPayloads() {
        //Log.i("SEND", "make rssi payloads");
        //ArrayList<byte[]> re = new ArrayList<byte[]>();
        ArrayList<String> re = new ArrayList<String>();
        synchronized (rssiSeries) {
            //Log.i("SYNC", "sync2b");
            for (int i = 0; i < minors.length; i++) {
                //byte[] tre = rssiSeries[i].getAggregatedRssiPayload();
                String tre = rssiSeries[i].getAggregatedRssiPayload();
                if (tre != null)
                    re.add(tre);
            }
            //Log.i("SYNC", "sync2e");
        }
        return re;
    }

    //public byte[] makeTxPowerRatePayload() {
    public String makeTxPowerRatePayload() {
        try {
            //ByteArrayOutputStream baos = new ByteArrayOutputStream();
            //DataOutputStream dos = new DataOutputStream(baos);
            //dos.writeByte(4);
            //dos.writeByte(clientId);
            //dos.writeByte((byte)txPowerInd2Num[txPower]);
            //dos.writeByte((byte)txRateHz);
            //dos.close();
            //return baos.toByteArray();
            return "4," + clientId + "," + txPowerInd2Num[txPower] + "," + txRateHz;
        } catch (Exception e) {
            Log.e("ucla.nesl.myfirstapp.BeaconRssiMaker", "here is an exception", e);
        }
        return null;
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
