package com.tunaikumobile.mlkittutorial.CameraKit

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Camera
import android.os.Build
import android.os.Bundle
import android.os.Environment.getExternalStorageDirectory
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.camerakit.CameraKitView
import com.tunaikumobile.mlkittutorial.R
import kotlinx.android.synthetic.main.activity_camera_kit.*
import java.io.File
import java.io.FileOutputStream


class CameraKitActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_kit)

        btnCapture.setOnClickListener {
            if (haveStoragePermission()) {
                cameraKitView.captureImage { cameraKitView, capturedImageBytes ->
                    val downloadDirectory = File(getExternalStorageDirectory().toString() + "/MLKit")
                    // have the object build the directory structure, if needed.
                    if (!downloadDirectory.exists()) {
                        downloadDirectory.mkdirs()
                    } else {
                        downloadDirectory.deleteRecursively()
                    }

                    val savedPhoto = File(getExternalStorageDirectory().toString() + "/MLKit/photo.jpg")
                    try {
                        val outputStream = FileOutputStream(savedPhoto.path)
                        outputStream.write(capturedImageBytes)
                        outputStream.close()
                        Toast.makeText(this, savedPhoto.path, Toast.LENGTH_LONG).show()
                        Log.d("1234", savedPhoto.path)
                    } catch (e: java.io.IOException) {
                        e.printStackTrace()
                    }

                    Toast.makeText(this, "tester", Toast.LENGTH_LONG).show()
                    Log.d("1234", "tester")
                }
            }
        }
    }

    private fun haveStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.e("Permission error", "You have permission")
                true
            } else {
                Log.e("Permission error", "You have asked for permission")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    1
                )
                false
            }
        } else { //you dont need to worry about these stuff below api level 23
            Log.e("Permission error", "You already have the permission")
            true
        }
    }

    override fun onStart() {
        super.onStart()
        cameraKitView.onStart()
    }

    override fun onResume() {
        super.onResume()
        cameraKitView.onResume()
    }

    override fun onPause() {
        cameraKitView.onPause()
        super.onPause()
    }

    override fun onStop() {
        cameraKitView.onStop()
        super.onStop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraKitView.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
