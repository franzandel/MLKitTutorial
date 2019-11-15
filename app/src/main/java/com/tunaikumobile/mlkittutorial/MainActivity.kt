package com.tunaikumobile.mlkittutorial

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.squareup.picasso.Picasso
import com.theartofdev.edmodo.cropper.CropImage
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.alert

// 1. Try implement CameraX
// 2. Try make a canvas for KTP (Concept like Barcode Scanner only half screen & without taking photo)
class MainActivity : AppCompatActivity() {

    private var ktp = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupUI()
    }

    private fun setupUI() {
        Picasso.get().load("https://storage.lapor.go.id/app/uploads/public/5ca/186/087/5ca18608787c7443980304.jpg")
            .into(image_holder)

        btnLoadImage.setOnClickListener {
            Picasso.get().load("https://storage.lapor.go.id/app/uploads/public/5ca/186/087/5ca18608787c7443980304.jpg")
                .into(image_holder)
        }

        btnTakePicture.setOnClickListener {
            CropImage.activity().start(this)
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)

            if (resultCode == Activity.RESULT_OK) {
                val imageUri = result.uri
                highlightTextInImage(MediaStore.Images.Media.getBitmap(contentResolver, imageUri))
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Toast.makeText(this, "There was some error : ${result.error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun highlightTextInImage(image: Bitmap?) {
        if (image == null) {
            Toast.makeText(this, "There was some error", Toast.LENGTH_SHORT).show()
            return
        }

        image_holder.setImageBitmap(image)

        val textImage = FirebaseVisionImage.fromBitmap(image)
        val textRecognizer = FirebaseVision.getInstance().onDeviceTextRecognizer

        textRecognizer.processImage(textImage)
            .addOnSuccessListener {
                val mutableImage = image.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(mutableImage)
                val rectPaint = Paint().apply {
                    color = Color.RED
                    style = Paint.Style.STROKE
                    strokeWidth = 4F
                }
                val textPaint = Paint().apply {
                    color = Color.RED
                    textSize = 40F
                }

                var index = 0
                for (block in it.textBlocks) {
                    for (line in block.lines) {
                        for (element in line.elements) {
                            canvas.drawRect(element.boundingBox!!, rectPaint)
                            canvas.drawText(index.toString(), element.cornerPoints!![2].x.toFloat(), element.cornerPoints!![2].y.toFloat(), textPaint)
                            showKTP(element.text)
                        }
                    }
                }

                image_holder.setImageBitmap(mutableImage)
                if (ktp == "") Toast.makeText(this, "KTP not found", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener {

            }
    }

    fun recognizeText(v: View) {
        val textImage = FirebaseVisionImage.fromBitmap(
            (image_holder.drawable as BitmapDrawable).bitmap
        )

        val textRecognizer = FirebaseVision.getInstance().onDeviceTextRecognizer

//        Paid version
//        val textRecognizer = FirebaseVision.getInstance().cloudTextRecognizer

//        Provide different Language for Cloud Based
//        val options = FirebaseVisionCloudTextRecognizerOptions.Builder()
//            .setLanguageHints(Arrays.asList("en", "hi"))
//            .build()
//        val textRecognizer = FirebaseVision.getInstance().getCloudTextRecognizer(options)

        textRecognizer.processImage(textImage)
            .addOnSuccessListener {
                onSuccessRecognize(it)
            }
            .addOnFailureListener {

            }
    }

    private fun onSuccessRecognize(firebaseVisionText: FirebaseVisionText) {
        val resultText = firebaseVisionText.text
        for (block in firebaseVisionText.textBlocks) {
            val blockText = block.text
            val blockConfidence = block.confidence
            val blockCornerPoints = block.cornerPoints
            val blockFrame = block.boundingBox

            for (line in block.lines) {
                val lineText = line.text
                val lineConfidence = line.confidence
                val lineCornerPoints = line.cornerPoints
                val lineFrame = line.boundingBox

                for (element in line.elements) {
                    val elementText = element.text
                    val elementConfidence = element.confidence
                    val elementCornerPoints = element.cornerPoints
                    val elementFrame = element.boundingBox

                    showKTP(elementText)
                }
            }
        }

        if (ktp == "") Toast.makeText(this, "KTP not found", Toast.LENGTH_LONG).show()
    }

    private fun showKTP(elementText: String) {
        val numeric = elementText.matches("-?\\d+(\\.\\d+)?".toRegex())

        if (numeric) {
            if (elementText.length in 15..17) {
                runOnUiThread {
                    ktp = elementText
                    alert(elementText, "Text").show()
                }
            }
        }
    }
}
