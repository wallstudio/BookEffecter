package wallstudio.work.kamishiba;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.opencv.core.Point;

public class PerspectiveController extends FrameLayout {

    public static final Point DEFAULT_VANISHING_RATIO = new Point(0.5, 0.05);
    public static final double DEFAULT_PAGE_EDGE_Y = 0.5;

    public Point vanishingRatio = DEFAULT_VANISHING_RATIO;
    public double pageEdgeY = DEFAULT_PAGE_EDGE_Y;

    private ImageView mVanishingCursor;
    private View mVanishingCursorArea;
    private View mPageYCursor;
    private View mPageYCursorArea;
    private View mInputPreviewWrapper;

    // 汚いけど，レイアウトが完全に計算され切ったときのイベントが無いので
    private boolean mIsCursorPositionInitialized = false;

    // ref. http://ojed.hatenablog.com/entry/2015/12/05/161013
    public PerspectiveController(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        supportConstructor(context, attrs);
    }
    public PerspectiveController(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        supportConstructor(context, attrs);
    }
    private void supportConstructor(Context context, AttributeSet attrs){
        View layout = LayoutInflater.from(context).inflate(R.layout.perspective_controller, this);
        mVanishingCursor = layout.findViewById(R.id.vanishing_cursor);
        mVanishingCursorArea = layout.findViewById(R.id.vanishing_cursor_area);
        mPageYCursor = layout.findViewById(R.id.page_y_cursor);
        mPageYCursorArea = layout.findViewById(R.id.page_y_cursor_area);
        mInputPreviewWrapper = layout.findViewById(R.id.controller_view);

        mIsCursorPositionInitialized = false;
        mVanishingCursor.addOnLayoutChangeListener(mInitSetVanishingCursorListener);
        mVanishingCursor.setOnTouchListener(mVanishingCursorDragListener);
        mPageYCursor.addOnLayoutChangeListener(mInitSetPageYCursorListener);
        mPageYCursor.setOnTouchListener(mPageYCursorDragListener);
    }

    // 初期位置
    private View.OnLayoutChangeListener mInitSetVanishingCursorListener = new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            if(!mIsCursorPositionInitialized) {
                left = (int) (mInputPreviewWrapper.getWidth() * vanishingRatio.x - mVanishingCursorArea.getLeft()) - mVanishingCursor.getWidth() / 2;
                top = (int) (mInputPreviewWrapper.getHeight() * vanishingRatio.y - mVanishingCursorArea.getTop()) - mVanishingCursor.getHeight() / 2;
                mVanishingCursor.layout(left, top, left + mVanishingCursor.getWidth(), top + mVanishingCursor.getHeight());
            }
        }
    };

    private View.OnLayoutChangeListener mInitSetPageYCursorListener = new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            if(!mIsCursorPositionInitialized) {
                top = (int) (mInputPreviewWrapper.getHeight() * pageEdgeY - mPageYCursorArea.getTop()) - mPageYCursor.getHeight() / 2;
                mPageYCursor.layout(mPageYCursor.getLeft(), top, mPageYCursor.getRight(), top + mPageYCursor.getHeight());
            }
        }
    };

    // Drag
    private View.OnTouchListener mVanishingCursorDragListener = new View.OnTouchListener() {
        // ref. https://akira-watson.com/android/imageview-drag.html
        private int mPreX;
        private int mPreY;
        @Override
        public boolean onTouch(View view, MotionEvent event) {

            mIsCursorPositionInitialized = true;

            int globalX = (int) event.getRawX();
            int globalY = (int) event.getRawY();

            if(event.getAction() == MotionEvent.ACTION_MOVE){
                int deltaX = globalX - mPreX;
                int deltaY = globalY - mPreY;
                int left = mVanishingCursor.getLeft() + deltaX;
                int top = mVanishingCursor.getTop() + deltaY;
                int minLeft = 0;
                int minTop = 0;
                int maxRight = mVanishingCursorArea.getWidth() - mVanishingCursor.getWidth();
                int maxBottom = mVanishingCursorArea.getHeight() - mVanishingCursor.getHeight();
                left = left < minLeft ? minLeft : left;
                left = left > maxRight ? maxRight : left;
                top = top < minTop ? minTop : top;
                top = top > maxBottom ? maxBottom : top;
                int right = left + mVanishingCursor.getWidth();
                int bottom = top + mVanishingCursor.getHeight();

                mVanishingCursor.layout(left, top, right, bottom);

                int xOnImage = mVanishingCursorArea.getLeft() + mVanishingCursor.getLeft() + mVanishingCursor.getWidth() / 2;
                int yOnImage = mVanishingCursorArea.getTop() + mVanishingCursor.getTop() + mVanishingCursor.getHeight() / 2;

                vanishingRatio.x = xOnImage / (double)mInputPreviewWrapper.getWidth();
                vanishingRatio.y = yOnImage / (double)mInputPreviewWrapper.getHeight();

                if(vanishingRatio.y > pageEdgeY - 0.05){
                    pageEdgeY = vanishingRatio.y + 0.05;
                    top = (int) (mInputPreviewWrapper.getHeight() * pageEdgeY - mPageYCursorArea.getTop()) - mPageYCursor.getHeight() / 2;
                    mPageYCursor.layout(mPageYCursor.getLeft(), top, mPageYCursor.getRight(), top + mPageYCursor.getHeight());
                }
            }
            mPreX = globalX;
            mPreY = globalY;
            return true;
        }
    };

    private View.OnTouchListener mPageYCursorDragListener = new View.OnTouchListener() {
        private int mPreY;
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            mIsCursorPositionInitialized = true;

            int globalY = (int) event.getRawY();

            if(event.getAction() == MotionEvent.ACTION_MOVE){
                int deltaY = globalY - mPreY;
                int left = mPageYCursor.getLeft();
                int top = mPageYCursor.getTop() + deltaY;
                int minLeft = 0;
                int minTop = 0;
                int maxRight = mPageYCursorArea.getWidth() - mPageYCursor.getWidth();
                int maxBottom = mPageYCursorArea.getHeight() - mPageYCursor.getHeight();
                left = left < minLeft ? minLeft : left;
                left = left > maxRight ? maxRight : left;
                top = top < minTop ? minTop : top;
                top = top > maxBottom ? maxBottom : top;
                int right = left + mPageYCursor.getWidth();
                int bottom = top + mPageYCursor.getHeight();

                mPageYCursor.layout(left, top, right, bottom);

                int yOnImage = mPageYCursorArea.getTop() + mPageYCursor.getTop() + mPageYCursor.getHeight() / 2;

                pageEdgeY =  yOnImage / (double)mInputPreviewWrapper.getHeight();

                if(vanishingRatio.y > pageEdgeY - 0.05){
                    vanishingRatio.y = pageEdgeY - 0.05;
                    left = (int) (mInputPreviewWrapper.getWidth() * vanishingRatio.x - mVanishingCursorArea.getLeft()) - mVanishingCursor.getWidth() / 2;
                    top = (int) (mInputPreviewWrapper.getHeight() * vanishingRatio.y - mVanishingCursorArea.getTop()) - mVanishingCursor.getHeight() / 2;
                    mVanishingCursor.layout(left, top, left + mVanishingCursor.getWidth(), top + mVanishingCursor.getHeight());
                }
            }
            mPreY = globalY;
            return true;
        }
    };

}
