package com.example.editor.edit.data

data class CurrentImageList(
    val imageList: List<ImageData> = listOf(),
    val currentIndex: Int = 0,//当前移动的图
)