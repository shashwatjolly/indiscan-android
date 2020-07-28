package com.rrss.documentscanner.helpers;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;
import android.widget.ImageView;

import com.rrss.documentscanner.libraries.NativeClass;
import com.rrss.documentscanner.libraries.PolygonView;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {
    private NativeClass nativeClass = new NativeClass();

    public Map<Integer, PointF> getEdgePoints(Bitmap tempBitmap, PolygonView polygonView) throws Exception {
        List<PointF> pointFs = getContourEdgePoints(tempBitmap);
        Map<Integer, PointF> orderedPoints = orderedValidEdgePoints(tempBitmap, pointFs,polygonView);
        return orderedPoints;
    }

    private List<PointF> getContourEdgePoints(Bitmap tempBitmap) {
        MatOfPoint2f point2f = nativeClass.getPoint(tempBitmap);
        if (point2f == null)
            point2f = new MatOfPoint2f();
        List<Point> points = Arrays.asList(point2f.toArray());
        List<PointF> result = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            result.add(new PointF(((float) points.get(i).x), ((float) points.get(i).y)));
        }
        return result;
    }

    private Map<Integer, PointF> getOutlinePoints(Bitmap tempBitmap) {
        Map<Integer, PointF> outlinePoints = new HashMap<>();
        outlinePoints.put(0, new PointF(0, 0));
        outlinePoints.put(1, new PointF(tempBitmap.getWidth(), 0));
        outlinePoints.put(2, new PointF(0, tempBitmap.getHeight()));
        outlinePoints.put(3, new PointF(tempBitmap.getWidth(), tempBitmap.getHeight()));
        return outlinePoints;
    }

    private Map<Integer, PointF> orderedValidEdgePoints(Bitmap tempBitmap, List<PointF> pointFs, PolygonView polygonView) {
        Map<Integer, PointF> orderedPoints = polygonView.getOrderedPointsNew(pointFs);
        if (orderedPoints.size() != 4) {
            orderedPoints = getOutlinePoints(tempBitmap);
        }
        return orderedPoints;
    }

    public Bitmap scaledBitmap(Bitmap bitmap, int width, int height) {
        Matrix m = new Matrix();
        m.setRectToRect(new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()), new RectF(0, 0, width, height), Matrix.ScaleToFit.CENTER);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }

    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static Bitmap applyMagicFilter(Bitmap inputBitmap){
        Mat source = new Mat();
        org.opencv.android.Utils.bitmapToMat(inputBitmap,source);
        Mat dest = new Mat(source.width(), source.height(), source.type());
        Mat dest1 = new Mat(source.width(), source.height(), source.type());
        source.convertTo(source,-1,1,30);
        Imgproc.medianBlur(source, dest, 25);
        Core.addWeighted(source, 1.6, dest, -0.6, 0, dest);
        Bitmap outputBitmap = Bitmap.createBitmap(source.cols(), source.rows(), Bitmap.Config.ARGB_8888);
        dest.convertTo(dest,-1,1.5,-80);
        Imgproc.medianBlur(dest, dest1, 41);
        Core.addWeighted(dest, 1.5, dest1, -0.4, 0, dest);
        org.opencv.android.Utils.matToBitmap(dest, outputBitmap);
        return outputBitmap;
    }

    public static Bitmap applyBrightness(Bitmap inputBitmap){
        Mat source = new Mat();
        org.opencv.android.Utils.bitmapToMat(inputBitmap,source);
        source.convertTo(source,-1,1.8,50);
        Bitmap outputBitmap = Bitmap.createBitmap(source.cols(), source.rows(), Bitmap.Config.ARGB_8888);
        org.opencv.android.Utils.matToBitmap(source, outputBitmap);
        return outputBitmap;

    }
    public static  Bitmap applyGrayMode(Bitmap inputBitmap){
        Mat source = new Mat();
        org.opencv.android.Utils.bitmapToMat(inputBitmap,source);
        Imgproc.cvtColor(source, source, Imgproc.COLOR_RGB2GRAY);
        Bitmap outputBitmap = Bitmap.createBitmap(source.cols(), source.rows(), Bitmap.Config.ARGB_8888);
        org.opencv.android.Utils.matToBitmap(source, outputBitmap);
        return outputBitmap;
    }

    public static  Bitmap applyBw(Bitmap inputBitmap){
        Mat source = new Mat();
        Mat dest = new Mat(source.width(), source.height(), source.type());
        org.opencv.android.Utils.bitmapToMat(inputBitmap,source);
        Imgproc.cvtColor(source, source, Imgproc.COLOR_RGB2GRAY);
        Imgproc.adaptiveThreshold(source,source,255,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,Imgproc.THRESH_BINARY,11,10);
        Imgproc.GaussianBlur(source, dest, new Size(0,0),20);
        Core.addWeighted(source, 1.6, dest, -0.3, 0, source);
        Imgproc.GaussianBlur(source, dest, new Size(0,0),5);
        Bitmap outputBitmap = Bitmap.createBitmap(source.cols(), source.rows(), Bitmap.Config.ARGB_8888);
        org.opencv.android.Utils.matToBitmap(source, outputBitmap);
        return outputBitmap;
    }
}