package ucla.nesl.buildsysdemo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by timestring on 9/22/14.
 */
public class ClientIDConsulter {
    private final String filename = "/storage/sdcard0/bsdemo";
    private int clientID = 0;

    public ClientIDConsulter() {
        try {
            FileInputStream fis = new FileInputStream(new File(filename));
            clientID = fis.read();
            fis.close();
        } catch (FileNotFoundException e) {
            setClientID(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean setClientID(int id) {
        if (id < 0 || id > 255)
            return false;
        try {
            FileOutputStream fos = new FileOutputStream(new File(filename));
            clientID = id;
            fos.write(id);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public int getClientID() {
        return clientID;
    }
}
