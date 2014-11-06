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

/**
 * Created by timestring on 9/18/14.
 */
public class PerminentConnection {
    //private static final int TIMEOUT_INTERVAL = 200;
    private static final String metaIpPortFileName = "/storage/sdcard0/bsdemoip";

    private String serverIP = "172.17.5.61";
    private int serverPort = 31000;

    //private byte[] msgToSend;

    private Socket socket = null;
    private BufferedWriter out;
    private BufferedReader in;

    private int debugCount = 0;

    private final int STATE_INACTIVE = 0;
    private final int STATE_CREATING = 1;
    private final int STATE_WORKING = 2;
    private int socketState = STATE_INACTIVE;

    public void loadMeta() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(metaIpPortFileName));
            serverIP = br.readLine();
            serverPort = Integer.parseInt(br.readLine());
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
            createMetaDefault();
            loadMeta();
        }
    }

    private void createMetaDefault() {
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



    //public OneShotConnection(byte[] msg, Handler h) {
    public PerminentConnection() {
        loadMeta();
        createSocket();
    }

    public void send(String msg, Handler h) {
        if (socketState == STATE_INACTIVE) {
            createSocket();
        }
        if (socketState != STATE_WORKING)
            return;

        final String msgToSend = msg;
        final Handler resultHandler = h;
        //final Handler resultHandler = null;

        //Log.i("OneShot", "constructor information is set");
        new AsyncTask<Integer, Integer, Integer>() {
            protected Integer doInBackground(Integer... urls) {
                try {
                    int thisDebugCount = debugCount++;
                    Log.i("Socket", "[D" + thisDebugCount + "] try to connect cmd " + msgToSend);
                    out.write(msgToSend + "\n");
                    out.flush();
                    Log.i("Socket", "[D" + thisDebugCount + "] cmd written " + msgToSend);

                    if (resultHandler != null) {
                        Log.i("Socket", "[D" + thisDebugCount + "] wait for a response");
                        String re = in.readLine();
                        Log.i("Socket", "[D" + thisDebugCount + "] get response " + re);
                        if (re != null) {
                            Message m = Message.obtain();
                            m.obj = re;
                            resultHandler.sendMessage(m);
                        }
                        Log.i("Socket", "[D" + thisDebugCount + "] finish handle message");
                    }
                /*} catch (SocketTimeoutException e) {
                    Log.e("TCP", "S: Timeout happened (cmd " + msgToSend + ")");
                    try {
                        socket.close();
                    } catch (Exception ie) {
                    }*/
                } catch (Exception e) {
                    //Log.e("TCP", "S: Error");
                    Log.e("TCP", "S: Error", e);
                    socketState = STATE_INACTIVE;
                }
                return null;
            }
            //}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, 0);
        }.execute(0);
    }

    private void createSocket() {
        socketState = STATE_CREATING;
        new Thread() {
            @Override
            public void run() {
                try {
                    InetAddress serverAddr = InetAddress.getByName(serverIP);
                    //socket = new Socket();
                    //socket.connect(new InetSocketAddress(serverAddr, serverPort), TIMEOUT_INTERVAL);
                    socket = new Socket(serverAddr, serverPort);
                    out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    //socket.setSoTimeout(TIMEOUT_INTERVAL);

                    //out = new BufferedOutputStream(socket.getOutputStream());
                    //out.write(msgToSend);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                } catch (Exception e) {
                    Log.e("SOCKETERROR", "error", e);
                    socketState = STATE_INACTIVE;
                }
                socketState = STATE_WORKING;
            }
        }.start();
    }
}
