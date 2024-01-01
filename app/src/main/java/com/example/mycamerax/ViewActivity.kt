package com.example.mycamerax

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.mycamerax.databinding.ActivityViewBinding

class ViewActivity:AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val contentView =
            DataBindingUtil.setContentView<ActivityViewBinding>(this, R.layout.activity_view)

        contentView.editView.apply {
            initView(BitmapFactory.decodeResource(resources,R.mipmap.ic_editor))
            addImage(BitmapFactory.decodeResource(resources,R.mipmap.icon11))
        }



    }
}