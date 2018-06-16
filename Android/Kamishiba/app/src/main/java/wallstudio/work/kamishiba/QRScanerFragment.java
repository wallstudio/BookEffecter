package wallstudio.work.kamishiba;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CompoundBarcodeView;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.List;


public class QRScanerFragment extends Fragment {

    private DecoratedBarcodeView mBarcodeView;

    // ref. https://qiita.com/sakuna63/items/653452eb48029d53d44f
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

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
        super.onResume();
        mBarcodeView.resume();
        Log.d("QR_CAMERA" , "onResume " );
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
        mBarcodeView.decodeSingle(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult barcodeResult) {

                TextView textView = getView().findViewById(R.id.rq_result);
                textView.setText(barcodeResult.getText());
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> list) {}
        });
        return v;
    }

}
