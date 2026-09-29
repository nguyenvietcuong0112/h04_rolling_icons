package com.iconchanger.rollingicons.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.iconchanger.rollingicons.R
import com.iconchanger.rollingicons.widget.ZoomableImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhotoShowActivity : BaseActivity() {

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_show)

        val zoomImageView = findViewById<ZoomableImageView>(R.id.zoomImageView)
        val btnClose = findViewById<ImageButton>(R.id.btnClose)

        btnClose.setOnClickListener {
            finish()
        }

        val uriString = intent.getStringExtra("photo_uri")
        if (uriString.isNullOrEmpty()) {
            finish()
            return
        }

        scope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val uri = Uri.parse(uriString)
                    val inputStream = contentResolver.openInputStream(uri)
                    
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeStream(inputStream, null, options)
                    inputStream?.close()

                    val screenW = resources.displayMetrics.widthPixels
                    val screenH = resources.displayMetrics.heightPixels
                    var inSampleSize = 1
                    val maxDim = Math.max(screenW, screenH)
                    while ((options.outWidth / inSampleSize) > maxDim || (options.outHeight / inSampleSize) > maxDim) {
                        inSampleSize *= 2
                    }

                    val decodeOptions = BitmapFactory.Options().apply {
                        this.inSampleSize = inSampleSize
                    }
                    val finalInputStream = contentResolver.openInputStream(uri)
                    val finalBitmap = BitmapFactory.decodeStream(finalInputStream, null, decodeOptions)
                    finalInputStream?.close()
                    finalBitmap
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            if (bitmap != null) {
                zoomImageView.setImageBitmap(bitmap)
            } else {
                finish()
            }
        }
    }
}

