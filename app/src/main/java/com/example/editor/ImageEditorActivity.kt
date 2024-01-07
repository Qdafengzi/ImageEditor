package com.example.editor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.editor.imageeditlibrary.editimage.EditImageFragment

class ImageEditorActivity:AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_editor)
        supportFragmentManager.beginTransaction()
            .add(R.id.frameLayout,EditImageFragment())
            .commitAllowingStateLoss()
    }


}