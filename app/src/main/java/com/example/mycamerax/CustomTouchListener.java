package com.example.mycamerax;

public interface CustomTouchListener {
	/**
		* 放大
		*/
	void zoom();

	/**
		* 缩小
		*/
	void ZoomOut();

	/**
		* 点击
		*/
	void click(float x, float y);

	/**
		* 双击
		*/
	void doubleClick(float x, float y);

	/**
		* 长按
		*/
	void longClick(float x, float y);
}