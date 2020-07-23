package com.rrss.documentscanner;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
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

import com.rrss.documentscanner.base.CropperErrorType;
import com.rrss.documentscanner.base.DocumentScanActivity;
import com.rrss.documentscanner.helpers.ScannerConstants;
import com.rrss.documentscanner.libraries.PolygonView;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ImageCropActivity extends DocumentScanActivity {

    private static ArrayList<FrameLayout> holderImageCrop = new ArrayList<FrameLayout>(0);
    private static ArrayList<FrameLayout> parentFrameArray = new ArrayList<FrameLayout>(0);
    private static ArrayList<ImageView> imageView = new ArrayList<ImageView>(0);
    private static ArrayList<PolygonView> polygonView = new ArrayList<PolygonView>(0);
    private static ArrayList<Bitmap> cropImage = new ArrayList<Bitmap>(0);
    protected static ArrayList<Integer> polygonViewId = new ArrayList<Integer>(0);
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
            bitmap = scaledBitmap(bitmap, getHolderImageCrop().get(i).getWidth(), getHolderImageCrop().get(i).getHeight());
            getImageView().get(i).setImageBitmap(bitmap);
            getPolygonView().get(i).setVisibility(View.INVISIBLE);
        }
    }

//    private OnClickListener btnRebase = v -> {
//        cropImage = ScannerConstants.selectedImageBitmap.copy(ScannerConstants.selectedImageBitmap.getConfig(), true);
//        isInverted = false;
//        startCropping();
//    };
    private OnClickListener btnCloseClick = v -> finish();
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
    private OnClickListener onRotateClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            ScannerConstants.isRotate = true;
            showProgressBar();
            disposable.add(
                    Observable.fromCallable(() -> {
                        if (isInverted)
                            invertColor();
                        int activeItem = ScannerConstants.activeImageId;
                        cropImage.set(activeItem, rotateBitmap(cropImage.get(activeItem), 90));
                        Log.e("hellorotate", cropImage.get(activeItem).getWidth()+"abc"+cropImage.get(activeItem).getHeight());
                        return false;
                    })
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((result) -> {
                                hideProgressBar();
                                startCropping();
                            })
            );
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_crop);
        cropImage = ScannerConstants.tempBitMapArray;
        isInverted = false;
        if (ScannerConstants.bitmaparray != null)
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

    private void initView() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        ScannerConstants.width = width;
//        int height = (int)( getResources().getDimension(R.dimen.imageViewHeight));
//        int paddingLeft = (int)(getResources().getDimension(R.dimen.imageFramePaddingLeft));
//        int paddingRight = (int)(getResources().getDimension(R.dimen.imageFramePaddingRight));
//        int paddingTop = (int)(getResources().getDimension(R.dimen.imageFramePaddingTop));
//        int paddingBottom = (int)(getResources().getDimension(R.dimen.imageFramePaddingBottom));

        int paddingLeft = 60;
        int paddingRight = 60;
        int paddingTop = 10;
        int paddingBottom = 10;
        width = ScannerConstants.width;
        int height = (int) (width*ScannerConstants.imageRatio);

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
                width,
                height
        );

        LinearLayout.LayoutParams parentFrameParam = new LinearLayout.LayoutParams(
                width,
                height
        );
        parentFrameParam.gravity = Gravity.CENTER;
//        parentFrameParam.setMargins(60,10,60,10);
        FrameLayout.LayoutParams childFrameParam = new FrameLayout.LayoutParams(
                width,
                height
        );
        childFrameParam.gravity = Gravity.CENTER;

        FrameLayout.LayoutParams imageViewParam = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        FrameLayout.LayoutParams polygonViewParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );

        LinearLayout mainLayout = (LinearLayout)findViewById(R.id.layoutProcessActivity);
        horizontalScrollView = new CustomHorizontalScrollView(this, ScannerConstants.bitmaparray.size(), width);
        mainLayout.addView(horizontalScrollView);
        LinearLayout container = new LinearLayout(this);
        container.setLayoutParams(containerParams);
        temp_id = LinearLayout.generateViewId();
        container.setId(temp_id);
        ScannerConstants.containerId = temp_id;
        horizontalScrollView.addView(container);

        for(int i=0;i<ScannerConstants.bitmaparray.size();i++) {
            FrameLayout parentFrame = new FrameLayout(this);
            parentFrame.setLayoutParams(parentFrameParam);
            temp_id = FrameLayout.generateViewId();
            parentFrame.setId(temp_id);
            parentFrameArray.add(parentFrame);
//            parentFrame.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
            container.addView(parentFrame);

            FrameLayout imageHolderFrame = new FrameLayout(this);
            imageHolderFrame.setLayoutParams(childFrameParam);
            temp_id = FrameLayout.generateViewId();
            imageHolderFrame.setId(temp_id);
            parentFrame.addView(imageHolderFrame);
            holderImageCrop.add(imageHolderFrame);

            ImageView clickedImage = new ImageView(this);
            clickedImage.setLayoutParams(imageViewParam);
            temp_id = ImageView.generateViewId();
            clickedImage.setId(temp_id);
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
        }

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