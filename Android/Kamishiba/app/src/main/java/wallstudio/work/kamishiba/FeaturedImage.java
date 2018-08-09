package wallstudio.work.kamishiba;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.features2d.AKAZE;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import io.github.jdiemke.triangulation.DelaunayTriangulator;
import io.github.jdiemke.triangulation.NotEnoughPointsException;
import io.github.jdiemke.triangulation.Triangle2D;
import io.github.jdiemke.triangulation.Vector2D;

public class FeaturedImage extends Mat{

    private static AKAZE sAkaze;
    {
        if(sAkaze == null) sAkaze = AKAZE.create();
    }

    public Set<Edge> edges = new HashSet<>();
    public MatOfKeyPoint keyPoints = new MatOfKeyPoint();
    public Mat descriptors = new Mat();

    public FeaturedImage(Mat image){
        super();
        Imgproc.cvtColor(image, this, Imgproc.COLOR_BGR2GRAY);
        Mat mask = Mat.ones(size(), CvType.CV_8U);
        sAkaze.detectAndCompute(this, mask, keyPoints, descriptors);
    }

    public FeaturedImage(Bitmap image){
        super();
        Utils.bitmapToMat(image, this);
        Imgproc.cvtColor(this, this, Imgproc.COLOR_BGR2GRAY);
        Mat mask = Mat.ones(size(), CvType.CV_8U);
        sAkaze.detectAndCompute(this, mask, keyPoints, descriptors);
    }

    public FeaturedImage(String path) throws IOException {
        super();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap image = LoadUtil.getBitmapFromPath(path);
        Utils.bitmapToMat(image, this);
        Imgproc.cvtColor(this, this, Imgproc.COLOR_BGR2GRAY);
        Mat mask = Mat.ones(size(), CvType.CV_8U);
        sAkaze.detectAndCompute(this, mask, keyPoints, descriptors);
        image.recycle();
    }

    public void detectEdges(List<DMatch> matches){

        List<Triangle2D> mesh = null;
        List<KeyPoint> valuableKeyPoints = new ArrayList<>();
        List<KeyPoint> referenceKeyPoints = keyPoints.toList();

        // 有効KeyPointを抽出
        for (DMatch dMatch : matches) {
            KeyPoint keyPoint = referenceKeyPoints.get(dMatch.queryIdx);
            valuableKeyPoints.add(keyPoint);
        }
        // KeyPoint -> Vector変換
        Vector<Vector2D> referenceVectors = new Vector<>();
        for (KeyPoint keyPoint: valuableKeyPoints) {
            referenceVectors.add(new Vector2D(keyPoint.pt.x, keyPoint.pt.y));
        }
        // メッシュ生成
        if (referenceVectors.size() > 0) {
            try {
                DelaunayTriangulator delaunayTriangulator = new DelaunayTriangulator(referenceVectors);
                delaunayTriangulator.triangulate();
                List<Triangle2D> triangleSoup = delaunayTriangulator.getTriangles();
                mesh = triangleSoup;
            } catch (NotEnoughPointsException e) {
            }
        }
        if(mesh != null){
            // KeyPointとの対応付け
            List<TriangleBind> triangleBinds = new ArrayList<>();
                for(Triangle2D triangle: mesh){
                    triangleBinds.add(new TriangleBind(triangle, keyPoints.toList()));
                }
            // Edgeに変換
            for(TriangleBind triangle: triangleBinds){
                edges.addAll(Edge.fromTriangle(triangle, keyPoints.toList()));
            }
        }
    }

    public void reEdge(Set<Edge> template, List<DMatch> matches){
        edges.clear();
        for(Edge edge : template){
            int start = -1;
            int goal = -1;
            for(DMatch match: matches){
                if(start < 0 && match.queryIdx == edge.p0Idx) start = match.trainIdx;
                if(goal < 0 && match.queryIdx == edge.p1Idx) goal = match.trainIdx;
            }
            if(start >= 0 && goal >= 0)
                edges.add(new Edge(start, goal, keyPoints.toList()));
            else
                Log.d("Edge", "Not found keypoint.");
        }
    }

    public double checkWarp(){
        double result = 0;
        for(Edge e0: edges){
            for(Edge e1: edges) {
                if(Edge.crossCheck(e0, e1, 0.1)) result++;
            }
        }
        return  result;
    }

    public void release(){
        keyPoints.release();
        descriptors.release();
        super.release();
    }

    public static class TriangleBind{
        public static final float NEAR_THRESHOLD = 0.1f;
        public int a;
        public int b;
        public int c;
        public TriangleBind(Triangle2D src, List<KeyPoint> keyPoints){
            a = pointBind(src.a, keyPoints);
            b = pointBind(src.b, keyPoints);
            c = pointBind(src.c, keyPoints);
        }
        private int pointBind(Vector2D point, List<KeyPoint> keyPoints){
            for (int i = 0; i < keyPoints.size(); i++){
                Point keyPoint = keyPoints.get(i).pt;
                if(Math.abs(point.x - keyPoint.x) < NEAR_THRESHOLD
                        && Math.abs(point.y - keyPoint.y) < NEAR_THRESHOLD)
                    return i;
            }
            return -1;
        }
    }

    public static class Edge{
        int p0Idx;
        int p1Idx;
        Point p0;
        Point p1;

        public Edge(int p0, int p1, List<KeyPoint> keyPoints){
            this.p0Idx = p0;
            this.p1Idx = p1;
            this.p0 = keyPoints.get(p0Idx).pt;
            this.p1 = keyPoints.get(p1Idx).pt;
        }

        @Override
        public int hashCode(){
            String hashString = Math.max(p0Idx, p1Idx) + "_" + Math.min(p0Idx, p1Idx);
            return hashString.hashCode();
        }

        @Override
        public boolean equals(Object o){
            Edge e = (Edge)o;
            return (e.p0Idx == p0Idx && e.p1Idx == p1Idx)
                    || (e.p0Idx == p1Idx && e.p1Idx == p0Idx);
        }

        public static Set<Edge> fromTriangle(TriangleBind triangle, List<KeyPoint> keyPoints){
            Set<Edge> edges = new HashSet<>();
            edges.add(new Edge(triangle.a, triangle.b, keyPoints));
            edges.add(new Edge(triangle.b, triangle.c, keyPoints));
            edges.add(new Edge(triangle.c, triangle.a, keyPoints));
            return  edges;
        }

        public static boolean crossCheck(Edge e0, Edge e1, double playRatio){

            if(e0.equals(e1)) return false;

            double ax = e0.p0.x;
            double ay = e0.p0.y;
            double bx = e0.p1.x;
            double by = e0.p1.y;
            double cx = e1.p0.x;
            double cy = e1.p0.y;
            double dx = e1.p1.x;
            double dy = e1.p1.y;

            double ta = (cx - dx) * (ay - cy) + (cy - dy) * (cx - ax);
            double tb = (cx - dx) * (by - cy) + (cy - dy) * (cx - bx);
            double tc = (ax - bx) * (cy - ay) + (ay - by) * (ax - cx);
            double td = (ax - bx) * (dy - ay) + (ay - by) * (ax - dx);

            return tc * td < 0 && ta * tb < 0;
        }
    }
}