package wallstudio.work.kamishiba;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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

    public void setActive(boolean isActive){
        if(mBarcodeView != null)
            if(isActive)
                mBarcodeView.resume();
            else
                mBarcodeView.pause();
    }



    @Override
    public void onResume() {
        super.onResume();
        mBarcodeView.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mBarcodeView.pause();
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
