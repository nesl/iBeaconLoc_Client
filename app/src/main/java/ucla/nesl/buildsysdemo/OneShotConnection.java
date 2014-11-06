package ucla.nesl.buildsysdemo;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Created by timestring on 9/18/14.
 */
public class OneShotConnection {
    //private static final int TIMEOUT_INTERVAL = 200;
    private static final String metaIpPortFileName = "/storage/sdcard0/bsdemoip";

    private static String serverIP = "172.17.5.61";
    private static int serverPort = 31000;

    //private byte[] msgToSend;
    private String msgToSend;
    private Handler resultHandler;

    public static void loadMeta() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(metaIpPortFileName));
            serverIP = br.readLine();
            serverPort = Integer.parseInt( br.readLine() );
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
            createMetaDefault();
            loadMeta();
        }
    }

    private static void createMetaDefault() {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(metaIpPortFileName, "UTF-8");
            writer.println(serverIP);
            writer.println(serverPort);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static int debugCount = 0;
    public OneShotConnection(String msg, Handler h, int dele) {
        msgToSend = msg;
        resultHandler = h;
        //Log.i("OneShot", "constructor information is set");
        try {
            new AsyncTask<Integer, Integer, Integer>() {
                protected Integer doInBackground(Integer... urls) {
                    //if (msgToSend != null)
                    //    return null;
                    Socket socket = null;
                    //BufferedOutputStream out;
                    BufferedWriter out;
                    //ylLog.i("OneShot", "begin to perform task");
                    try {
                        int thisDebugCount = debugCount++;
                        Log.i("Socket", "[D" + thisDebugCount + "] try to connect cmd " + msgToSend);
                        InetAddress serverAddr = InetAddress.getByName(serverIP);
                        //socket = new Socket();
                        //socket.connect(new InetSocketAddress(serverAddr, serverPort), TIMEOUT_INTERVAL);
                        socket = new Socket(serverAddr, serverPort);

                        //socket.setSoTimeout(TIMEOUT_INTERVAL);

                        //out = new BufferedOutputStream(socket.getOutputStream());
                        //out.write(msgToSend);
                        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                        out.write(msgToSend);
                        out.flush();
                        Log.i("Socket", "[D" + thisDebugCount + "] cmd written " + msgToSend);

                        resultHandler = null; /////////////
                        if (resultHandler == null) {
                            socket.close();
                            Log.i("Socket", "[D" + thisDebugCount + "] close by null");
                        } else {
                            Log.i("Socket", "[D" + thisDebugCount + "] wait for a response");
                            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            String re = in.readLine();
                            socket.close();
                            Log.i("Socket", "[D" + thisDebugCount + "] get response and close");
                            if (re != null) {
                                Message m = Message.obtain();
                                m.obj = re;
                                resultHandler.sendMessage(m);
                            }
                            Log.i("Socket", "[D" + thisDebugCount + "] finish handle message");
                        }
                    } catch (SocketTimeoutException e) {
                        Log.e("TCP", "S: Timeout happened (cmd " + msgToSend + ")");
                        try {
                            socket.close();
                        } catch (Exception ie) {
                        }
                    } catch (Exception e) {
                        //Log.e("TCP", "S: Error");
                        //Log.e("TCP", "S: Error", e);
                        try {
                            socket.close();
                        } catch (Exception ie) {
                        }
                    }
                    return null;
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, 0);
        }catch(Exception ooe){}
    }



    /*
    public OneShotConnection(byte[] msg, Handler h) {
        msgToSend = msg;
        resultHandler = h;
        //Log.i("OneShot", "constructor information is set");
        new Thread() {
            @Override
            public void run() {
                //if (msgToSend != null)
                //    return null;
                Socket socket = null;
                DataOutputStream out;
                //ylLog.i("OneShot", "begin to perform task");
                try {
                    InetAddress serverAddr = InetAddress.getByName(serverIP);
                    socket = new Socket(serverAddr, serverPort);
                    out = new DataOutputStream(socket.getOutputStream());
                    out.write(msgToSend);
                    out.flush();
                    if (msgToSend[0] == 1) {
                        Log.i("Socket", "send out cmd type 1 with rssi " + msgToSend[6]);
                    }
                    if (resultHandler != null) {
                        DataInputStream in = new DataInputStream(socket.getInputStream());
                        byte[] tre = new byte[1000];
                        int len = in.read(tre);
                        if (len > 0) {
                            byte[] re = Arrays.copyOfRange(tre, 0, len);
                            Message m = Message.obtain();
                            m.obj = re;
                            resultHandler.sendMessage(m);
                        }
                    }
                    socket.close();
                } catch (Exception e) {
                    Log.e("TCP", "S: Error");
                    //e.printStackTrace();
                }
            }
        }.start();
    }
    */
}
