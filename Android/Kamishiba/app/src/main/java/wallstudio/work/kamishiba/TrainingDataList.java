package wallstudio.work.kamishiba;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.Scalar;
import org.opencv.features2d.BFMatcher;
import org.opencv.features2d.Features2d;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TrainingDataList extends ArrayList<FeaturedImage> {

    public static final int USE_MATCH_COUNT = 10;
    public final Scalar RANDOM_COLOR = Scalar.all(-1);
    private static BFMatcher sBFMatcher;
    {
        sBFMatcher = BFMatcher.create(Core.NORM_HAMMING, true);
    }

    private List<Double> mScores;
    private List<List<DMatch>> mMatchesList;

    public TrainingDataList(String dir) throws IOException {
        super();

        File[] files = new File(dir).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches("[0-9]{3}\\.jpg");
            }
        });

        for (File file : files)
            add(new FeaturedImage(file.getAbsolutePath()));
    }

    public void release(){
        for(FeaturedImage image : this)
            image.release();
    }

    private int mCalculatedInputHash = 0;
    public int indexOf(FeaturedImage input){

        if(input == null) return -1;

        if(mCalculatedInputHash != input.hashCode()) {
            mCalculatedInputHash = input.hashCode();
            mScores = new ArrayList<>();
            mMatchesList = new ArrayList<>();

            for (FeaturedImage trainImage : this) {
                if (input.keyPoints.rows() > 0 && input.descriptors.rows() > 0
                        && trainImage.keyPoints.rows() > 0 && trainImage.descriptors.rows() > 0) {
                    MatOfDMatch matchesMat = new MatOfDMatch();
                    sBFMatcher.match(input.descriptors, trainImage.descriptors, matchesMat);
                    List<DMatch> matches = matchesMat.toList();
                    matchesMat.release();

                    // 上位 USE_MATCH_COUNT 個の平均をスコアに採用
                    Collections.sort(matches, new Comparator<DMatch>() {
                        @Override
                        public int compare(DMatch a, DMatch b) {
                            if (a.distance == b.distance) return 0;
                            return (a.distance - b.distance > 0) ? 1 : -1;
                        }
                    });
                    matches = matches.subList(0, USE_MATCH_COUNT < matches.size() ? USE_MATCH_COUNT : matches.size());
                    mMatchesList.add(matches);
                    mScores.add(averageMatchDistance(matches));
                } else {
                    mMatchesList.add(new ArrayList<DMatch>());
                    mScores.add(Double.MAX_VALUE);
                }
            }
        }
        double min = Collections.min(mScores);
        return mScores.indexOf(min);
    }

    public double getSimilarity(FeaturedImage input){
        indexOf(input);
        return Collections.min(mScores);
    }

    public void drawMatches(FeaturedImage input, Mat dest, int index){
        indexOf(input);

        if(mMatchesList.get(index) != null && mMatchesList.get(index).size() > 0){
            MatOfDMatch matchMat = new MatOfDMatch();
            matchMat.fromList(mMatchesList.get(index));
            MatOfByte mask = new MatOfByte(Mat.ones(matchMat.size(), CvType.CV_8U));
            Features2d.drawMatches(
                    input, input.keyPoints, get(index), get(index).keyPoints, matchMat,
                    dest, RANDOM_COLOR, RANDOM_COLOR, mask,
                    Features2d.NOT_DRAW_SINGLE_POINTS);
            matchMat.release();
            mask.release();
        }else {
            input.copyTo(dest);
        }
    }

    private static double averageMatchDistance(Collection<DMatch> matches){
        double average = 0;
        for(DMatch m : matches)
            average += m.distance;
        return  average / matches.size();
    }
}
