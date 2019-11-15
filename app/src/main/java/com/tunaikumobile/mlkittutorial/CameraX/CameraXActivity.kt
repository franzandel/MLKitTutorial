package com.tunaikumobile.mlkittutorial.CameraX

import android.Manifest
import android.app.ActionBar
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.tunaikumobile.mlkittutorial.R
import com.tunaikumobile.mlkittutorial.DrawImageCanvas
import kotlinx.android.synthetic.main.activity_camera_x.*
import org.jetbrains.anko.alert
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors


private const val REQUEST_CODE_PERMISSIONS = 10

// This is an array of all the permission specified in the manifest.
private val REQUIRED_PERMISSIONS = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.WRITE_EXTERNAL_STORAGE
)

// Minimum API 21 (Lollipop)
class CameraXActivity : AppCompatActivity(), LifecycleOwner {

    private val downloadDirectory =
        Environment.getExternalStorageDirectory().toString() + "/CameraX"
    private var ktp = ""
    private var filePath = ""
    private val executor = Executors.newSingleThreadExecutor()
    lateinit var drawImageCanvas: DrawImageCanvas

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_x)

        drawImageCanvas()
        setupCamera()

        // Every time the provided texture view changes, recompute layout
        view_finder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }

        extract_button.setOnClickListener {
            val intent = Intent(this, PictureKtpActivity::class.java).apply {
                putExtra("FILEPATH", filePath)
            }
            startActivity(intent)
        }
    }

    private fun setupCamera() {
        if (allPermissionsGranted()) {
            view_finder.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun startCamera() {
        val preview = setupPreview()
        val imageCapture = setupImageCapture()
        val analyzerUseCase = setupAnalyzer()

        // Bind use cases to lifecycle
        // If Android Studio complains about "this" being not a LifecycleOwner
        // try rebuilding the project or updating the appcompat dependency to
        // version 1.1.0 or higher.
        CameraX.bindToLifecycle(
            this, preview, imageCapture, analyzerUseCase
        )
    }

    private fun setupPreview(): Preview {
        // Create configuration object for the view_finder use case
        val previewConfig = PreviewConfig.Builder().apply {
            setTargetResolution(Size(640, 480))
        }.build()


        // Build the view_finder use case
        val preview = Preview(previewConfig)

        // Every time the view_finder is updated, recompute layout
        preview.setOnPreviewOutputUpdateListener {
            // To update the SurfaceTexture, we have to remove it and re-add it
            val parent = view_finder.parent as ViewGroup
            parent.removeView(view_finder)
            parent.addView(view_finder, 0)

            view_finder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        return preview
    }

    private fun setupImageCapture(): ImageCapture {
        // Create configuration object for the image capture use case
        val imageCaptureConfig = ImageCaptureConfig.Builder()
            .apply {
                // We don't set a resolution for image capture; instead, we
                // select a capture mode which will infer the appropriate
                // resolution based on aspect ration and requested mode
                setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
            }.build()

        // Build the image capture use case and attach button click listener
        val imageCapture = ImageCapture(imageCaptureConfig)
        capture_button.setOnClickListener {
            onCaptureButtonClicked(imageCapture)
        }

        return imageCapture
    }

    private fun onCaptureButtonClicked(imageCapture: ImageCapture) {
        setupDirectory()

        val file = File(this.downloadDirectory + "/${System.currentTimeMillis()}.jpg")

        imageCapture.takePicture(file, executor,
            object : ImageCapture.OnImageSavedListener {
                override fun onError(
                    imageCaptureError: ImageCapture.ImageCaptureError,
                    message: String,
                    exc: Throwable?
                ) {
                    imageSaveFailed(message, exc!!)
                }

                override fun onImageSaved(file: File) {
                    imageSaveSuccess(file)
                }
            })
    }

    private fun setupDirectory() {
        val downloadDirectory = File(downloadDirectory)
        if (!downloadDirectory.exists()) {
            downloadDirectory.mkdirs()
        } else {
            downloadDirectory.deleteRecursively()
            downloadDirectory.mkdirs()
        }
    }

    private fun imageSaveFailed(message: String, exc: Throwable) {
        val msg = "Photo capture failed: $message"
        Log.e("CameraXApp", msg, exc)
        view_finder.post {
            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun imageSaveSuccess(file: File) {
        val msg = "Photo capture succeeded: ${file.absolutePath}"
        Log.d("CameraXApp", msg)
        view_finder.post {
            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
        }

        filePath = file.absolutePath
//        recognizeText(filePath)

        val byteArray = File(filePath).readBytes()
        saveBitmap(croppedImage(byteArray))
    }

    private fun setupAnalyzer(): ImageAnalysis {
        // Setup image analysis pipeline that computes average pixel luminance
        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
            // In our analysis, we care more about the latest image than
            // analyzing *every* image
            setImageReaderMode(
                ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE
            )
        }.build()

        // Build the image analysis use case and instantiate our analyzer

        return ImageAnalysis(analyzerConfig).apply {
            setAnalyzer(executor, LuminosityAnalyzer())
        }
    }

    private fun updateTransform() {
        val matrix = Matrix()

        // Compute the center of the view finder
        val centerX = view_finder.width / 2f
        val centerY = view_finder.height / 2f

        // Correct preview output to account for display rotation
        val rotationDegrees = when (view_finder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        // Finally, apply transformations to our TextureView
        view_finder.setTransform(matrix)
    }

    /**
     * Process result from permission request dialog box, has the request
     * been granted? If yes, start Camera. Otherwise display a toast
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                view_finder.post { startCamera() }
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isNumeric(text: String): Boolean {
        return text.matches("-?\\d+(\\.\\d+)?".toRegex())
    }

    private fun showKTP(elementText: String) {
        if (isNumeric(elementText)) {
            if (elementText.length in 15..17) {
                runOnUiThread {
                    ktp = elementText
                    alert(elementText, "No. KTP").show()
                }
            }
        }
    }

    private fun recognizeText(filePath: String) {
        val myBitmap = BitmapFactory.decodeFile(filePath)
        val textImage = FirebaseVisionImage.fromBitmap(myBitmap)
        val textRecognizer = FirebaseVision.getInstance().onDeviceTextRecognizer

        textRecognizer.processImage(textImage)
            .addOnSuccessListener {
                for (block in it.textBlocks) {
                    for (line in block.lines) {
                        for (element in line.elements) {
                            showKTP(element.text)
                        }
                    }
                }

                if (ktp == "") Toast.makeText(
                    this@CameraXActivity,
                    "KTP not found",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnFailureListener {

            }
    }

    private fun drawImageCanvas() {
        drawImageCanvas = DrawImageCanvas(this)

        addContentView(
            drawImageCanvas,
            ActionBar.LayoutParams(
                ActionBar.LayoutParams.MATCH_PARENT,
                ActionBar.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun croppedImage(data: ByteArray): Bitmap {
        val imageOriginal = BitmapFactory.decodeByteArray(data, 0, data.size, null)
        val rotationMatrix = Matrix()
        rotationMatrix.postRotate(0F) // for orientation

        // SM-J530Y
        return Bitmap.createBitmap(
            imageOriginal,
            570,
            600,
            2650,
            1700,
            rotationMatrix,
            false
        )

        // SM-G532G
//        return Bitmap.createBitmap(
//            imageOriginal,
//            450,
//            500,
//            2150,
//            1300,
//            rotationMatrix,
//            false
//        )
    }

    private fun saveBitmap(bmp: Bitmap): File {
        val bytes = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 60, bytes);
        val file = File(this.downloadDirectory + "/${System.currentTimeMillis()}.jpg")
        file.createNewFile()
        FileOutputStream(file).apply {
            write(bytes.toByteArray())
            close()
        }

        recognizeText(file.absolutePath)
        return file
    }
}
