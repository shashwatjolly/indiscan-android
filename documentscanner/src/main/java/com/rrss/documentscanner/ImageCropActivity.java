package com.rrss.documentscanner;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.rrss.documentscanner.base.CropperErrorType;
import com.rrss.documentscanner.base.DocumentScanActivity;
import com.rrss.documentscanner.helpers.ScannerConstants;
import com.rrss.documentscanner.libraries.PolygonView;

import org.opencv.core.Point;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

@RequiresApi(api = Build.VERSION_CODES.M)
public class ImageCropActivity extends DocumentScanActivity {

    private static ArrayList<FrameLayout> holderImageCrop = new ArrayList<FrameLayout>(0);
    private static ArrayList<FrameLayout> parentFrameArray = new ArrayList<FrameLayout>(0);
    private static ArrayList<ImageView> imageView = new ArrayList<ImageView>(0);
    private static ArrayList<PolygonView> polygonView = new ArrayList<PolygonView>(0);
    private static ArrayList<Bitmap> cropImage = new ArrayList<Bitmap>(0);
    protected static ArrayList<Integer> polygonViewId = new ArrayList<Integer>(0);
    protected static ArrayList<Integer> rotationAngle = new ArrayList<Integer>(0);
    private boolean isInverted;
    private ProgressBar progressBar;
    private int temp_id;
    private CustomHorizontalScrollView horizontalScrollView;
    private OnClickListener btnImageEnhanceClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            showProgressBar();
            disposable.add(
                    Observable.fromCallable(() -> {
                        cropImage = getCroppedImage();
                        if (cropImage == null)
                            return false;
//                        if (ScannerConstants.saveStorage)
//                            saveToInternalStorage(cropImage);
                        return false;
                    })
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((result) -> {
                                hideProgressBar();
                                if (cropImage != null) {
                                    ScannerConstants.bitmaparrayfinal = cropImage;
                                    showProcessedImage();
                                    setResult(RESULT_OK);
//                                    finish();
                                }
                            })
            );
        }
    };

    private void showProcessedImage() {
        Bitmap bitmap;
        for (int i = 0; i < getImageView().size() ; i++) {
            bitmap = ScannerConstants.bitmaparrayfinal.get(i);
            int width = ScannerConstants.width;
            int height = (int) (width*ScannerConstants.imageRatios.get(i));
            bitmap = scaledBitmap(bitmap, width, height);
            getImageView().get(i).setImageBitmap(bitmap);
            getParentFrame().get(i).setScaleX(0.8f);
            getParentFrame().get(i).setScaleY(0.8f);
            getPolygonView().get(i).setVisibility(View.INVISIBLE);
        }
    }

//    private OnClickListener btnRebase = v -> {
//        cropImage = ScannerConstants.selectedImageBitmap.copy(ScannerConstants.selectedImageBitmap.getConfig(), true);
//        isInverted = false;
//        startCropping();
//    };
    private OnClickListener btnCloseClick = v -> initView();
    //    private OnClickListener btnInvertColor = new OnClickListener() {
//        @Override
//        public void onClick(View v) {
//            showProgressBar();
//            disposable.add(
//                    Observable.fromCallable(() -> {
//                        invertColor();
//                        return false;
//                    })
//                            .subscribeOn(Schedulers.io())
//                            .observeOn(AndroidSchedulers.mainThread())
//                            .subscribe((result) -> {
//                                hideProgressBar();
//                                Bitmap scaledBitmap = scaledBitmap(cropImage, holderImageCrop.getWidth(), holderImageCrop.getHeight());
//                                imageView.setImageBitmap(scaledBitmap);
//                            })
//            );
//        }
//    };
    private final OnClickListener onRotateClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            ScannerConstants.isRotate = true;
            int activeItem = ScannerConstants.activeImageId;
            int currRotationAngle = rotationAngle.get(activeItem);
            currRotationAngle = (currRotationAngle + 90) % 360;
            rotationAngle.set(activeItem, currRotationAngle);
            Bitmap rotatedBitmap = rotateBitmap(ScannerConstants.tempBitMapArray.get(activeItem), 90);
            float ratio = ScannerConstants.imageRatios.get(activeItem);
            ScannerConstants.imageRatios.set(activeItem, 1/ratio);
            int width = (int) (ScannerConstants.width-2*getResources().getDimension(R.dimen.scanPadding));
            int height = (int) (width*ScannerConstants.imageRatios.get(activeItem));
            Bitmap scaledBitmap = scaledBitmap(rotatedBitmap , width, height);
            getImageView().get(activeItem).setImageBitmap(scaledBitmap);
            ScannerConstants.tempBitMapArray.set(activeItem, scaledBitmap);
            if(currRotationAngle == 0){
                ScannerConstants.pointfArray.set(activeItem, ScannerConstants.pointfArray0.get(activeItem));
            }
            if(currRotationAngle == 90){
                ScannerConstants.pointfArray.set(activeItem, ScannerConstants.pointfArray90.get(activeItem));
            }
            if(currRotationAngle == 180){
                ScannerConstants.pointfArray.set(activeItem, ScannerConstants.pointfArray180.get(activeItem));
            }
            if(currRotationAngle == 270){
                ScannerConstants.pointfArray.set(activeItem, ScannerConstants.pointfArray270.get(activeItem));
            }
            ScannerConstants.tempBitMapArray.set(activeItem, scaledBitmap);
            startCropping();
        }
    };

    protected Map<Integer, PointF> rotatedCropHandles(int activeItem){
        Map<Integer, PointF> pointArray = ScannerConstants.pointfArray.get(activeItem);

        Log.e("hello3", String.valueOf(pointArray.values()));
        PointF origin = new PointF(ScannerConstants.tempBitMapArray.get(activeItem).getWidth() / 2, ScannerConstants.tempBitMapArray.get(activeItem).getHeight() / 2);
        Map<Integer, PointF> rotatedPointArray = new HashMap<>();
        int t = 3;
        float x;
        float y;
        float xNew;
        float yNew;
        float ratio = ScannerConstants.imageRatios.get(activeItem);
//        float ratio = 1;

//       point0---------point1;
//       |                 |
//       |                 |
//       |                 |
//       |                 |
//       point2---------point3;

        // NEW POINT 0
        PointF p = pointArray.get(0);
        x = p.x-origin.x;
        y = p.y-origin.y;
        rotatedPointArray.put(0, new PointF((-y + origin.x)*ratio, (x + origin.y)*ratio));

        // NEW POINT 1
        p = pointArray.get(1);
        x = p.x-origin.x;
        y = p.y-origin.y;
        rotatedPointArray.put(1, new PointF((-y + origin.x)*ratio, (x + origin.y)*ratio));

        // NEW POINT 2
        p = pointArray.get(2);
        x = p.x-origin.x;
        y = p.y-origin.y;
        rotatedPointArray.put(2, new PointF((-y + origin.x)*ratio, (x + origin.y)*ratio));

        // NEW POINT 3
        p = pointArray.get(3);
        x = p.x-origin.x;
        y = p.y-origin.y;
        rotatedPointArray.put(3, new PointF((-y + origin.x)*ratio, (x + origin.y)*ratio));

        return rotatedPointArray;
    }
    protected int getQuadrant(PointF p){
        if(p.x >= 0 && p.y >= 0 )return 1;
        if(p.x < 0 && p.y > 0 )return 2;
        if(p.x < 0 && p.y < 0)return 3;
        if(p.x > 0 && p.y < 0 )return 4;
        return 0;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_crop);
        cropImage = ScannerConstants.tempBitMapArray;
        isInverted = false;
        if (ScannerConstants.tempBitMapArray != null)
            initView();
        else {
            Toast.makeText(this, ScannerConstants.imageError, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected ArrayList<FrameLayout> getHolderImageCrop() {
        return holderImageCrop;
    }

    @Override
    protected ArrayList<FrameLayout> getParentFrame() {
        return parentFrameArray;
    }

    @Override
    protected ArrayList<ImageView> getImageView() {
        return imageView;
    }

    @Override
    protected ArrayList<PolygonView> getPolygonView() {
        return polygonView;
    }

    @Override
    protected void showProgressBar() {
        RelativeLayout rlContainer = findViewById(R.id.rlContainer);
        setViewInteract(rlContainer, false);
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected void hideProgressBar() {
        RelativeLayout rlContainer = findViewById(R.id.rlContainer);
        setViewInteract(rlContainer, true);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    protected void showError(CropperErrorType errorType) {
        switch (errorType) {
            case CROP_ERROR:
                Toast.makeText(this, ScannerConstants.cropError, Toast.LENGTH_LONG).show();
                break;
        }
    }

    @Override
    protected ArrayList<Bitmap> getBitmapImage() {
        return cropImage;
    }

    private void setViewInteract(View view, boolean canDo) {
        view.setEnabled(canDo);
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setViewInteract(((ViewGroup) view).getChildAt(i), canDo);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initView() {
//        int width = displayMetrics.widthPixels;
        int margin = (int) getResources().getDimension(R.dimen.scanPadding);
        progressBar = findViewById(R.id.progressBar);
        if (progressBar.getIndeterminateDrawable() != null && ScannerConstants.progressColor != null)
            progressBar.getIndeterminateDrawable().setColorFilter(Color.parseColor(ScannerConstants.progressColor), android.graphics.PorterDuff.Mode.MULTIPLY);
        else if (progressBar.getProgressDrawable() != null && ScannerConstants.progressColor != null)
            progressBar.getProgressDrawable().setColorFilter(Color.parseColor(ScannerConstants.progressColor), android.graphics.PorterDuff.Mode.MULTIPLY);

        Button btnImageCrop = findViewById(R.id.btnImageCrop);
        Button btnClose = findViewById(R.id.btnClose);
        btnImageCrop.setText(ScannerConstants.cropText);
        btnClose.setText(ScannerConstants.backText);

        btnImageCrop.setBackgroundColor(Color.parseColor(ScannerConstants.cropColor));
        btnClose.setBackgroundColor(Color.parseColor(ScannerConstants.backColor));
        btnImageCrop.setOnClickListener(btnImageEnhanceClick);
        btnClose.setOnClickListener(btnCloseClick);

        // INITIALIZE PARAMETERS
        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );

        FrameLayout.LayoutParams childFrameParam = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        childFrameParam.setMargins(margin,margin,margin,margin);
        childFrameParam.gravity = Gravity.CENTER;

        FrameLayout.LayoutParams imageViewParam = new FrameLayout.LayoutParams(
               FrameLayout.LayoutParams.WRAP_CONTENT,
               FrameLayout.LayoutParams.WRAP_CONTENT
        );
        imageViewParam.gravity = Gravity.CENTER;

        FrameLayout.LayoutParams polygonViewParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );

        LinearLayout mainLayout = (LinearLayout)findViewById(R.id.layoutProcessActivity);
        horizontalScrollView = new CustomHorizontalScrollView(this, ScannerConstants.bitmaparray.size(), ScannerConstants.width);
        mainLayout.addView(horizontalScrollView);
        LinearLayout container = new LinearLayout(this);
        container.setLayoutParams(containerParams);
        temp_id = LinearLayout.generateViewId();
        container.setId(temp_id);
        ScannerConstants.containerId = temp_id;
        horizontalScrollView.addView(container);

        for(int i=0;i<ScannerConstants.bitmaparray.size();i++) {
            FrameLayout parentFrame = new FrameLayout(this);
            LinearLayout.LayoutParams parentFrameParam = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
            );
            parentFrameParam.gravity = Gravity.CENTER;
            parentFrame.setLayoutParams(parentFrameParam);
            parentFrame.setBackgroundColor(getColor(R.color.orange));
            temp_id = FrameLayout.generateViewId();
            parentFrame.setId(temp_id);
            parentFrameArray.add(parentFrame);
            container.addView(parentFrame);
            rotationAngle.add(0);

            FrameLayout imageHolderFrame = new FrameLayout(this);
            imageHolderFrame.setLayoutParams(childFrameParam);
            temp_id = FrameLayout.generateViewId();
            imageHolderFrame.setId(temp_id);
            imageHolderFrame.setBackgroundColor(getColor(R.color.blue));
            parentFrame.addView(imageHolderFrame);
            holderImageCrop.add(imageHolderFrame);

            ImageView clickedImage = new ImageView(this);
            clickedImage.setLayoutParams(imageViewParam);
//            clickedImage.setAdjustViewBounds(true);
            temp_id = ImageView.generateViewId();
            clickedImage.setId(temp_id);
            clickedImage.setBackgroundColor(getColor(R.color.colorGrey));
            imageHolderFrame.addView(clickedImage);
            clickedImage.setImageBitmap(ScannerConstants.tempBitMapArray.get(i));
            imageView.add(clickedImage);

            PolygonView cropHandles = new PolygonView(this);
            cropHandles.setLayoutParams(polygonViewParams);
            cropHandles.setVisibility(View.INVISIBLE);
            temp_id = PolygonView.generateViewId();
            cropHandles.setId(temp_id);
            polygonViewId.add(temp_id);
            parentFrame.addView(cropHandles);
            polygonView.add(cropHandles);
//            Log.e("ICA PolyView", i + " - " + cropHandles.getWidth() + " " + cropHandles.getHeight());
//            Log.e("ICA ImageView", i + " - " + clickedImage.getWidth() + " " + clickedImage.getHeight());
//            Log.e("ICA HolderFrame", i + " - " + imageHolderFrame.getWidth() + " " + imageHolderFrame.getHeight());
//            Log.e("ICA ParentFrame", i + " - " + parentFrame.getWidth() + " " + parentFrame.getHeight());
//            Log.e("ICA ContainerFrame", i + " - " + container.getWidth() + " " + container.getHeight());
//            Log.e("ICA HScrollView", i + " - " + horizontalScrollView.getWidth() + " " + horizontalScrollView.getHeight());
//            Log.e("ICA MainLayout", i + " - " + mainLayout.getWidth() + " " + mainLayout.getHeight());

        }
//        for(int i=0;i<ScannerConstants.bitmaparray.size();i++) {
//            Log.e("ICA PolyView 2", i + " - " + polygonView.get(i).getWidth() + " " + polygonView.get(i).getHeight());
//            Log.e("ICA ImageView 2", i + " - " + imageView.get(i).getWidth() + " " + imageView.get(i).getHeight());
//            Log.e("ICA HolderFrame 2", i + " - " + holderImageCrop.get(i).getWidth() + " " + holderImageCrop.get(i).getHeight());
//            Log.e("ICA ParentFrame 2", i + " - " + parentFrameArray.get(i).getWidth() + " " + parentFrameArray.get(i).getHeight());
//            Log.e("ICA ContainerFrame 2", i + " - " + container.getWidth() + " " + container.getHeight());
//            Log.e("ICA HScrollView 2", i + " - " + horizontalScrollView.getWidth() + " " + horizontalScrollView.getHeight());
//            Log.e("ICA MainLayout 2", i + " - " + mainLayout.getWidth() + " " + mainLayout.getHeight());
//        }
            //OnclickListeners
        ImageView ivRotate = findViewById(R.id.ivRotate);
        ivRotate.setOnClickListener(onRotateClick);
        startCropping();
    }

    private void invertColor() {
        int activeItem = ScannerConstants.activeImageId;
        if (!isInverted) {
            Bitmap bmpMonochrome = Bitmap.createBitmap(cropImage.get(activeItem).getWidth(), cropImage.get(activeItem).getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmpMonochrome);
            ColorMatrix ma = new ColorMatrix();
            ma.setSaturation(0);
            Paint paint = new Paint();
            paint.setColorFilter(new ColorMatrixColorFilter(ma));
            canvas.drawBitmap(cropImage.get(activeItem), 0, 0, paint);
            cropImage.set(activeItem, bmpMonochrome.copy(bmpMonochrome.getConfig(), true));
        } else {
            cropImage.set(activeItem, cropImage.get(activeItem).copy(cropImage.get(activeItem).getConfig(), true));
        }
        isInverted = !isInverted;
    }

    private String saveToInternalStorage(Bitmap bitmapImage) {
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "cropped_" + timeStamp + ".png";
        File mypath = new File(directory, imageFileName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return directory.getAbsolutePath();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposable.dispose();
    }
}