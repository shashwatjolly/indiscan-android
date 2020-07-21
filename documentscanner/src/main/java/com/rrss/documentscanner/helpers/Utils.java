package com.rrss.documentscanner.helpers;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;

import com.rrss.documentscanner.libraries.NativeClass;
import com.rrss.documentscanner.libraries.PolygonView;

import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

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
        m.setRectToRect(new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()), new RectF(0, 0, width, height), Matrix.ScaleToFit.FILL);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }
}