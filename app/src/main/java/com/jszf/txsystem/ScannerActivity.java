package com.jszf.txsystem;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.zxing.Result;
import com.google.zxing.client.result.ParsedResult;
import com.jszf.txsystem.cache.MemoryCache;
import com.jszf.txsystem.core.mvp.base.BaseActivity;
import com.mylhyl.zxing.scanner.OnScannerCompletionListener;
import com.mylhyl.zxing.scanner.ScannerView;
/**
 * 扫描
 */
public class ScannerActivity extends BaseActivity implements OnScannerCompletionListener{

    public static final String EXTRA_LASER_LINE_MODE = "laser_line_mode";
    public static final int EXTRA_LASER_LINE_MODE_0 = 0;
    public static final int EXTRA_LASER_LINE_MODE_1 = 1;
    public static final int EXTRA_LASER_LINE_MODE_2 = 2;

    private ScannerView mScannerView;
    private Result mLastResult;
    private int laserMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        mScannerView = (ScannerView) findViewById(R.id.scanner);
        mScannerView.setOnScannerCompletionListener(this);
        mScannerView.setText("3");
        hideStatusBar();
        showStatusBar();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            laserMode = extras.getInt(EXTRA_LASER_LINE_MODE);
        }
        mScannerView.setMediaResId(R.raw.beep);//设置扫描成功的声音

//        mScannerView.setLaserFrameTopMargin(100);//扫描框与屏幕上方距离
//        mScannerView.setLaserFrameSize(200, 200);//扫描框大小
//        mScannerView.setLaserFrameCornerLength(25);//设置4角长度
//        mScannerView.setLaserLineHeight(5);//设置扫描线高度

        mScannerView.setLaserGridLineResId(R.drawable.capture_icon_sao);//网格图
        mScannerView.setLaserFrameBoundColor(getResources().getColor(R.color.bg_yzf)); //四边框颜色
    }

    @Override
    protected void onResume() {
        mScannerView.onResume();
        resetStatusView();
        super.onResume();
    }

    @Override
    protected void onPause() {
        mScannerView.onPause();
        super.onPause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (mLastResult != null) {
                    restartPreviewAfterDelay(0L);
                    return true;
                }
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void restartPreviewAfterDelay(long delayMS) {
        mScannerView.restartPreviewAfterDelay(delayMS);
        resetStatusView();
    }

    private void resetStatusView() {
        mLastResult = null;
    }

    @Override
    public void OnScannerCompletion(Result mResult, ParsedResult mParsedResult, Bitmap mBitmap) {
        String result = mResult.getText();
        Toast.makeText(getApplicationContext(),"扫码结果是："+result,Toast.LENGTH_LONG).show();
    }

    private void hideStatusBar() {
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        getWindow().setAttributes(attrs);
    }

    private void showStatusBar() {
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
        getWindow().setAttributes(attrs);
    }
}
