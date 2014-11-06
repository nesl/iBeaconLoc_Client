package ucla.nesl.buildsysdemo;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by timestring on 10/15/14.
 */
public class RssiSeries {
    private ArrayList<Byte> txPowers = new ArrayList<Byte>();
    private ArrayList<Byte> rssis = new ArrayList<Byte>();
    //private byte[] payloadPrefix;
    private String payloadPrefix;

    public RssiSeries(byte clientId, short major, short minor) {
        try {
            //ByteArrayOutputStream baos = new ByteArrayOutputStream();
            //DataOutputStream dos = new DataOutputStream(baos);
            //dos.writeByte(1);
            //dos.writeByte(clientId);
            //dos.writeShort(major);
            //dos.writeShort(minor);
            //dos.close();
            //payloadPrefix = baos.toByteArray();
            payloadPrefix = "1," + clientId + "," + major + "," + minor + ",";
        }  catch (Exception e) {
            Log.e("ucla.nesl.myfirstapp.BeaconRssiMaker", "here is an exception", e);
        }
    }

    public void addRssi(byte txPower, byte rssi) {
        txPowers.add(txPower);
        rssis.add(rssi);
    }

    //public byte[] getAggregatedRssiPayload() {
    public String getAggregatedRssiPayload() {
        if (rssis.size() == 0)
            return null;

        byte trssi = getRssi();
        byte ttx = getTxPower();
        txPowers.clear();
        rssis.clear();
        try {
            //ByteArrayOutputStream baos = new ByteArrayOutputStream();
            //DataOutputStream dos = new DataOutputStream(baos);
            //dos.write(payloadPrefix);
            //dos.writeByte(trssi);
            //dos.writeByte(ttx);
            //dos.close();
            //Log.i("SEND", "minor=" + payloadPrefix[5] + "  " + trssi);
            //return baos.toByteArray();
            return payloadPrefix + trssi + "," + ttx;
        }  catch (Exception e) {
            Log.e("ucla.nesl.myfirstapp.BeaconRssiMaker", "here is an exception", e);
        }
        return null;
    }

    private byte getTxPower() {
        return getTxPowerFirst();
    }

    private byte getTxPowerFirst() {
        return txPowers.get(0);
    }

    private byte getRssi() {
        //Log.i("SEND", "result is " + get);
        return getRssiP5();
        //return getRssiCycleForDebug();
    }

    private byte getRssiAverage() {
        int t = 0;
        for (Byte b: rssis)
            t += b;
        return (byte)(t / rssis.size());
    }

    private byte getRssiP5() {
        Collections.sort(rssis);
        Collections.reverse(rssis);
        int ind = rssis.size() / 20;
        //for (Byte b: rssis)
        //    Log.i("RSSI", "" + b);
        //Log.i("RSSI", "--");
        return rssis.get(ind);
    }

    private int debugCnt = 0;
    private byte getRssiCycleForDebug() {
        debugCnt++;
        return (byte)(debugCnt % 100);
    }
}
