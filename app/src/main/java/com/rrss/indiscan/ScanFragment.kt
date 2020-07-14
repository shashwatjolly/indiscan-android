package com.rrss.indiscan

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.OnTouchListener
import android.widget.LinearLayout
import android.widget.Toast
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.*
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat.invalidateOptionsMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import androidx.lifecycle.LiveData
import com.rrss.documentscanner.ImageCropActivity
import com.rrss.documentscanner.helpers.ScannerConstants
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_scan.*
import kotlinx.android.synthetic.main.recent_scans_bottom_sheet.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList


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


    private var flashMode: Int = ImageCapture.FLASH_MODE_OFF
    private var currentCameraFacingId: Int = CameraSelector.LENS_FACING_BACK
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
        bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet_layout)
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
        camera_capture_button.setOnClickListener { takePhoto() }

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
//        cropbutton.setOnClickListener{cropImage()}
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
                    .build()
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setFlashMode(flashMode)
                    .build()
                // Select back camera
                val cameraSelector = CameraSelector.Builder().requireLensFacing(currentCameraFacingId).build()
                flashbutton.setOnClickListener { toggleFlashMode(cameraProvider,cameraSelector)}

                try {
                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll()

                    // Bind use cases to camera
                    camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture)
                    preview?.setSurfaceProvider(viewFinder.createSurfaceProvider(camera?.cameraInfo))
                } catch(exc: Exception) {
                    Log.e(TAG, "Use case binding failed", exc)
                }

            }, ContextCompat.getMainExecutor(activity))
        }

        camera_capture_button.setOnClickListener { takePhoto() }

    }

    private fun toggleFlashMode(cameraProvider: ProcessCameraProvider , cameraSelector:CameraSelector){
        when (flashMode) {
            ImageCapture.FLASH_MODE_OFF ->
                flashMode = ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON ->
                flashMode = ImageCapture.FLASH_MODE_OFF
        }



        cameraProvider.unbind(imageCapture)
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(flashMode)
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
        numPhotos++
//        toolbar_center_title.text = numPhotos.toString()
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create timestamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg")

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Setup image capture listener which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(activity), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(activity?.baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                    bitmaparray.add(BitmapFactory.decodeFile(photoFile.absolutePath))
                    imgid+=1;
                }
            })
    }

//    private fun showImageLayout(){
//        setContentView(androidx.camera.core.R.layout.showimage)
//
//        // imageview params
//        val imparam1 = ViewGroup.LayoutParams(
//            ViewGroup.LayoutParams.MATCH_PARENT,
//            ViewGroup.LayoutParams.MATCH_PARENT
//        )
//
//        for (i in 0 until bitmaparray.size){
//            var imgv: ImageView = ImageView(this);
//            imgv.id = View.generateViewId();
//            imgv.setImageBitmap(bitmaparray.get(i));
//            displaycapturedImage.addView(imgv,imparam1);
//
//        }
//        button1.setOnClickListener { cropImage() }
//    }

    private fun cropImage(){
        for (i in 0 until bitmaparray.size) {

            ScannerConstants.bitmaparray.add(bitmaparray.get(i));
        }
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
            File(it, resources.getString(com.rrss.indiscan.R.string.app_name)).apply { mkdirs() }
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
}