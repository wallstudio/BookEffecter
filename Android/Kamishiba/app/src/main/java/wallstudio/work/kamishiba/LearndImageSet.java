package wallstudio.work.kamishiba;

import org.opencv.core.DMatch;
import org.opencv.core.MatOfDMatch;
import org.opencv.features2d.AKAZE;
import org.opencv.features2d.BFMatcher;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.AKAZE;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.Feature2D;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class LearndImageSet {

    public  class SearchResult{
        public LearndImage learndImage;
        public double bestScore;
        public List<Float> scores;
        public Mat resultImage;
    }

    public String path;
    public List<String> imageNames;
    public List<String> imagePaths;
    public BFMatcher bfMatcher;
    public List<LearndImage> learndImages;

    public  LearndImageSet(String directoryPath){
        path = directoryPath;
        imageNames = new ArrayList<String>(); //TODO: file list for android
        imagePaths = imageNames;
        bfMatcher = BFMatcher.create(Core.NORM_HAMMING, true);
        calc();
    }

    private  void calc(){
        learndImages = new ArrayList<LearndImage>();
        for(String imagePath : imagePaths){
            LearndImage learndImage = new LearndImage(imagePath);
            learndImages.add(learndImage);
        }
    }

    public SearchResult search(Mat inputImage, boolean isDrawResult){

        LearndImage learndInputImage = new LearndImage(inputImage);
        MatOfKeyPoint keypoint_i = learndInputImage.keyPoints;
        Mat descriptor_i = learndInputImage.descriptors;
        float bestScore = 10000000.0f;
        DMatch[] bestMatch = new DMatch[0];
        LearndImage best = null;
        List<Float> scores = new ArrayList<Float>();
        for(LearndImage learndImage: learndImages) {
            MatOfKeyPoint keypoint_d = learndImage.keyPoints;
            Mat descriptor_d = learndImage.descriptors;
            MatOfDMatch matchesMat = new MatOfDMatch();
            bfMatcher.match(descriptor_i, descriptor_d, matchesMat);
            DMatch[] matchies = matchesMat.toArray();
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
                best = learndImage;
                bestMatch = matchies;
            }
        }

        SearchResult result = new SearchResult();
        result.learndImage = best;
        result.bestScore = bestScore;
        result.scores = scores;
        result.resultImage = null;

        if(isDrawResult){
            result.resultImage = drawResult(best, learndInputImage, bestMatch, bestScore);
        }

        return  result;
    }

    private Mat drawResult(LearndImage dataImage, LearndImage inputImage, DMatch[] matches, float score){
        Mat resultImage = new Mat();
        MatOfDMatch matchMat= new MatOfDMatch();
        DMatch[] subMatchs = Arrays.copyOf(matches, matches.length < 10 ? matches.length : 10);
        matchMat.fromArray(matches);
        Features2d.drawMatches(inputImage.image, inputImage.keyPoints, dataImage.image, dataImage.keyPoints, matchMat, resultImage);
        //TODO: put score;
        return resultImage;
    }
}
