package com.tunaikumobile.mlkittutorial.CameraX

import android.annotation.SuppressLint
import android.graphics.*
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.tunaikumobile.mlkittutorial.R
import kotlinx.android.synthetic.main.activity_picture_ktp.*
import org.jetbrains.anko.alert
import java.io.File
import kotlin.math.abs
import kotlin.math.min
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T




class PictureKtpActivity : AppCompatActivity() {

    private var ktp = ""

    @SuppressLint("WrongThread")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picture_ktp)

        val bundle = intent.extras
        val filePath = bundle?.getString("FILEPATH")

//        val myBitmap = BitmapFactory.decodeFile(filePath)
//        ivKtp.setImageBitmap(myBitmap)

        val byteArray = File(filePath!!).readBytes()
        val croppedImage = croppedImage(byteArray)
        ivKtp.setImageBitmap(croppedImage)

        btnExtract.setOnClickListener {
            val textImage = FirebaseVisionImage.fromBitmap(croppedImage)
            val textRecognizer = FirebaseVision.getInstance().onDeviceTextRecognizer

            textRecognizer.processImage(textImage)
                .addOnSuccessListener {
                    val mutableImage = croppedImage.copy(Bitmap.Config.ARGB_8888, true)
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
                                canvas.drawText(
                                    index.toString(),
                                    element.cornerPoints!![2].x.toFloat(),
                                    element.cornerPoints!![2].y.toFloat(),
                                    textPaint
                                )
                                showKTP(element.text)
                            }
                        }
                    }

                    ivKtp.setImageBitmap(mutableImage)
                    if (ktp == "") Toast.makeText(this, "KTP not found", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener {

                }
        }
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

    private fun croppedImage(data: ByteArray): Bitmap {


        val imageOriginal = BitmapFactory.decodeByteArray(data, 0, data.size, null)
        var width = imageOriginal.width
        var height = imageOriginal.height // for width
        val narrowSize = min(width, height) // for height
        val differ = abs((imageOriginal.height - imageOriginal.width) / 2)  // for dimension
        width = if (width == narrowSize) 0 else differ
        height = if (width == 0) differ else 0

        val rotationMatrix = Matrix()
        rotationMatrix.postRotate(0F) // for orientation

//        return Bitmap.createBitmap(
//            imageOriginal,
//            width,
//            height,
//            narrowSize,
//            narrowSize,
//            rotationMatrix,
//            false
//        )
        return Bitmap.createBitmap(
            imageOriginal,
            570,
            600,
            2650,
            1700,
            rotationMatrix,
            false
        )

//        val imageOriginal = BitmapFactory.decodeByteArray(data, 0, data.size, null)
//        val scale = 1280 / 1000f
//        val left = scale.toInt() * (imageOriginal.getWidth() - 400) / 2
//        val top = scale.toInt() * (imageOriginal.getHeight() - 616) / 2
//        val width = scale.toInt() * 400
//        val height = scale.toInt() * 616
//
//        val rotationMatrix = Matrix()
//        rotationMatrix.postRotate(90F) // for orientation
//
//        return Bitmap.createBitmap(imageOriginal, left, top, width, height, rotationMatrix, false)
    }
}
