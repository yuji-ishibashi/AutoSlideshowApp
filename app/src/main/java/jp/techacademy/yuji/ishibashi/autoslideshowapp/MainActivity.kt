package jp.techacademy.yuji.ishibashi.autoslideshowapp

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.provider.MediaStore
import android.content.ContentUris
import android.database.Cursor
import android.os.Handler
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val TAG: String = "AutoSlideShowApp"

    private var mTimer: Timer? = null

    // タイマー用の時間のための変数
    private var mTimerSec = 0.0

    private var mHandler = Handler()

    private val PERMISSIONS_REQUEST_CODE = 100

    private val BUTTON_MESSAGE_START: String = "開始"

    private val BUTTON_MESSAGE_STOP: String = "停止"

    private var cursor: Cursor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate start")
        setContentView(R.layout.activity_main)

        button_next.setOnClickListener(this)
        button_back.setOnClickListener(this)
        button_start_stop.setOnClickListener(this)

        if(checkPermission()){
            // 許可されている
            init()
        } else {
            // 許可されていないので許可ダイアログを表示する
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_CODE)
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause start")

        //スライドショー実行中の場合は停止させる
        if (mTimer != null){
            stopSlideShow()
        }

        if(cursor != null) {
            cursor!!.close()
            cursor = null
        }
    }

    override fun onClick(v: View) {
        Log.d(TAG, "onClick start")
        if(checkPermission()){
            //権限拒否した後、設定から許可された場合はこのルート。
            //一度初期化されている可能性があるため再度初期処理を実行する
            if(cursor == null) {
                setupCursor()
            }
        } else {
            //許可されていない場合はその旨を通知して処理終了
            Toast.makeText(this, "権限が許可されていないため操作できません。", Toast.LENGTH_LONG).show()
            return
        }

        when(v.id){
            R.id.button_next -> next()
            R.id.button_back -> back()
            R.id.button_start_stop ->
                if(button_start_stop.text.toString() == BUTTON_MESSAGE_START) {
                    Log.d(TAG, "push start button")
                    startSlideShow()
                } else if(button_start_stop.text.toString() == BUTTON_MESSAGE_STOP){
                    Log.d(TAG, "push stop button")
                    stopSlideShow()
                } else {
                    Log.d(TAG, "unknown button text")
                    //何らかの原因でボタンの文字による判別ができない場合、タイマーで判別する。
                    if(mTimer == null) {
                        startSlideShow()
                    } else {
                        stopSlideShow()
                    }
                }
        }
    }

    /**
     * 権限を確認する関数
     * @return　true：権限が許可されている、false：権限が許可されていない
     */
    private fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE ->
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    init()
                }
        }
    }

    /**
     * 初期処理を行う関数
     * cursorを用意し、最初の画像をImageViewに設定する
     */
    private fun init() {
        Log.d(TAG, "init start")
        if(cursor == null) {
            setupCursor()
        }

        if (cursor!!.moveToFirst()) {
            // indexからIDを取得し、そのIDから画像のURIを取得する
            val fieldIndex = cursor!!.getColumnIndex(MediaStore.Images.Media._ID)
            val id = cursor!!.getLong(fieldIndex)
            val imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

            imageView.setImageURI(imageUri)
        }
    }

    /**
     * cursorを準備する関数
     */
    private fun setupCursor() {
        Log.d(TAG, "setupCursor start")
        val resolver = contentResolver
        cursor = resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // データの種類
            null, // 項目（null = 全項目）
            null, // フィルタ条件（null = フィルタなし）
            null, // フィルタ用パラメータ
            null // ソート (nullソートなし）
        )
    }

    /**
     * 1枚後の保存画像を表示する関数
     */
    private fun next() {
        Log.d(TAG, "next start")

        if(cursor!!.moveToNext()) {
            setupImageView(cursor!!)
        } else {
            //moveToNextがfalse == 最後のため、先頭に戻る
            if (cursor!!.moveToFirst()) {
                setupImageView(cursor!!)
            }
        }
    }

    /**
     * 1枚前の保存画像を表示する関数
     */
    private fun back() {
        Log.d(TAG, "back start")

        if(cursor!!.moveToPrevious()) {
            setupImageView(cursor!!)
        } else {
            //moveToPreviousがfalse == 最初のため、最後に戻る
            if (cursor!!.moveToLast()) {
                setupImageView(cursor!!)
            }
        }
    }

    /**
     * スライドショーを開始する関数
     */
    private fun startSlideShow() {
        Log.d(TAG, "startSlideShow start")
        if (mTimer == null){
            mTimer = Timer()
            mTimer!!.schedule(object : TimerTask() {
                override fun run() {
                    mHandler.post {
                        next()
                    }
                }
            }, 2000, 2000) // 最初に始動させるまで2000ミリ秒、ループの間隔を2000ミリ秒 に設定
        }

        //ボタンの文字を「停止」に変更
        button_start_stop.text = BUTTON_MESSAGE_STOP

        //進むボタンと戻るボタンの非活性化
        button_next.isClickable = false
        button_back.isClickable = false
    }

    /**
     * スライドショーを停止する関数
     */
    private fun stopSlideShow() {
        Log.d(TAG, "stopSlideShow start")
        if (mTimer != null){
            mTimer!!.cancel()
            mTimer = null
        }

        //ボタンの文字を「開始」に変更
        button_start_stop.text = BUTTON_MESSAGE_START

        //進むボタンと戻るボタンの活性化
        button_next.isClickable = true
        button_back.isClickable = true
    }

    private fun setupImageView(cursor: Cursor){
        // indexからIDを取得し、そのIDから画像のURIを取得する
        val fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID)
        val id = cursor.getLong(fieldIndex)
        val imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

        imageView.setImageURI(imageUri)
    }
}