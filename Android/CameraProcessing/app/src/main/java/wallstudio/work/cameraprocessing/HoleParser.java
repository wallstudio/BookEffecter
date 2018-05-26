package wallstudio.work.cameraprocessing;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.List;

public class HoleParser {

    public interface HoleParserCallBack{
        void playSound(String url);
        void refleshText(String message);
    }

    private HoleParserCallBack mHoleParserCallBack;

    private Bitmap mRecycleBitmap;
    private Mat mFrame;
    private int mHoleCount;
    private String mMessage = "";

    public HoleParser(Bitmap bitmap, HoleParserCallBack holeParserCallBack){
        mHoleParserCallBack = holeParserCallBack;

        mRecycleBitmap = bitmap;
        mFrame = new Mat();
        Utils.bitmapToMat(bitmap, mFrame, false);
        preProcess();
        mHoleCount = countHole();
        mHoleCount = smooth2(mHoleCount);
        if(mBufferForSmooth.size() == BUFFER_SIZE)
            action(mHoleCount);
        Utils.matToBitmap(mFrame,bitmap,false);
        mHoleParserCallBack.refleshText(mMessage);
    }

    public Bitmap getProcessedBitmap(){
        Utils.matToBitmap(mFrame, mRecycleBitmap,false);
        return mRecycleBitmap;
    }

    private void preProcess(){
        // グレースケール
        Imgproc.cvtColor(mFrame,mFrame,Imgproc.COLOR_RGB2GRAY);
        // 二値化手法を切り替え
        Core.MinMaxLocResult minMax = Core.minMaxLoc(mFrame);
        double ave = Core.mean(mFrame).val[0];
        mMessage += String.format("range=%d-%d, ave=%d, ",
                (int)minMax.minVal,(int)minMax.maxVal,(int)ave);
        if(minMax.maxVal < 23){
            // 固定閾値
            Imgproc.threshold(mFrame,mFrame,10.0, 255.0,
                    Imgproc.THRESH_BINARY);
            mMessage += "algo=FIX, ";
        }else {
            // 大津の二値化
            Imgproc.threshold(mFrame, mFrame, 0.0, 255.0,
                    Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
            mMessage += "algo=OTSU, ";
        }
        // ノイズ処理
        Mat kernel =  Mat.ones(3, 3, CvType.CV_8UC1);
        Point anchor = new Point(-1,-1);
        Imgproc.morphologyEx(mFrame, mFrame,
                Imgproc.MORPH_CLOSE, kernel,anchor ,1);
        Imgproc.morphologyEx(mFrame, mFrame,
                Imgproc.MORPH_OPEN, kernel,anchor ,1);
        Imgproc.erode(mFrame,mFrame,kernel, anchor, 4);
    }

    private int countHole(){
        // 輪郭でラベリング
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(mFrame.clone(), contours,hierarchy,
                Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_SIMPLE);
        // 出力にオーバーレイ
        Imgproc.cvtColor(mFrame,mFrame,Imgproc.COLOR_GRAY2BGR);
        for(int i=0; i<contours.size(); i++){

            Moments moments = Imgproc.moments(contours.get(i));
            int x = (int)(moments.m10/moments.m00);
            int y = (int)(moments.m01/moments.m00);
            Imgproc.circle(mFrame,new Point(x,y),3,new Scalar(50,50,200),3);
        }
        mMessage += "cnt=" + contours.size() +", ";
        return contours.size();
    }

    private static final int BUFFER_SIZE = 8;
    private static List<Integer> mBufferForSmooth = new ArrayList<>();
    private int smooth(int value){
        mBufferForSmooth.add(value);
        while (mBufferForSmooth.size() > BUFFER_SIZE)
            mBufferForSmooth.remove(0);
        if(mBufferForSmooth.size() == BUFFER_SIZE) {
            int max = -1;
            for (Integer i : mBufferForSmooth)
                if (max < i) max = i;

            int[] bins = new int[max+1];
            for (Integer i : bins)
                i = 0;

            for (Integer i : mBufferForSmooth)
                bins[i]++;

            max = -1;
            int maxIndex = -1;
            for(int i=0; i<bins.length;i++)
                if (max < bins[i]) maxIndex = i;

            mMessage += String.format("smooth=%d (%s), ", maxIndex, mBufferForSmooth.toString());
            return maxIndex;
        }else{return  value;}
    }
    private int smooth2(int value){
        mBufferForSmooth.add(value);
        while (mBufferForSmooth.size() > BUFFER_SIZE)
            mBufferForSmooth.remove(0);
        if(mBufferForSmooth.size() == BUFFER_SIZE) {

            int pre = mBufferForSmooth.get(0);
            boolean isSame = true;
            for(Integer i :mBufferForSmooth) {
                if (pre != i) {
                    isSame = false;
                    break;
                }
            }

            int ret = isSame? pre : -1;

            mMessage += String.format("smooth=%d (%s), ", ret, mBufferForSmooth.toString());
            return ret;
        }else{return  -1;}
    }

    private static int mPreCount = -1;
    private void action(int count){
        if(mPreCount >= 0){
            if(mPreCount != count){
                Log.d("HOLE",String.format("CHANGE %d -> %d",mPreCount,count));
                switch (count){
                    case 0:mHoleParserCallBack.playSound("yukarisan.mp3");break;
                    case 1:mHoleParserCallBack.playSound("count1.mp3");break;
                    case 2:mHoleParserCallBack.playSound("count2.mp3");break;
                    case 3:mHoleParserCallBack.playSound("tts0.mp3");break;
                    case 4:mHoleParserCallBack.playSound("count4.mp3");break;
                    case 5:mHoleParserCallBack.playSound("count5.mp3");break;
                    case 6:mHoleParserCallBack.playSound("tts1.mp3");break;
                    case 7:mHoleParserCallBack.playSound("count7.mp3");break;
                    case 8:mHoleParserCallBack.playSound("count8.mp3");break;
                    case 9:mHoleParserCallBack.playSound("tts2.mp3");break;
                    default:break;
                }
            }
        }
        mPreCount = count < 0 ? mPreCount : count;
    }

    public final static String text = "民安ともえ(v1)＞けだまきまき、の育てかた？\n" +
            "/\n" +
            "琴葉 茜＞けだマキマキ、掛ける結月ゆかり\n" +
            "結月ゆかり＞けだまきちゃんは自慢の毛並みを保つため、毎日その体毛が入れ替わり、大量の毛が抜け落ちます。\n" +
            "結月ゆかり＞こまめに掃除をしてあげましょう。\n" +
            "結月ゆかり＞けだまきちゃんにとって心地よい環境を整えてあげることが絆を深める第一歩となります。\n" +
            "/\n" +
            "琴葉 葵＞けだマキマキ、掛ける弦巻マキ\n" +
            "民安ともえ(v1)＞ギューン\n" +
            "民安ともえ(v1)＞おや、何かを伝えたいのでしょうか？\n" +
            "民安ともえ(v1)＞お腹がすいているようですね？大好物であるラザニアを食べさせてあげると、きっと喜ぶでしょう？";
}
