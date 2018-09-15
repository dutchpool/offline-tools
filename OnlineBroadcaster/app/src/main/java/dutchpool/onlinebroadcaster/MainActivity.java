package dutchpool.onlinebroadcaster;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.Locale;

import dutchpool.onlinebroadcaster.pojos.Nethash;
import dutchpool.onlinebroadcaster.pojos.TransactionResponse;
import dutchpool.onlinebroadcaster.pojos.Version;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final String ENDPOINT_POST_TRANSACTION = "peer/transactions";
    private static final String ENDPOINT_GET_VERSION = "api/peers/version";
    private static final String ENDPOINT_GET_NETHASH = "api/blocks/getNethash";

    private static final int LISK_TESTNET = 1;
    private static final int LISK_MAINNET = 2;
    private static final int LWF_TESTNET = 3;
    private static final int LWF_MAINNET= 4;
    private static final int ONZ_TESTNET = 5;
    private static final int ONZ_MAINNET = 6;
    private static final int OXY_TESTNET = 7;
    private static final int OXY_MAINNET = 8;
    private static final int SAUCO_MAINNET = 9;
    private static final int SHIFT_TESTNET = 10;
    private static final int SHIFT_MAINNET = 11;
    private static final int CUSTOM_NODE = 12;

    private SurfaceView cameraView;
    private TextView textView;
    private CameraSource cameraSource;
    private ToneGenerator toneG;
    private String transaction;
    private Spinner spinnerLiskNet;
    private EditText textCustomNode;
    private Button sendTransactionButton;
    private View progressBar;

    private Api api;

    final int RequestCameraPermissionID = 1001;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RequestCameraPermissionID: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    try {
                        cameraSource.start(cameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();


        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://testnet.lisk.io/peer/transactions/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(Api.class);

        // custom code
        cameraView = findViewById(R.id.camera_view);
        textView = findViewById(R.id.code_info);
        textCustomNode = findViewById(R.id.txtCustomNode);

        spinnerLiskNet = findViewById(R.id.spinnerNets);
        spinnerLiskNet.setOnItemSelectedListener(this.sliderItemSelectListener);

        sendTransactionButton = findViewById(R.id.buttonStart);
        sendTransactionButton.setOnClickListener(this.buttonSendClickListener);
        progressBar = findViewById(R.id.progress);

        setupBarcodeReader();
    }

    private void setupBarcodeReader() {

        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.QR_CODE).build();

        toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        if (!barcodeDetector.isOperational()) {
            Log.w("MainActivity", "Detector dependencies are not yet available");
        } else {
            cameraSource = new CameraSource
                    .Builder(getApplicationContext(), barcodeDetector)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(640, 480)  // TODO: set resolution to a better value
                    .setRequestedFps(2.0f)
                    .setAutoFocusEnabled(true)
                    .build();

            cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder surfaceHolder) {
                    try {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{android.Manifest.permission.CAMERA},
                                    RequestCameraPermissionID);
                            return;
                        }
                        cameraSource.start(cameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

                }

                @Override
                public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                    cameraSource.stop();
                }
            });

            barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
                @Override
                public void release() {
                }

                @Override
                public void receiveDetections(Detector.Detections<Barcode> detections) {
                    final SparseArray<Barcode> barcodes = detections.getDetectedItems();

                    if (barcodes.size() != 0) {
                        textView.post(new Runnable() {    // Use the post method of the TextView
                            public void run() {
                                if (barcodes.valueAt(0).displayValue.startsWith("{")) {
                                    textView.setText(    // Update the TextView
                                            barcodes.valueAt(0).displayValue
                                    );
                                } else if (!textView.getText().toString().endsWith("}")) {
                                    textView.setText(textView.getText() +     // Update the TextView
                                            barcodes.valueAt(0).displayValue
                                    );
                                }
                                transaction = "{\"transaction\":" + textView.getText() + "}";
                            }
                        });

                        toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private AdapterView.OnItemSelectedListener sliderItemSelectListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
            switch (spinnerLiskNet.getSelectedItemPosition()) {
                case 0:
                    sendTransactionButton.setEnabled(false);
                    textCustomNode.setVisibility(View.INVISIBLE);
                    break;
                case CUSTOM_NODE:
                    sendTransactionButton.setEnabled(true);
                    textCustomNode.setVisibility(View.VISIBLE);
                    break;
                default:
                    sendTransactionButton.setEnabled(true);
                    textCustomNode.setVisibility(View.INVISIBLE);
                    break;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parentView) {
            // your code here
        }
    };

    private View.OnClickListener buttonSendClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            textView.setText("Waiting for response from Server");
            progressBar.setVisibility(View.VISIBLE);
            sendTransactionButton.setVisibility(View.GONE);

            String trans = transaction;
            transaction = "";
            try {
                JsonObject transactionObject = (JsonObject) new JsonParser().parse(trans);
                String nodeUrl = getNodeEndpointl();
                if (nodeUrl == null) {
                    notifyError("ERROR: Please select a net");
                    return;
                }
                getVersion(nodeUrl, transactionObject);
            } catch (Exception e) {
                notifyError("ERROR: Transaction had no valid JSON format!");
            }
        }
    };

    private String getNodeEndpointl() {
        switch (spinnerLiskNet.getSelectedItemPosition()) { // TODO: add custom node
            case 0:         // NO net selected
                textView.setText("NO NET SELECTED");
                Toast toast = Toast.makeText(getApplicationContext(), "Please select net.", Toast.LENGTH_LONG);
                TextView vi = toast.getView().findViewById(android.R.id.message);
                vi.setTextColor(Color.RED);
                toast.show();
                return null;
            //break;
            case LISK_TESTNET:         // LISK TEST NET selected
                return "https://testnet.lisk.io/";
            case LISK_MAINNET:         // LISK MAIN NET selected
                return "https://node01.lisk.io/";
            case LWF_TESTNET:         // LWF TEST NET selected
                return "https://twallet.lwf.io/";
            case LWF_MAINNET:         // LWF MAIN NET selected
                return "https://wallet.lwf.io/";
            case ONZ_TESTNET:         // ONZ TEST NET selected
                return "https://tnode11.onzcoin.com/";
            case ONZ_MAINNET:         // ONZ MAIN NET selected
                return "https://node06.onzcoin.com/";
            case OXY_TESTNET:         // OXY TEST NET selected
                return "https://twallet.oxycoin.io/";
            case OXY_MAINNET:         // OXY MAIN NET selected
                return "https://wallet.oxycoin.io/";
            case SAUCO_MAINNET:         // SAUCO MAIN NET selected
                return "https://wallet.sauco.io/";
            case SHIFT_TESTNET:         // SHIFT TEST NET selected
                return "https://wallet.testnet.shiftnrg.org/";
            case SHIFT_MAINNET:         // SHIFT MAIN NET selected
                return "https://wallet.shiftnrg.org/";
            case CUSTOM_NODE:         // CUSTOM NODE selected
                return String.valueOf(textCustomNode.getText()) + "/";
            default:
                return "https://testnet.lisk.io/";
        }
    }

    private void getVersion(final String nodeUrl, final JsonObject transaction) {
        Log.w("MainActivity", nodeUrl + ENDPOINT_GET_VERSION);
        api.getVersion(nodeUrl + ENDPOINT_GET_VERSION).enqueue(new Callback<Version>() {
            @Override
            public void onResponse(Call<Version> call, Response<Version> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    getNethash(nodeUrl, transaction, response.body().version);
                } else if (response.body() != null && response.body().error != null) {
                    notifyError(String.format(Locale.ENGLISH,
                            "Could not get version.\nResponse %d %s", response.code(), response.body().error));
                } else {
                    notifyError(String.format(Locale.ENGLISH,
                            "Could not get version.\nResponse code: %d", response.code()));
                }
            }

            @Override
            public void onFailure(Call<Version> call, Throwable t) {
                notifyError(String.format("Could not get version.\nException: %s", t.getMessage()));
            }
        });
    }

    private void getNethash(final String nodeUrl, final JsonObject transaction, final String version) {
        api.getNethash(nodeUrl + ENDPOINT_GET_NETHASH).enqueue(new Callback<Nethash>() {
            @Override
            public void onResponse(Call<Nethash> call, Response<Nethash> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    doTransaction(nodeUrl, transaction, version, response.body().nethash);
                } else if (response.body() != null && response.body().error != null) {
                    notifyError(String.format(Locale.ENGLISH,
                            "Could not get nethash.\nResponse %d %s", response.code(), response.body().error));
                } else {
                    notifyError(String.format(Locale.ENGLISH,
                            "Could not get nethash.\nResponse code: %d", response.code()));
                }
            }

            @Override
            public void onFailure(Call<Nethash> call, Throwable t) {
                notifyError(String.format("Could not get nethash.\nException: %s", t.getMessage()));
            }
        });
    }

    private void doTransaction(String nodeUrl, JsonObject transaction, String version, String nethash) {
        api.postTransaction(nodeUrl + ENDPOINT_POST_TRANSACTION,
                transaction, version, nethash).enqueue(new Callback<TransactionResponse>() {
            @Override
            public void onResponse(Call<TransactionResponse> call, Response<TransactionResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    notifySuccess();
                } else if (response.body() != null && response.body().error != null) {
                    notifyError(String.format(Locale.ENGLISH,
                            "Could not do transaction.\nResponse %d %s", response.code(), response.body().error));
                } else {
                    notifyError(String.format(Locale.ENGLISH,
                            "Could not do transaction.\nResponse code: %d", response.code()));
                }
            }

            @Override
            public void onFailure(Call<TransactionResponse> call, Throwable t) {
                notifyError(String.format("Could not do transaction.\nException: %s", t.getMessage()));
            }
        });
    }

    private void notifyError(String message) {
        textView.setText(message);
        Toast toast = Toast.makeText(getApplicationContext(), "FAILED! See textfield for details.", Toast.LENGTH_LONG);
        TextView v = toast.getView().findViewById(android.R.id.message);
        v.setTextColor(Color.RED);
        toast.show();

        progressBar.setVisibility(View.GONE);
        sendTransactionButton.setVisibility(View.VISIBLE);
    }

    private void notifySuccess() {
        textView.setText("");
        Toast toast = Toast.makeText(getApplicationContext(), "SUCCESS!", Toast.LENGTH_LONG);
        TextView v = toast.getView().findViewById(android.R.id.message);
        v.setTextColor(Color.GREEN);
        toast.show();

        progressBar.setVisibility(View.GONE);
        sendTransactionButton.setVisibility(View.VISIBLE);
    }

//    private class SendPostRequest extends AsyncTask<String, Void, String> {
//
//        protected void onPreExecute() {
//        }
//
//        protected String doInBackground(String... arg0) {
//            try {
//
//                //URL urlNetLink = new URL("https://testnet.lisk.io/peer/transactions");
//                //String trans = "{\"transaction\":{\"type\":0,\"amount\":10000000,\"fee\":10000000,\"recipientId\":\"1541786588265098370L\",\"timestamp\":44505392,\"asset\":{},\"senderPublicKey\":\"23be4e11ddcb6bf6d18ad2e4de1141b0ea2b08625767a20ade249b4117276b5f\",\"signature\":\"5c0a40ecb723fecbc2eceb64ac6489a63bbbcb6da59e3dd36de224ee240108f54601a9d725e76a906b5e54a33cd3910d8abb6c251a22ade3fea68d8e757a3e0e\",\"signSignature\":\"b9081c128ddc3453efd9820797f91ba67fc29367bb22cde90b7795e215ff0bcbd0b77e36e88217996e35640accdcd18cddf1137ae9bdc2d0c0353d0bf490870f\",\"id\":\"5630001309045380353\"}}";
//                String trans = transaction;
//                transaction = "";
//                JSONObject postDataParams;
//                try {
//                    postDataParams = new JSONObject(trans);
//                } catch (Exception e) {
//                    return new String("ERROR: Transaction had no valid JSON format!");
//                }
//                Log.e("params", postDataParams.toString());
//
//                HttpURLConnection conn = (HttpURLConnection) urlNetLink.openConnection();
//                conn.setReadTimeout(15000 /* milliseconds */);
//                conn.setConnectTimeout(15000 /* milliseconds */);
//                conn.setRequestMethod("POST");
//
//                conn.setRequestProperty("Content-Type", "application/json");
//                conn.setRequestProperty("os", "linux4.4.0-78-generic");
//                conn.setRequestProperty("version", strVersion);
//                conn.setRequestProperty("port", "1");
//                conn.setRequestProperty("nethash", netHash);
//
//                conn.setDoInput(true);
//                conn.setDoOutput(true);
//
//                OutputStream os = conn.getOutputStream();
//                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
//                writer.write(postDataParams.toString());
//
//                writer.flush();
//                writer.close();
//                os.close();
//
//                int responseCode = conn.getResponseCode();
//
//                if (responseCode == HttpsURLConnection.HTTP_OK) {
//
//                    BufferedReader in = new BufferedReader(
//                            new InputStreamReader(
//                                    conn.getInputStream()));
//                    StringBuffer sb = new StringBuffer("");
//                    String line = "";
//
//                    while ((line = in.readLine()) != null) {
//
//                        sb.append(line);
//                        break;
//                    }
//                    in.close();
//                    return sb.toString();
//                } else {
//                    return new String("false : " + responseCode);
//                }
//            } catch (Exception e) {
//                return new String("Exception: " + e.getMessage());
//            }
//        }
//
//        @Override
//        protected void onPostExecute(String result) {
//            String strResult = "";
//            try {
//                JSONObject postResult = new JSONObject(result);
//                strResult = postResult.getString("success");
//
//            } catch (JSONException e) {
//                textView.setText(result);
//                Toast toast = Toast.makeText(getApplicationContext(), "FAILED! See textfield for details.", Toast.LENGTH_LONG);
//                TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
//                v.setTextColor(Color.RED);
//                toast.show();
//            }
//
//            if (strResult == "true") {
//                textView.setText("");
//                Toast toast = Toast.makeText(getApplicationContext(), "SUCCESS!", Toast.LENGTH_LONG);
//                TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
//                v.setTextColor(Color.GREEN);
//                toast.show();
//            } else {
//                textView.setText(result);
//                Toast toast = Toast.makeText(getApplicationContext(), "FAILED! See textfield for details.", Toast.LENGTH_LONG);
//                TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
//                v.setTextColor(Color.RED);
//                toast.show();
//            }
//        }
//    }
}
