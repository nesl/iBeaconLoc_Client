package ucla.nesl.buildsysdemo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by timestring on 9/23/14.
 */
public class MapPlotter {
    private final String rootFolder = "/storage/sdcard0/inLoc/";

    private final int radiusShadow = 80;
    private final int radiusSelf = 24;

    private final int shadowCycle = 75; // frames

    private Bitmap bitmapMap;
    private Bitmap bitmapShadow;
    private Bitmap bitmapPoint;
    private ImageView imageMap;
    private ImageView imageShadow;
    private ImageView imageSelf;

    private double xMeter;
    private double yMeter;
    private double xPxByMeter = 1.0;
    private double yPxByMeter = 1.0;

    private int offXpx = 0;
    private int offYpx = 0;

    private double scaleMapResize;
    private double curScale = 1.0;

    private int frame = 0;



    public MapPlotter(String packName, String picFormat, ImageView _imageMap, ImageView _imageShadow, ImageView _imageSelf) {
        String packFolder = rootFolder + packName + "/";
        File imgFile = new File(packFolder + "plan." + picFormat);
        imageMap = _imageMap;
        imageShadow = _imageShadow;
        imageSelf = _imageSelf;

        if (imgFile.exists()) {
            bitmapMap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            File file = new File(packFolder + "meta");
            try {
                FileInputStream fis = new FileInputStream(file);
                byte[] data = new byte[(int) file.length()];
                fis.read(data);
                fis.close();
                String s = new String(data, "UTF-8");
                String[] nums = s.split(" ");
                xMeter = Double.parseDouble(nums[0]);
                xPxByMeter = bitmapMap.getWidth() / xMeter;
                yMeter = Double.parseDouble(nums[1]);
                yPxByMeter = bitmapMap.getHeight() / yMeter;
                imageMap.setImageBitmap(bitmapMap);
                imageMap.setPadding(offXpx, offYpx, 0, 0);
                Log.i("PICTURE", "meters" + xMeter + " " + yMeter);
            } catch (IOException e) {
                Log.e("PICTURE", "read meta error", e);
            }
        }
        bitmapPoint = BitmapFactory.decodeFile(rootFolder + "point.png");
        bitmapPoint = Bitmap.createScaledBitmap(bitmapPoint, radiusSelf * 2, radiusSelf * 2, false);
    }

    public void onDraw(double userXmeter, double userYmeter) {
        makeBitmapShadow();
        if (imageMap.getWidth() == 0.0)
            return;

        scaleMapResize = Math.min((double)imageMap.getWidth() / bitmapMap.getWidth(), (double)imageMap.getHeight() / bitmapMap.getHeight());
        double imagecx = imageMap.getWidth() / 2.0;
        double imagecy = imageMap.getHeight() / 2.0;
        double orix = imagecx + (-bitmapMap.getWidth() / 2.0 + userXmeter * xPxByMeter) * scaleMapResize;
        double oriy = imagecy - (-bitmapMap.getHeight() / 2.0 + userYmeter * xPxByMeter) * scaleMapResize;
        double scrollx = orix + offXpx;
        double scrolly = oriy + offYpx;
        double scalex = (scrollx - imagecx) * curScale + imagecx;
        double scaley = (scrolly - imagecy) * curScale + imagecy;

        imageShadow.setImageBitmap(bitmapShadow);
        imageSelf.setImageBitmap(bitmapPoint);
        int txpx = xMeter2px(userXmeter);
        int typx = yMeter2px(userYmeter);
        imageShadow.setPadding((int)scalex - radiusShadow, (int)scaley - radiusShadow, 0 ,0);
        imageSelf.setPadding((int)scalex - radiusSelf, (int)scaley - radiusSelf, 0 ,0);

        frame++;
    }

    public void touchMove(int addx, int addy) {
        offXpx += addx / curScale;
        offYpx += addy / curScale;
        imageMap.setScrollX(-offXpx);
        imageMap.setScrollY(-offYpx);
    }

    public void touchScale(double scale, int cx, int cy) {
        curScale *= scale;
        if (curScale < 1.0)
            curScale = 1.0;
        imageMap.setScaleX((float)curScale);
        imageMap.setScaleY((float)curScale);
        double cdx = cx - imageMap.getWidth() / 2.0;
        double cdy = cy - imageMap.getHeight() / 2.0;
        //Log.i("CXCY", "cx,cy:" + cx + "," + cy + "  cdx,cdy:" + cdx + "," + cdy);
        double ncdx = cdx * scale;
        double ncdy = cdy * scale;
        touchMove(-(int)(ncdx - cdx), -(int)(ncdy - cdy));
        //int[] mapLeftTop = new int[2];
        //imageMap.getLocationOnScreen(mapLeftTop);
        //Log.i("LT", mapLeftTop[0] + " " + mapLeftTop[1]);
    }

    private void makeBitmapShadow() {
        int nframe = frame % shadowCycle;
        bitmapShadow = Bitmap.createBitmap(radiusShadow*2, radiusShadow*2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapShadow);
        Paint paint = new Paint();
        int alpha = 127 * (shadowCycle - nframe) / shadowCycle;
        paint.setColor(0x00ff7777 | (alpha << 24));
        paint.setStyle(Paint.Style.FILL);
        float radius = 20f + (radiusShadow - 20f) * nframe / shadowCycle;
        canvas.drawCircle(radiusShadow, radiusShadow, radius, paint);
    }

    private int xMeter2px(double meter) {
        return (int)(meter * xPxByMeter * curScale);
    }

    private int yMeter2px(double meter) {
        return (int)(meter * yPxByMeter * curScale);
    }
}
