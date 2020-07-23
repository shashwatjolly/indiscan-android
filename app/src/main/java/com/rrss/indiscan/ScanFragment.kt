package com.rrss.indiscan

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.BitmapDrawable
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.rrss.documentscanner.ImageCropActivity
import com.rrss.documentscanner.helpers.ScannerConstants
import com.rrss.documentscanner.helpers.Utils
import com.rrss.documentscanner.libraries.PolygonView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_scan.*
import kotlinx.android.synthetic.main.recent_scans_bottom_sheet.*
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ScanFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ScanFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var bitmaparray = ArrayList<Bitmap>()
    private var imgid = 0;
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>? = null
    private var batchModeStarted = false     // TODO: Replace this boolean
    private var numPhotos = 0
    private var disposable = CompositeDisposable();
    private var flashMode: Int = ImageCapture.FLASH_MODE_OFF
    private var currentCameraFacingId: Int = CameraSelector.LENS_FACING_BACK
    private var orientationEventListener: OrientationEventListener? = null
    private var currentOrientation: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val v =  inflater.inflate(R.layout.fragment_scan, container, false)

        setHasOptionsMenu(true)

        val gesture = GestureDetector(
            activity,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent?): Boolean {
                    return true
                }
                override fun onFling(
                    e1: MotionEvent, e2: MotionEvent, velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    Log.i("Swipe", "onFling has been called!")
                    val SWIPE_MIN_DISTANCE = 120
                    val SWIPE_MAX_OFF_PATH = 250
                    val SWIPE_THRESHOLD_VELOCITY = 200
                    try {
                        if (Math.abs(e1.getX() - e2.getX()) > SWIPE_MAX_OFF_PATH) return false
                        if (e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE
                            && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY
                        ) {
                            Log.i("Swipe", "Up")
                            bottomSheetBehavior!!.setState(BottomSheetBehavior.STATE_EXPANDED);
                        }
                        else {
                            Log.i("Swipe", "Down")
                            bottomSheetBehavior!!.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        }
                    } catch (e: java.lang.Exception) {
                        // nothing
                    }
                    return super.onFling(e1, e2, velocityX, velocityY)
                }
            })

        v.setOnTouchListener(OnTouchListener { v, event -> gesture.onTouchEvent(event) })

        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scan_progress_bar.visibility = GONE
        bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet_layout)
        bottomSheetBehavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (isAdded()) {
                    transitionBottomSheetBackgroundColor(slideOffset);
                    camera_controls.alpha = 1-slideOffset
                    camera_controls_layout.translationY = -1*(activity as AppCompatActivity).supportActionBar?.height!! * slideOffset
                }
            }
        })
        if(allPermissionsGranted()){
            startCamera();
        }
        else{
            requestPermissions(
                REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
        outputDirectory = getOutputDirectory();
        cameraExecutor = Executors.newSingleThreadExecutor()

        orientationEventListener = object : OrientationEventListener(activity) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation >= 330 || orientation < 30) {
                    currentOrientation = Surface.ROTATION_0
                } else if (orientation >= 60 && orientation < 120) {
                    currentOrientation = Surface.ROTATION_90
                } else if (orientation >= 150 && orientation < 210) {
                    currentOrientation = Surface.ROTATION_0
                } else if (orientation >= 240 && orientation < 300) {
                    currentOrientation = Surface.ROTATION_270
                }
            }
        }
        orientationEventListener?.enable()
    }

    //inflate the menu
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.batch_mode_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }
    //handle item clicks of menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.batchModeDone){
            cropImage()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val done = menu.findItem(R.id.batchModeDone)
        if(batchModeStarted) {
            done.isVisible = true
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = activity?.applicationContext?.let {
            ProcessCameraProvider.getInstance(
                it
            )
        }
        if (cameraProviderFuture != null) {
            cameraProviderFuture.addListener(Runnable {

                // Used to bind the lifecycle of cameras to the lifecycle owner
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()


                // Preview
                preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .build()
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .setFlashMode(flashMode)
                    .build()
                // Select back camera
                val cameraSelector =
                    CameraSelector.Builder().requireLensFacing(currentCameraFacingId).build()
                flash_button.setOnClickListener { toggleFlashMode(cameraProvider, cameraSelector) }
                try {
                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll()

                    // Bind use cases to camera
                    camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture
                    )
                    preview?.setSurfaceProvider(viewFinder.createSurfaceProvider())
                } catch (exc: Exception) {
                    Log.e(TAG, "Use case binding failed", exc)
                }

            }, ContextCompat.getMainExecutor(activity))
            camera_capture_button.setOnClickListener { takePhoto() }
        }
    }

    private fun toggleFlashMode(cameraProvider: ProcessCameraProvider , cameraSelector:CameraSelector){
        when (flashMode) {
            ImageCapture.FLASH_MODE_OFF -> {
                flashMode = ImageCapture.FLASH_MODE_ON
                flash_button_img_view.setImageResource(R.drawable.flash_anim_off)

            }
            ImageCapture.FLASH_MODE_ON -> {
                flashMode = ImageCapture.FLASH_MODE_OFF
                flash_button_img_view.setImageResource(R.drawable.flash_anim_on)
            }
        }

        (flash_button_img_view.drawable as AnimatedVectorDrawable).start()

        cameraProvider.unbind(imageCapture)
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(flashMode)
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()

        // Bind use cases to camera
        camera = cameraProvider.bindToLifecycle(
            this, cameraSelector,imageCapture)
    }

    private fun takePhoto() {
        if(!batchModeStarted) {
            batchModeStarted = true
            activity?.invalidateOptionsMenu()
        }
        scan_progress_bar.visibility = VISIBLE
        numPhotos++

        imageCapture?.takePicture(
            ContextCompat.getMainExecutor(activity), object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                @androidx.camera.core.ExperimentalGetImage
                override fun onCaptureSuccess(image: ImageProxy) {
                    var clickedBitmap = image.image!!.toBitmap()
                    var rotatedBitmap = clickedBitmap
                    if(currentOrientation == Surface.ROTATION_0) {
                        rotatedBitmap = Utils.rotateBitmap(clickedBitmap, image.imageInfo.rotationDegrees.toFloat())
                    }
                    bitmaparray.add(rotatedBitmap)
                    ScannerConstants.bitmaparray.add(rotatedBitmap)
                    var width = rotatedBitmap.width
                    var height = rotatedBitmap.height
                    Log.e("ImageRatio ", rotatedBitmap.height.toString() + "  " + rotatedBitmap.width.toString());
                    ScannerConstants.imageRatios.add(height.toFloat()/width.toFloat())
                    imgid+=1
//                    Log.e("hello1111", width.toString()+"aaaa"+height.toString()+"Aaa"+ScannerConstants.imageRatio.toString())
                    val msg = "Photo capture succeeded"
                    Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
                    image.close()
                    disposable.add(
                        Observable.fromCallable {
                            context?.let { initClickedImage(imgid-1, it) };
                            false
                        }
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe { result: Boolean? ->
                                scan_progress_bar.visibility = GONE
                            }
                    )
                }
            })
    }
    private fun initClickedImage(id:Int, context: Context){

        var utils: Utils = Utils();

        var width = 1080
        var height = (width*ScannerConstants.imageRatios[id]).toInt()
        ScannerConstants.width = width;
        ScannerConstants.height = height;

        // INITIALIZE PARAMETERS
        var imageViewParam = ViewGroup.LayoutParams(
            width,
            height
        )

        val polygonViewParams: FrameLayout.LayoutParams = FrameLayout.LayoutParams(
            width,
            height
        )

        // INITIALIZE LAYOUTS
        var imageView = ImageView(context)
        imageView.layoutParams = imageViewParam;

        var polygonView = PolygonView(context)
        polygonView.layoutParams = polygonViewParams;

        var scaledBitmap:Bitmap = utils.scaledBitmap(bitmaparray.get(id), width, height);
        Log.e("hello0", bitmaparray.get(id).width.toString()+"a"+bitmaparray.get(id).height.toString())
        Log.e("hello1", scaledBitmap.width.toString()+"a"+scaledBitmap.height.toString())
        imageView.setImageBitmap(scaledBitmap)
        Log.e("hello2", imageView.width.toString()+"cbc"+imageView.height.toString())
        val tempBitmap = (imageView.getDrawable() as BitmapDrawable).bitmap
        ScannerConstants.tempBitMapArray.add(tempBitmap);
        val pointFs = utils.getEdgePoints(tempBitmap, polygonView);
        Log.e("hello23", tempBitmap.width.toString()+"cbc"+tempBitmap.height.toString())
        ScannerConstants.pointfArray.add(pointFs);
    }

    fun Image.toBitmap(): Bitmap {
        val buffer: ByteBuffer = planes[0].buffer
        buffer.rewind()
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun cropImage(){
        startActivity(Intent(activity, ImageCropActivity::class.java))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        activity?.baseContext?.let { it1 ->
            ContextCompat.checkSelfPermission(
                it1, it)
        } == PackageManager.PERMISSION_GRANTED
    }

    // callback after checking permission
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(activity,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                activity?.finish();
            }
        }
    }

    fun getOutputDirectory(): File {
        val mediaDir = activity?.externalMediaDirs?.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else requireActivity().filesDir
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ScanFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ScanFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    ////////////////////// Handle Bottom Sheet ///////////////////////////////

    private fun transitionBottomSheetBackgroundColor(slideOffset: Float) {
        val colorFrom = getColor(requireContext(), R.color.colorBlackTranslucentLight)
        val colorTo = getColor(requireContext(), R.color.colorBlackTranslucentDarker)
        bottom_sheet_layout.setBackgroundColor(
            interpolateColor(
                slideOffset,
                colorFrom, colorTo
            )
        )
    }

    /**
     * This function returns the calculated in-between value for a color
     * given integers that represent the start and end values in the four
     * bytes of the 32-bit int. Each channel is separately linearly interpolated
     * and the resulting calculated values are recombined into the return value.
     *
     * @param fraction The fraction from the starting to the ending values
     * @param startValue A 32-bit int value representing colors in the
     * separate bytes of the parameter
     * @param endValue A 32-bit int value representing colors in the
     * separate bytes of the parameter
     * @return A value that is calculated to be the linearly interpolated
     * result, derived by separating the start and end values into separate
     * color channels and interpolating each one separately, recombining the
     * resulting values in the same way.
     */
    private fun interpolateColor(fraction: Float, startValue: Int, endValue: Int): Int {
        val startA = startValue shr 24 and 0xff
        val startR = startValue shr 16 and 0xff
        val startG = startValue shr 8 and 0xff
        val startB = startValue and 0xff
        val endA = endValue shr 24 and 0xff
        val endR = endValue shr 16 and 0xff
        val endG = endValue shr 8 and 0xff
        val endB = endValue and 0xff
        return startA + (fraction * (endA - startA)).toInt() shl 24 or
                (startR + (fraction * (endR - startR)).toInt() shl 16) or
                (startG + (fraction * (endG - startG)).toInt() shl 8) or
                startB + (fraction * (endB - startB)).toInt()
    }
}