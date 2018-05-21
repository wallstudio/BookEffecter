package wallstudio.work.myapplication;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.w3c.dom.Text;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity
        implements CameraBridgeViewBase.CvCameraViewListener{

    private CameraBridgeViewBase mCameraView;
    private TextView mTextView;
    private MediaPlayer mMediaPlayer;
    private Handler mHandler;

    private BaseLoaderCallback mLoaderCallback
            = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS)
                mCameraView.enableView();
            else
                super.onManagerConnected(status);
        }
    };

    void PlaySound(final String url, final boolean isInternet){
        if(mMediaPlayer.isPlaying())
            mMediaPlayer.stop();
        mMediaPlayer.release();
        mMediaPlayer = new MediaPlayer();
        try {
            AssetFileDescriptor afd = getAssets().openFd(url);
            mMediaPlayer.setDataSource(afd.getFileDescriptor());
            mMediaPlayer.prepare();
            mMediaPlayer.start();
            Log.d("SOUND", "Play start \""+ url + "\"");
        }catch (IOException e){
            Log.e("ERROR", "Cann't load audio file.");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = findViewById(R.id.view_text);
        mTextView.setText("Initialize");
        mMediaPlayer = new MediaPlayer();
        mHandler = new Handler();

        mCameraView =findViewById(R.id.camera_view);
        mCameraView.setCvCameraViewListener(this);
    }

    static {
        // ネイティブライブラリ読み込み
        System.loadLibrary("opencv_java3");
    }

    @Override
    protected void onResume() {
        super.onResume();

        mLoaderCallback.onManagerConnected
                (LoaderCallbackInterface.SUCCESS);

        //mCameraView.setMaxFrameSize(200,200);

        // OpenCV Managerつかうならこっち
        //
        // OpenCVLoader.initDebug()で切り分けるのがお行儀良い
        //OpenCVLoader.initAsync(
        //        OpenCVLoader.OPENCV_VERSION_3_0_0,
        //        this, mLoaderCallback);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.d("CvCamera","Start");
    }
    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(Mat inputFrame) {

        String message = "";

        Mat frame = inputFrame.clone();
        // グレースケール
        Imgproc.cvtColor(frame,frame,Imgproc.COLOR_RGB2GRAY);
        // 二値化手法を切り替え
        Core.MinMaxLocResult minMax = Core.minMaxLoc(frame);
        double ave = Core.mean(frame).val[0];
        message += String.format("range=%d-%d, ave=%d, ",
                (int)minMax.minVal,(int)minMax.maxVal,(int)ave);
        if(minMax.maxVal < 23){
            // 固定閾値
            Imgproc.threshold(frame,frame,10.0, 255.0,
                    Imgproc.THRESH_BINARY);
            message += "algo=FIX, ";
        }else {
            // 大津の二値化
            Imgproc.threshold(frame, frame, 0.0, 255.0,
                    Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
            message += "algo=OTSU, ";
        }
        // ノイズ処理
        final Mat kernel =  Mat.ones(3, 3, CvType.CV_8UC1);
        final Point anchor = new Point(-1,-1);
        Imgproc.morphologyEx(frame, frame,
                Imgproc.MORPH_CLOSE, kernel,anchor ,1);
        Imgproc.morphologyEx(frame, frame,
                Imgproc.MORPH_OPEN, kernel,anchor ,1);
        // ハフ変換でエン検出
//        Mat circles = new Mat();// 検出した円の情報格納する変数
//        Imgproc.HoughCircles(frame, circles, Imgproc.CV_HOUGH_GRADIENT,
//                2, 10, 160, 30, 7, 20);
//        Point pt = new Point();
//        // 検出した直線上を緑線で塗る
//        Imgproc.cvtColor(frame,frame,Imgproc.COLOR_GRAY2BGR);
//        for (int i = 0; i < circles.cols(); i++){
//            double data[] = circles.get(0, i);
//            pt.x = data[0];
//            pt.y = data[1];
//            double rho = data[2];
//            Imgproc.circle(frame, pt, (int)rho, new Scalar(0, 200, 0), 5);
//        }
//        message += "cnt=" + circles.toString() + ", ";
        // 輪郭でラベリング
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(frame, contours,hierarchy,
                Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_SIMPLE);
        // 出力にオーバーレイ
        Imgproc.cvtColor(frame,frame,Imgproc.COLOR_GRAY2BGR);
        for(int i=0; i<contours.size(); i++){
            Moments moments = Imgproc.moments(contours.get(i));
            int x = (int)(moments.m10/moments.m00);
            int y = (int)(moments.m01/moments.m00);
            Imgproc.circle(frame,new Point(x,y),3,new Scalar(50,50,200),5);
        }
        message += "cnt=" + contours.size() +", ";

        refrashMessage(message);
        Action(contours.size());
        return frame; // 30fps固定らしい
    }

    void refrashMessage(final String message){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mTextView.setText(message);
            }
        });
    }

    void Action(final int count){

        if(count ==3){
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    PlaySound("yukarisan.mp3", false);
                }
            });
        }
    }

}