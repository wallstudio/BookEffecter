package wallstudio.work.kamishiba;

import android.graphics.PointF;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Triangulator {

    public static class Triangle {
        public int p1;
        public int p2;
        public int p3;
        public Triangle(int point1, int point2, int point3) {
            p1 = point1;
            p2 = point2;
            p3 = point3;
        }
    }

    public static class  Edge {
        public int p1;
        public int p2;
        public Edge(int point1, int point2) {
            p1 = point1;
            p2 = point2;
        }
        public Edge() {
            this(0, 0);
        }
        public int hashCode(){
            return Integer.valueOf(this.p1 + this.p2).hashCode();
        }
        public boolean Equals(Edge other) {
            return ((this.p1 == other.p2) && (this.p2 == other.p1)) || ((this.p1 == other.p1) && (this.p2 == other.p2));
        }
    }

    public static class  Vector2 extends PointF{
        public Vector2(double x, double y){
            super((float)x, (float)y);
        }
    }

    public static Set<Edge> createMesh(Vector2[] pointCloud2D){
        return extractEdge(triangulatePolygon(pointCloud2D));
    }
    public static Set<Edge> createMesh(List<Vector2> pointCloud2D){
        return extractEdge(triangulatePolygon(pointCloud2D.toArray(new Vector2[0])));
    }

    private static Set<Edge> extractEdge(List<Triangle> triangles){
        Set<Edge> edges = new HashSet<>(triangles.size() * 3);
        for (Triangle triangle : triangles){
            edges.add(new Edge(triangle.p1, triangle.p2));
            edges.add(new Edge(triangle.p2, triangle.p3));
            edges.add(new Edge(triangle.p3, triangle.p1));
        }
        return edges;
    }

    private static List<Triangle> triangulatePolygon(Vector2[] XZofVertices) {
        int VertexCount = XZofVertices.length;
        float xmin = XZofVertices[0].x;
        float ymin = XZofVertices[0].y;
        float xmax = xmin;
        float ymax = ymin;
        for (int ii1 = 1; ii1 < VertexCount; ii1++) {
            if (XZofVertices[ii1].x < xmin) {
                xmin = XZofVertices[ii1].x;
            } else if (XZofVertices[ii1].x > xmax) {
                xmax = XZofVertices[ii1].x;
            }
            if (XZofVertices[ii1].y < ymin) {
                ymin = XZofVertices[ii1].y;
            } else if (XZofVertices[ii1].y > ymax) {
                ymax = XZofVertices[ii1].y;
            }
        }
        float dx = xmax - xmin;
        float dy = ymax - ymin;
        float dmax = (dx > dy) ? dx : dy;
        float xmid = (xmax + xmin) * 0.5f;
        float ymid = (ymax + ymin) * 0.5f;

        Vector2[] ExpandedXZ = new Vector2[3 + VertexCount];
        for (int ii1 = 0; ii1 < VertexCount; ii1++) {
            ExpandedXZ[ii1] = XZofVertices[ii1];
        }
        ExpandedXZ[VertexCount] = new Vector2((xmid - 2 * dmax), (ymid - dmax));
        ExpandedXZ[VertexCount + 1] = new Vector2(xmid, (ymid + 2 * dmax));
        ExpandedXZ[VertexCount + 2] = new Vector2((xmid + 2 * dmax), (ymid - dmax));
        List<Triangle> TriangleList = new ArrayList<>();
        TriangleList.add(new Triangle(VertexCount, VertexCount + 1, VertexCount + 2));
        for (int ii1 = 0; ii1 < VertexCount; ii1++) {
            List<Edge> Edges = new ArrayList<>();
            for (int ii2 = 0; ii2 < TriangleList.size(); ii2++) {
                if (triangulatePolygonSubFuncInCircle(ExpandedXZ[ii1], ExpandedXZ[TriangleList.get(ii2).p1], ExpandedXZ[TriangleList.get(ii2).p2], ExpandedXZ[TriangleList.get(ii2).p3])) {
                    Edges.add(new Edge(TriangleList.get(ii2).p1, TriangleList.get(ii2).p2));
                    Edges.add(new Edge(TriangleList.get(ii2).p2, TriangleList.get(ii2).p3));
                    Edges.add(new Edge(TriangleList.get(ii2).p3, TriangleList.get(ii2).p1));
                    TriangleList.remove(ii2);
                    ii2--;
                }
            }
            if (ii1 >= VertexCount) {
                continue;
            }
            for (int ii2 = Edges.size() - 2; ii2 >= 0; ii2--) {
                for (int ii3 = Edges.size() - 1; ii3 >= ii2 + 1; ii3--) {
                    if (Edges.get(ii2).Equals(Edges.get(ii3))) {
                        Edges.remove(ii3);
                        Edges.remove(ii2);
                        ii3--;
                        continue;
                    }
                }
            }
            for (int ii2 = 0; ii2 < Edges.size(); ii2++) {
                TriangleList.add(new Triangle(Edges.get(ii2).p1, Edges.get(ii2).p2, ii1));
            }
            Edges.clear();
            Edges = null;
        }
        for (int ii1 = TriangleList.size() - 1; ii1 >= 0; ii1--) {
            if (TriangleList.get(ii1).p1 >= VertexCount || TriangleList.get(ii1).p2 >= VertexCount || TriangleList.get(ii1).p3 >= VertexCount) {
                TriangleList.remove(ii1);
            }
        }
        //TriangleList.TrimExcess();
        int[] Triangles = new int[3 * TriangleList.size()];
        return TriangleList;
    }

    private static float epsilon = Float.MIN_VALUE;
    private static boolean triangulatePolygonSubFuncInCircle(Vector2 p, Vector2 p1, Vector2 p2, Vector2 p3) {
        if (Math.abs(p1.y - p2.y) < epsilon && Math.abs(p2.y - p3.y) < epsilon) {
            return false;
        }
        float m1, m2, mx1, mx2, my1, my2, xc, yc;
        if (Math.abs(p2.y - p1.y) < epsilon) {
            m2 = -(p3.x - p2.x) / (p3.y - p2.y);
            mx2 = (p2.x + p3.x) * 0.5f;
            my2 = (p2.y + p3.y) * 0.5f;
            xc = (p2.x + p1.x) * 0.5f;
            yc = m2 * (xc - mx2) + my2;
        } else if (Math.abs(p3.y - p2.y) < epsilon) {
            m1 = -(p2.x - p1.x) / (p2.y - p1.y);
            mx1 = (p1.x + p2.x) * 0.5f;
            my1 = (p1.y + p2.y) * 0.5f;
            xc = (p3.x + p2.x) * 0.5f;
            yc = m1 * (xc - mx1) + my1;
        } else {
            m1 = -(p2.x - p1.x) / (p2.y - p1.y);
            m2 = -(p3.x - p2.x) / (p3.y - p2.y);
            mx1 = (p1.x + p2.x) * 0.5f;
            mx2 = (p2.x + p3.x) * 0.5f;
            my1 = (p1.y + p2.y) * 0.5f;
            my2 = (p2.y + p3.y) * 0.5f;
            xc = (m1 * mx1 - m2 * mx2 + my2 - my1) / (m1 - m2);
            yc = m1 * (xc - mx1) + my1;
        }
        float dx = p2.x - xc;
        float dy = p2.y - yc;
        float rsqr = dx * dx + dy * dy;
        dx = p.x - xc;
        dy = p.y - yc;
        double drsqr = dx * dx + dy * dy;
        return (drsqr <= rsqr);
    }

}
