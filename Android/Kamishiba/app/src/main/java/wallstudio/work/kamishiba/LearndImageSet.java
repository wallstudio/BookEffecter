package wallstudio.work.kamishiba;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.Scalar;
import org.opencv.features2d.BFMatcher;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.Features2d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class LearndImageSet {

    public static final int DRAW_MATCH_COUT  = 10;
    // TODO: ネット or ストレージから拾うように
    public static final int[] TEST_DATA = new int[]{
            R.drawable.td0,
            R.drawable.td1,
            R.drawable.td2,
            R.drawable.td3,
            R.drawable.td4,
            R.drawable.td5,
            R.drawable.td6,
            R.drawable.td7
    };
    public final Scalar RANDOM_COLOR = Scalar.all(-1);

    public static BFMatcher sBFMatcher;

    public List<LearndImage> learndImages;

    public LearndImage learndInputImage;
    public LearndImage bestLearndImage;
    public double bestScore;
    public List<Float> scores;
    public DMatch[] bestMatch;
    public Mat resultImage = new Mat();


    public  LearndImageSet(Resources res){

        if(null == sBFMatcher)
            sBFMatcher = BFMatcher.create(Core.NORM_HAMMING, true);

        learndImages = new ArrayList<>();
        // ref. http://android-java.hatenablog.jp/entry/2016/06/27/054415
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        for(int srcId : TEST_DATA){
            Bitmap bitmap = BitmapFactory.decodeResource(res, srcId, options);
            Mat imageMat = new Mat();
            Utils.bitmapToMat(bitmap, imageMat, false);
            LearndImage learndImage = new LearndImage(imageMat);
            learndImages.add(learndImage);
            bitmap.recycle();
        }
    }



    public void search(Mat inputImage){

        if(learndInputImage != null)
            learndInputImage.release();
        learndInputImage = new LearndImage(inputImage);

        Mat descriptor_i = learndInputImage.descriptors;
        bestScore = Float.MAX_VALUE;
        bestMatch = new DMatch[0];
        bestLearndImage = null;
        List<Float> scores = new ArrayList<Float>();
        for(LearndImage learndImage: learndImages) {
            Mat descriptor_d = learndImage.descriptors;
            MatOfDMatch matchesMat = new MatOfDMatch();
            sBFMatcher.match(descriptor_i, descriptor_d, matchesMat);
            DMatch[] matchies = matchesMat.toArray();
            matchesMat.release();
            Arrays.sort(matchies, new Comparator<DMatch>() {
                @Override
                public int compare(DMatch dMatch, DMatch t1) {
                    float diff = dMatch.distance - t1.distance;
                    if (diff > 0) return 1;
                    if (diff < 0) return -1;
                    return 0;
                }
            });
            float score = 0;
            for (DMatch match : matchies) {
                score += match.distance;
            }
            score /= matchies.length;
            scores.add(score);
            if (score < bestScore) {
                bestScore = score;
                bestLearndImage = learndImage;
                bestMatch = matchies;
            }
        }
    }

    public void drawResult(){
        if(null != bestLearndImage) {
            MatOfDMatch matchMat = new MatOfDMatch();
            MatOfByte matchMask = new MatOfByte();
            DMatch[] subMatchs = Arrays.copyOf(bestMatch, bestMatch.length < DRAW_MATCH_COUT ? bestMatch.length : DRAW_MATCH_COUT);
            matchMat.fromArray(subMatchs);
            byte[] matchMaskArr = new byte[DRAW_MATCH_COUT];
            Arrays.fill(matchMaskArr, (byte)1);
            matchMask.fromArray(matchMaskArr);
            Features2d.drawMatches(
                    learndInputImage.image, learndInputImage.keyPoints,
                    bestLearndImage.image, bestLearndImage.keyPoints,
                    matchMat, resultImage, RANDOM_COLOR, RANDOM_COLOR, matchMask, Features2d.NOT_DRAW_SINGLE_POINTS);
            matchMat.release();
            //TODO: put score;
        }else {
            learndInputImage.image.copyTo(resultImage);
        }
    }

    public void release(){
        for(LearndImage l :learndImages)
            l.release();
        learndInputImage.release();
        bestLearndImage.release();
        resultImage.release();
    }
}
