package wallstudio.work.kamishiba;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.List;


public class QRScanerFragment extends TabFragment {

    public static final String TITLE = "ライブラリ ＞ QR読み取り";

    private DecoratedBarcodeView mBarcodeView;
    private boolean mIsUserVisible = false;
    private boolean decoded = false;

    // ref. https://qiita.com/sakuna63/items/653452eb48029d53d44f
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        mIsUserVisible = isVisibleToUser;

        if(isVisibleToUser && getActivity() != null)
            getActivity().setTitle(TITLE);

        if(mBarcodeView != null) {
            if (isVisibleToUser) {
                mBarcodeView.resume();
                Log.d("QR_CAMERA", "onResume ");
            } else {
                mBarcodeView.pause();
                Log.d("QR_CAMERA", "onPause ");
            }
        }
    }

    @Override
    public void onResume() {
        decoded = false;
        super.onResume();
        if(mIsUserVisible) {
            mBarcodeView.resume();
            Log.d("QR_CAMERA", "onResume ");
        }else {
            mBarcodeView.pause();
            Log.d("QR_CAMERA" , "onPause " );
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mBarcodeView.pause();
        Log.d("QR_CAMERA" , "onPause " );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_qrscaner, container, false);
        mBarcodeView = v.findViewById(R.id.decoratedBarcodeView);
        mBarcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult barcodeResult) {
                if(!decoded) {
                    decoded = true;
                    String input = barcodeResult.getText().trim();
                    startLauncher(input);
                }
            }
            @Override
            public void possibleResultPoints(List<ResultPoint> list) {}
        });
        return v;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

}
