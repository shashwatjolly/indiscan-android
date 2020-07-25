
package com.rrss.documentscanner.base;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.rrss.documentscanner.R;
import com.rrss.documentscanner.helpers.ScannerConstants;
import com.rrss.documentscanner.libraries.NativeClass;
import com.rrss.documentscanner.libraries.PolygonView;

import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public abstract class DocumentScanActivity extends AppCompatActivity {

    protected CompositeDisposable disposable = new CompositeDisposable();
    private ArrayList<Bitmap> selectedImage = new ArrayList<Bitmap>(0);;
    private NativeClass nativeClass = new NativeClass();
    private int visibleFlag = 0;
    protected abstract ArrayList<FrameLayout> getHolderImageCrop();
    protected abstract ArrayList<FrameLayout> getParentFrame();
    protected abstract ArrayList<ImageView> getImageView();

    protected abstract ArrayList<PolygonView> getPolygonView();

    protected abstract void showProgressBar();

    protected abstract void hideProgressBar();

    protected abstract void showError(CropperErrorType errorType);

    protected abstract ArrayList<Bitmap> getBitmapImage();

    private void setImageRotation() {

        for(int i=0;i<ScannerConstants.bitmaparray.size();i++) {
            Bitmap tempBitmap = selectedImage.get(i).copy(selectedImage.get(i).getConfig(), true);
            for (int j = 1; j <= 4; j++) {
                MatOfPoint2f point2f = nativeClass.getPoint(tempBitmap);
                if (point2f == null) {
                    tempBitmap = rotateBitmap(tempBitmap, 90 * j);
                } else {
                    selectedImage.set(i, tempBitmap.copy(selectedImage.get(i).getConfig(), true));
                    break;
                }
            }
        }
    }

    protected Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private void setProgressBar(boolean isShow) {
        if (isShow)
            showProgressBar();
        else
            hideProgressBar();
    }

    protected void startCropping() {
        selectedImage = getBitmapImage();
        setProgressBar(true);
        disposable.add(Observable.fromCallable(() -> {
//                    setImageRotation();
                    return false;
                })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe((result) -> {
                            initializeCropping();
                            setProgressBar(false);
                        })
        );
    }


    protected void initializeCropping() throws Exception {
        Bitmap tempBitmap;
        Map<Integer, PointF> pointFs = null;
            if(ScannerConstants.isRotate){
                pointFs = ScannerConstants.pointfArray.get(ScannerConstants.activeImageId);
                tempBitmap = ScannerConstants.tempBitMapArray.get(ScannerConstants.activeImageId);
                setCropHandles(tempBitmap, pointFs, ScannerConstants.activeImageId, visibleFlag);
                ScannerConstants.isRotate = false;
            }
            else
            {
                for(int i = 0; i< ScannerConstants.bitmaparray.size(); i++)
                {
                    try {
                        pointFs = ScannerConstants.pointfArray.get(i);
                        tempBitmap = ScannerConstants.tempBitMapArray.get(i);
                        setCropHandles(tempBitmap, pointFs, i, visibleFlag);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

    }

    private void setCropHandles(Bitmap tempBitmap, Map<Integer, PointF> pointFs,  int i, int visibleFlag){

        int padding = (int) getResources().getDimension(R.dimen.scanPadding);
        getPolygonView().get(i).setPoints(pointFs);
        getPolygonView().get(i).setVisibility(visibleFlag);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(tempBitmap.getWidth() + 2 * padding, tempBitmap.getHeight() + 2 * padding);
        layoutParams.gravity = Gravity.CENTER;
        getPolygonView().get(i).setLayoutParams(layoutParams);
        getPolygonView().get(i).setPointColor(getResources().getColor(R.color.blue));
    }
    protected ArrayList<Bitmap> getCroppedImage() {
        visibleFlag = 4;
        ArrayList<Bitmap> finalArr = new ArrayList<Bitmap>(0);
        for(int i=0;i<ScannerConstants.bitmaparray.size();i++)
        {
            try {
                Map<Integer, PointF> points = getPolygonView().get(i).getPoints();

                float xRatio = (float) selectedImage.get(i).getWidth() / getImageView().get(i).getWidth();
                float yRatio = (float) selectedImage.get(i).getHeight() / getImageView().get(i).getHeight();
//
                Log.e("hello232", getImageView().get(i).getWidth()+"a"+selectedImage.get(i).getHeight());
                float x1 = (Objects.requireNonNull(points.get(0)).x) * xRatio;
                float x2 = (Objects.requireNonNull(points.get(1)).x) * xRatio;
                float x3 = (Objects.requireNonNull(points.get(2)).x) * xRatio;
                float x4 = (Objects.requireNonNull(points.get(3)).x) * xRatio;
                float y1 = (Objects.requireNonNull(points.get(0)).y) * yRatio;
                float y2 = (Objects.requireNonNull(points.get(1)).y) * yRatio;
                float y3 = (Objects.requireNonNull(points.get(2)).y) * yRatio;
                float y4 = (Objects.requireNonNull(points.get(3)).y) * yRatio;
                finalArr.add(nativeClass.getScannedBitmap(selectedImage.get(i), x1, y1, x2, y2, x3, y3, x4, y4));
            } catch (Exception e) {
                showError(CropperErrorType.CROP_ERROR);
                return null;
            }
        }
        return finalArr;
    }

    protected Bitmap scaledBitmap(Bitmap bitmap, int width, int height) {
        Matrix m = new Matrix();
        m.setRectToRect(new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()), new RectF(0, 0, width, height), Matrix.ScaleToFit.CENTER);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }

    private Map<Integer, PointF> getEdgePoints(Bitmap tempBitmap, int i) throws Exception {
        List<PointF> pointFs = getContourEdgePoints(tempBitmap);
        Map<Integer, PointF> orderedPoints = orderedValidEdgePoints(tempBitmap, pointFs, i);
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

    private Map<Integer, PointF> orderedValidEdgePoints(Bitmap tempBitmap, List<PointF> pointFs, int i) {
        Map<Integer, PointF> orderedPoints = getPolygonView().get(i).getOrderedPoints(pointFs);
        if (!getPolygonView().get(i).isValidShape(orderedPoints)) {
            orderedPoints = getOutlinePoints(tempBitmap);
        }
        return orderedPoints;
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposable.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposable.dispose();
    }
}
