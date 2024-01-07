package com.example.editor.imageeditlibrary.editimage

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.editor.R
import com.example.editor.XLogger
import com.example.editor.databinding.ActivityImageEditBinding
import com.example.editor.imageeditlibrary.editimage.view.imagezoom.ImageViewTouchBase
import com.example.editor.imageeditlibrary.editimage.widget.RedoUndoController
import java.io.File
import java.io.IOException

/**
 *
 *
 * 图片编辑 主页面
 *
 * @author panyi
 *
 *
 * 包含 1.贴图 2.滤镜 3.剪裁 4.底图旋转 功能
 * add new modules
 */
class EditImageFragment : Fragment() {
    var saveFilePath: String? = null // 生成的新图片路径
    var mode = MODE_NONE // 当前操作模式
    protected var mOpTimes = 0
    protected var isBeenSaved = false
    var mainBit: Bitmap? = null // 底层显示Bitmap
        private set
    private var mRedoUndoController: RedoUndoController? = null //撤销操作
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_image_edit, container, false)
    }

    lateinit var mBinding: ActivityImageEditBinding
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding = DataBindingUtil.bind(view)!!
        initView()
        data
    }

    private val data: Unit
        private get() {
            saveFilePath = File(requireContext().cacheDir.absolutePath, "tietu" + System.currentTimeMillis() + ".png").absolutePath
            val bitmap = BitmapFactory.decodeResource(this.resources, R.drawable.ic_editor)
            changeMainBitmap(bitmap, false)
        }

    private fun initView() {
        val metrics = resources.displayMetrics
        mBinding.bannerFlipper.setInAnimation(context, R.anim.in_bottom_to_top)
        mBinding.bannerFlipper.setOutAnimation(context, R.anim.out_bottom_to_top)
        mBinding.apply.setOnClickListener(ApplyBtnClick())
        mBinding.saveBtn.setOnClickListener{

        }
        mBinding.backBtn.setOnClickListener {
            //onBackPressed();
        }
        mBinding.mainImage.viewTreeObserver.addOnGlobalLayoutListener {
            mBinding.imageGroup.setRootImageRect(
                mBinding.mainImage.rootImageRect,
                mBinding.mainImage.width,
                mBinding.mainImage.height
            )
        }
        mBinding.btnImage.setOnClickListener {
            mBinding.imageGroup.visibility = View.VISIBLE
            mBinding.textItem.visibility = View.GONE
            val bitmap = BitmapFactory.decodeResource(resources, R.mipmap.icon11)
            mBinding.imageGroup.addBitImage(bitmap)
            XLogger.d("add image")
        }
        mBinding.btnText.setOnClickListener {
            mBinding.imageGroup.visibility = View.GONE
            mBinding.textItem.visibility = View.VISIBLE
            //                mAddTextItemView.setText("哈哈哈哈");
            mBinding.textItem.setText("哈哈哈哈\n嘻嘻嘻嘻嘻嘻\n啦啦啦啦啦啦")
        }
        mBinding.mainImage.setFlingListener { e1, e2, velocityX, velocityY ->
            if (velocityY > 1) {
                closeInputMethod()
            }
        }
        mRedoUndoController = RedoUndoController(this, mBinding.redoUodoPanel)
    }

    private fun getImageFromAssetsFile(fileName: String): Bitmap? {
        var image: Bitmap? = null
        val am = resources.assets
        try {
            val `is` = am.open(fileName)
            image = BitmapFactory.decodeStream(`is`)
            `is`.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return image
    }

    /**
     * 关闭输入法
     */
    private fun closeInputMethod() {
//        if (mAddTextFragment.isAdded()) {
//            mAddTextFragment.hideInput();
//        }
    }
    /**
     * 应用按钮点击
     *
     * @author panyi
     */
    private inner class ApplyBtnClick : View.OnClickListener {
        override fun onClick(v: View) {
            when (mode) {
                MODE_STICKERS -> {}
                MODE_TEXT -> {}
                else -> {}
            }
        }
    } // end inner class


    /**
     * @param newBit
     * @param needPushUndoStack
     */
    fun changeMainBitmap(newBit: Bitmap?, needPushUndoStack: Boolean) {
        if (newBit == null) return
        if (mainBit == null || mainBit != newBit) {
            if (needPushUndoStack) {
                mRedoUndoController!!.switchMainBit(mainBit, newBit)
                increaseOpTimes()
            }
            mainBit = newBit
            mBinding.mainImage.setImageBitmap(mainBit)
            mBinding.mainImage.displayType = ImageViewTouchBase.DisplayType.FIT_TO_SCREEN
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (mRedoUndoController != null) {
            mRedoUndoController!!.onDestroy()
        }
    }

    fun increaseOpTimes() {
        mOpTimes++
        isBeenSaved = false
    }

    fun resetOpTimes() {
        isBeenSaved = true
    }

    fun canAutoExit(): Boolean {
        return isBeenSaved || mOpTimes == 0
    }

    protected fun onSaveTaskDone() {
        //tODO:
//        Intent returnIntent = new Intent();
//        returnIntent.putExtra(FILE_PATH, filePath);
//        returnIntent.putExtra(EXTRA_OUTPUT, saveFilePath);
//        returnIntent.putExtra(IMAGE_IS_EDIT, mOpTimes > 0);
//
//        FileUtil.ablumUpdate(this, saveFilePath);
//        setResult(RESULT_OK, returnIntent);
//        finish();
    }


    companion object {
        const val FILE_PATH = "file_path"
        const val EXTRA_OUTPUT = "extra_output"
        const val SAVE_FILE_PATH = "save_file_path"
        const val IMAGE_IS_EDIT = "image_is_edit"
        const val MODE_NONE = 0
        const val MODE_STICKERS = 1 // 贴图模式
        const val MODE_TEXT = 5 // 文字模式

        /**
         * @param context
         * @param editImagePath
         * @param outputPath
         * @param requestCode
         */
        fun start(context: Activity, editImagePath: String?, outputPath: String?, requestCode: Int) {
            if (TextUtils.isEmpty(editImagePath)) {
                Toast.makeText(context, R.string.no_choose, Toast.LENGTH_SHORT).show()
                return
            }
            val it = Intent(context, EditImageFragment::class.java)
            it.putExtra(FILE_PATH, editImagePath)
            it.putExtra(EXTRA_OUTPUT, outputPath)
            context.startActivityForResult(it, requestCode)
        }
    }
}
