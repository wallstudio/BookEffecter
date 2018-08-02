package wallstudio.work.kamishiba;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.BFMatcher;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import io.github.jdiemke.triangulation.DelaunayTriangulator;
import io.github.jdiemke.triangulation.NotEnoughPointsException;
import io.github.jdiemke.triangulation.Triangle2D;
import io.github.jdiemke.triangulation.Vector2D;

public class TrainingDataList extends ArrayList<FeaturedImage> {

    public static final int USE_MATCH_COUNT = 10;
    public final Scalar RANDOM_COLOR = Scalar.all(-1);
    public final Scalar MESH_COLOR = new Scalar(32, 255, 32);
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

            // 局所特徴で最尤画像を検索
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

            // メッシュ法でダブルチェック
            List<KeyPoint> referenceKeyPoints = input.keyPoints.toList();

            input.keyPoints2 = new ArrayList<>();
            for (DMatch dMatch : mMatchesList.get(mScores.indexOf(Collections.min(mScores)))) {
                KeyPoint keyPoint = referenceKeyPoints.get(dMatch.queryIdx);
                input.keyPoints2.add(keyPoint);
            }
//            {
//                List<Triangulator.Vector2> referencePoints = new ArrayList<>();
//                for (DMatch dMatch : mMatchesList.get(mScores.indexOf(Collections.min(mScores)))) {
//                    KeyPoint keyPoint = referenceKeyPoints.get(dMatch.queryIdx);
//                    referencePoints.add(new Triangulator.Vector2(keyPoint.pt.x, keyPoint.pt.y));
//                }
//                if (referencePoints.size() > 0) {
//                    input.mesh = Triangulator.createMesh(referencePoints);
//                }
//            }

            Vector<Vector2D> referencePoints = new Vector<>();
            for (KeyPoint keyPoint: input.keyPoints2) {
                referencePoints.add(new Vector2D(keyPoint.pt.x, keyPoint.pt.y));
            }
            if (referencePoints.size() > 0) {
                try {
                    DelaunayTriangulator delaunayTriangulator = new DelaunayTriangulator(referencePoints);
                    delaunayTriangulator.triangulate();
                    List<Triangle2D> triangleSoup = delaunayTriangulator.getTriangles();
                    input.mesh2 = triangleSoup;
                } catch (NotEnoughPointsException e) {
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

        // メッシュの描画
        if(input.keyPoints2 != null && input.mesh2 != null) {

            for(KeyPoint keyPoint: input.keyPoints2){
                Imgproc.circle(input, keyPoint.pt, 5, MESH_COLOR, 1);
            }

//            KeyPoint[] keyPoints = input.keyPoints.toArray();
//            for (Triangulator.Edge edge : input.mesh) {
//                Imgproc.line(input, keyPoints[edge.p1].pt, keyPoints[edge.p2].pt, MESH_COLOR, 1);
//            }

            for(Triangle2D triangle: input.mesh2){
                Imgproc.line(input, new Point(triangle.a.x, triangle.a.y),  new Point(triangle.b.x, triangle.b.y), MESH_COLOR, 2);
                Imgproc.line(input, new Point(triangle.b.x, triangle.b.y),  new Point(triangle.c.x, triangle.c.y), MESH_COLOR, 2);
                Imgproc.line(input, new Point(triangle.b.x, triangle.b.y),  new Point(triangle.a.x, triangle.a.y), MESH_COLOR, 2);
            }

        }

        // マッチの描画
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
