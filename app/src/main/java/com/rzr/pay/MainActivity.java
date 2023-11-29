package com.rzr.pay;

import static my.com.softspace.ssmpossdk.transaction.MPOSTransaction.TransactionEvents.TransactionResult.TransactionFailed;
import static my.com.softspace.ssmpossdk.transaction.MPOSTransaction.TransactionEvents.TransactionResult.TransactionSuccessful;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.visa.CheckmarkMode;
import com.visa.CheckmarkTextOption;
import com.visa.SensoryBrandingView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


import com.rzr.pay.R;
import my.com.softspace.ssmpossdk.Environment;
import my.com.softspace.ssmpossdk.SSMPOSSDK;
import my.com.softspace.ssmpossdk.SSMPOSSDKConfiguration;
import my.com.softspace.ssmpossdk.transaction.MPOSTransaction;
import my.com.softspace.ssmpossdk.transaction.MPOSTransactionOutcome;
import my.com.softspace.ssmpossdk.transaction.MPOSTransactionParams;

public class MainActivity extends AppCompatActivity
{
    private final static String TAG = "FasstapSDKTester";

    private final static String CARD_TYPE_VISA = "0";
    private final static String CARD_TYPE_MASTERCARD = "1";
    private final static String CARD_TYPE_AMEX = "2";
    private final static String CARD_TYPE_JCB = "3";
    private final static String CARD_TYPE_DISCOVER = "23";

    private final static String TRX_STATUS_APPROVED = "100";
    private final static String TRX_STATUS_REVERSED = "101";
    private final static String TRX_STATUS_VOIDED = "102";
    private final static String TRX_STATUS_PENDING_SIGNATURE = "103";
    private final static String TRX_STATUS_SETTLED = "104";
    private final static String TRX_STATUS_PENDING_TC = "105";
    private final static String TRX_STATUS_REFUNDED = "106";

    private EditText edtAmtAuth;
    private EditText edtUserID;
    private EditText edtDeveloperID;
    private EditText edtRefNo;
    private EditText edtTrxID;
    private TextView tvLogArea;
    private Button btnStartTrx;
    private Button btnClearLog;
    private Button btnVoidTrx;
    private Button btnSettlement;

    private Button after_transferstatus;
    private Button btnRefreshToken;
    private Button btnGetTransactionStatus;
    private Button btnRefundTrx;
    private Button btnUploadSignature;
    private volatile boolean isTrxRunning = false;
    private LinearLayout layoutSensoryBranding;
    private SensoryBrandingView visaSensoryBrandingView;
    private VideoView sensoryBrandingVideoView;

    private MPOSTransactionOutcome _transactionOutcome;
    private ProgressDialog progressDialog;

    String MerchantHost,OpType,Currency,Amount,OrderId,Channel,Tid,data_value;

    ImageView imageView;
    TextView text_status;



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();


    }

    @Override
    protected void onResume() {
        super.onResume();
        edtAmtAuth.clearFocus();
    }

    private void writeLog(String msg) {
        this.runOnUiThread(() -> {
            Date now = new Date();
            tvLogArea.append(new SimpleDateFormat("MM-dd HH:mm:ss.SS").format(now) + "\n" + msg + "\n\n");
        });

    }

    private void clearLogs() {
        this.runOnUiThread(() -> {
            tvLogArea.setText("");
            tvLogArea.scrollTo(0,0);
        });
    }

    private void initUI()
    {
        MerchantHost = getIntent().getStringExtra("MerchantHost");
        OpType = getIntent().getStringExtra("OpType");
        Currency = getIntent().getStringExtra("Currency");
        Amount = getIntent().getStringExtra("Amount");
        OrderId = getIntent().getStringExtra("OrderId");
        Channel = getIntent().getStringExtra("Channel");
        Tid = getIntent().getStringExtra("Tid");
        data_value = getIntent().getStringExtra("Log");
        edtAmtAuth = findViewById(R.id.edtAmtAuth);
        edtUserID = findViewById(R.id.edtUserID);
        edtDeveloperID = findViewById(R.id.edtDeveloperID);
        edtRefNo = findViewById(R.id.edtRefNo);
        edtTrxID = findViewById(R.id.edtTrxID);
        tvLogArea = findViewById(R.id.tvLogArea);
        btnStartTrx = findViewById(R.id.btnStartTrx);
        btnClearLog = findViewById(R.id.btnClearLog);
        btnVoidTrx = findViewById(R.id.btnVoidTrx);
        btnSettlement = findViewById(R.id.btnSettlement);
        after_transferstatus = findViewById(R.id.after_transferstatus);
        btnRefreshToken = findViewById(R.id.btnRefreshToken);
        btnGetTransactionStatus = findViewById(R.id.btnGetTransactionStatus);
        btnRefundTrx = findViewById(R.id.btnRefundTrx);
        btnUploadSignature = findViewById(R.id.btnUploadSignature);
        layoutSensoryBranding = findViewById(R.id.activity_layoutSensoryBranding);
        visaSensoryBrandingView = findViewById(R.id.activity_visaSensoryBrandingView);
        sensoryBrandingVideoView = findViewById(R.id.activity_sensoryBrandingVideoView);

        imageView = findViewById(R.id.imageView);
        text_status = findViewById(R.id.text_status);

        showProgressDialog();

        after_transferstatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String packageName = "com.cmr.cowinemployees";
                String className = "com.cmr.cowinemployees.ResultScreen";
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(packageName, className));

                String log_data = tvLogArea.getText().toString();

                intent.putExtra("log_data", log_data);
                PackageManager packageManager = getPackageManager();
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent);
                } else {
                    // The activity doesn't exist or the app is not installed
                }
            }
        });
        try {
            if (Amount.equalsIgnoreCase("0")) {
                Toast.makeText(this, "Enter amount is Zero", Toast.LENGTH_LONG).show();
                dismissProgressDialog();
            }

            if (Tid.isEmpty()) {
                Toast.makeText(this, "Please enter credentials", Toast.LENGTH_LONG).show();
                dismissProgressDialog();
            }
        } catch (Exception e) {
            // Handle other types of exceptions that might occur
            e.printStackTrace(); // Print the stack trace for debugging
            // You might want to display an error message to the user or take other appropriate actions.
            Toast.makeText(this, "Directly cannot Accessible", Toast.LENGTH_LONG).show();
            dismissProgressDialog();
        }

        if (Amount != null && Tid != null) {
            try {
                // Do something with the string
                // For example, display it in a TextView
                String[] tidArray = Tid.split(",");
                edtAmtAuth.setText(Amount);
                edtUserID.setText(tidArray[0]);
                edtDeveloperID.setText(tidArray[1]);
                initFasstapMPOSSDK();
            } catch (ArrayIndexOutOfBoundsException e) {
                // Handle the exception caused by accessing an index that doesn't exist in the array
                e.printStackTrace(); // Print the stack trace for debugging
                // You might want to display an error message to the user or take other appropriate actions.
            } catch (Exception e) {
                // Handle other types of exceptions that might occur
                e.printStackTrace(); // Print the stack trace for debugging
                // You might want to display an error message to the user or take other appropriate actions.
            }
        }

        tvLogArea.setMovementMethod(new ScrollingMovementMethod());

        btnStartTrx.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                btnStartTrxOnClick();
            }
        });

        btnClearLog.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
//                clearLogs();
            }
        });

        btnVoidTrx.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                btnVoidTrxOnClick();
            }
        });

        btnSettlement.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                btnSettlementOnClick();
            }
        });

        btnRefreshToken.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnRefreshTokenOnClick();
            }
        });

        btnGetTransactionStatus.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnGetTransactionStatussOnClick();
            }
        });

        btnRefundTrx.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnRefundTrxOnClick();
            }
        });

        btnUploadSignature.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadSignature();
            }
        });
    }

    private void showProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait while we are fetching data.....!");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    public void btnStartTrxOnClick() {
        //trx running, means user have the intention to cancel trx
        if (isTrxRunning) {
            cancelTrx();
        }
        else {
            edtRefNo.setText("SS" + Calendar.getInstance().getTimeInMillis());
            startTrx();
            btnUploadSignature.setEnabled(false);
        }
    }

    private void cancelTrx() {
        toggleTransactionRunning(false);
        SSMPOSSDK.getInstance().getTransaction().abortTransaction();
        // clearLogs();
        writeLog("transaction successfully cancelled");
    }

    public void btnVoidTrxOnClick() {
        voidTransaction();
    }

    public void btnRefundTrxOnClick() {
        refundTransaction();
    }

    public void btnSettlementOnClick() {
        performSettlement();
    }

    public void btnRefreshTokenOnClick() {
        refreshToken();
    }

    public void btnGetTransactionStatussOnClick() {
        getTransactionStatus();
    }

    private void startTrx() {

        if (!isNfcEnabled(this))
        {
            return;
        }

        toggleTransactionRunning(true);

        if (SSMPOSSDK.requestPermissionIfRequired(MainActivity.this, 10009))
        {
            new Thread() {
                @Override
                public void run() {
                    startEMVProcessing();
                }
            }.start();
        }
        else
        {
            toggleTransactionRunning(false);
        }
    }

    private void toggleTransactionRunning(boolean isRunning) {
        if (isRunning) {
            isTrxRunning = true;
            btnStartTrx.setText("Cancel\nTransaction");
            if (btnRefundTrx.isEnabled()) btnRefundTrx.setText("Cancel\nRefund Transaction");
        }
        else {
            isTrxRunning = false;
            btnStartTrx.setText("Start\nTransaction");
            btnRefundTrx.setText("Refund\nTransaction");
            btnUploadSignature.setEnabled(false);
        }

    }

    public boolean isNfcEnabled(Context context) {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(context);
        if (adapter != null) {
            if (adapter.isEnabled())
            {
                return true;
            }
            else
            {
                runOnUiThread(() -> new AlertDialog.Builder(MainActivity.this)
                        .setMessage(R.string.ALERT_NFC_NOT_ENABLE)
                        .setPositiveButton(R.string.ALERT_BTN_OK, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton(R.string.BTN_SETTINGS, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
                                if (intent.resolveActivity(getPackageManager()) != null) {
                                    startActivity(intent);
                                    return;
                                }
                                intent = new Intent(Settings.ACTION_SETTINGS);
                                if (intent.resolveActivity(getPackageManager()) != null) {
                                    startActivity(intent);
                                } else {
                                    new AlertDialog.Builder(MainActivity.this)
                                            .setMessage(R.string.ALERT_NOT_SUPPORTED_MSG)
                                            .setCancelable(true)
                                            .setPositiveButton(R.string.ALERT_BTN_OK, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            })
                                            .create()
                                            .show();
                                }
                            }
                        })
                        .setCancelable(false)
                        .create()
                        .show()
                );
                return false;
            }
        }

        // NFC not supported
        btnStartTrx.setEnabled(false);
        return false;
    }

        private void initFasstapMPOSSDK()
        {
            writeLog("Init...");
            Context context = getApplicationContext();

            SSMPOSSDKConfiguration config = SSMPOSSDKConfiguration.Builder.create()
                    .setAttestationHost(BuildConfig.ATTESTATION_HOST)
                    .setAttestationHostCertPinning(BuildConfig.ATTESTATION_CERT_PINNING)
                    .setAttestationHostReadTimeout(10000L)
                    .setAttestationRefreshInterval(300000L)
                    .setAttestationStrictHttp(true)
                    .setAttestationConnectionTimeout(30000L)
                    .setLibGooglePlayProjNum("1065331747627") // use own google play project number
                    .setLibAccessKey(BuildConfig.ACCESS_KEY)
                    .setLibSecretKey(BuildConfig.SECRET_KEY)
                    .setUniqueID(edtUserID.getText().toString()) // please set the userID shared by Soft Space
                    .setDeveloperID(edtDeveloperID.getText().toString())
                    .setEnvironment(BuildConfig.FLAVOR_environment.equals("uat") ? Environment.UAT : Environment.PROD)
                    .build();

            SSMPOSSDK.init(context, config);
            if(SSMPOSSDK.getInstance().getSdkVersion().isEmpty()){

            }else {
                refreshToken();
            }
            writeLog("SDK Version: " + SSMPOSSDK.getInstance().getSdkVersion());
            writeLog("COTS ID: " + SSMPOSSDK.getInstance().getCotsId());

            if (!SSMPOSSDK.hasRequiredPermission(getApplicationContext())) {
                SSMPOSSDK.requestPermissionIfRequired(this, 1000);
                Log.e("2222222222222222", "7777777777777777777");
            }
        }

    private void refreshToken()
    {
        writeLog("refreshToken()");
        SSMPOSSDK.getInstance().getSSMPOSSDKConfiguration().uniqueID = edtUserID.getText().toString();
        SSMPOSSDK.getInstance().getSSMPOSSDKConfiguration().developerID = edtDeveloperID.getText().toString();
        SSMPOSSDK.getInstance().getTransaction().refreshToken(this, new MPOSTransaction.TransactionEvents() {
            @Override
            public void onTransactionResult(int result, MPOSTransactionOutcome transactionOutcome) {
                writeLog("onTransactionResult :: " + result);
//                writeLog("onTransactionOutCome ::" + transactionOutcome.getStatusCode());

                if(result == TransactionSuccessful) {
                    btnStartTrx.setEnabled(true);
                    startTrx();
                    dismissProgressDialog();
                    imageView.setVisibility(View.VISIBLE);
                    text_status.setVisibility(View.VISIBLE);
                    text_status.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.colorBlack));
                    btnVoidTrx.setEnabled(false);
                    btnGetTransactionStatus.setEnabled(false);
                    btnSettlement.setEnabled(true);
                } else {
                    if(transactionOutcome != null) {
                        writeLog(transactionOutcome.getStatusCode() + " - " + transactionOutcome.getStatusMessage());
                    }
                }
            }

            @Override
            public void onTransactionUIEvent(int event) {
                writeLog("onTransactionUIEvent :: " + event);
            }
        });
    }

    private void startEMVProcessing()
    {
        if (edtAmtAuth.getText().toString() != null && Double.parseDouble(edtAmtAuth.getText().toString()) <= 0)
        {
            writeLog("Amount cannot be zero!");
            toggleTransactionRunning(false);
            return;
        }

        // run aync task as blocking.
        this.runOnUiThread(() -> {
//            clearLogs();
            writeLog("Amount, Authorised: " + edtAmtAuth.getText().toString());
        });

        try {
            _transactionOutcome = null;

            MPOSTransactionParams transactionalParams = MPOSTransactionParams.Builder.create()
                    .setReferenceNumber(edtRefNo.getText().toString())
                    .setAmount(edtAmtAuth.getText().toString())
                    .build();

            SSMPOSSDK.getInstance().getTransaction().startTransaction(this, transactionalParams, new MPOSTransaction.TransactionEvents() {
                @Override
                public void onTransactionResult(int result, MPOSTransactionOutcome transactionOutcome) {
                    _transactionOutcome = transactionOutcome;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            clearLogs();
                            writeLog("onTransactionResult :: " + result);
                            writeLog("onTransactionOutCome ::" + transactionOutcome.getStatusCode());

                            if(result == TransactionSuccessful)
                            {
                                edtTrxID.setText(transactionOutcome.getTransactionID());
                                btnVoidTrx.setEnabled(true);
                                btnRefundTrx.setEnabled(true);
                                btnGetTransactionStatus.setEnabled(true);
                                btnUploadSignature.setEnabled(false);

                                String outcome = "Transaction ID :: " + transactionOutcome.getTransactionID() + "\n";
                                outcome += "Reference No :: " + transactionOutcome.getReferenceNo() + "\n";
                                outcome += "Approval code :: " + transactionOutcome.getApprovalCode() + "\n";
                                outcome += "Card number :: " + transactionOutcome.getCardNo() + "\n";
                                outcome += "Cardholder name :: " + transactionOutcome.getCardHolderName() + "\n";
                                outcome += "Acquirer ID :: " + transactionOutcome.getAcquirerID() + "\n";
                                outcome += "Contactless CVM Type :: " + transactionOutcome.getContactlessCVMType() + "\n";
                                outcome += "RRN :: " + transactionOutcome.getRrefNo()+ "\n";
                                outcome += "Trace No :: " + transactionOutcome.getTraceNo()+ "\n";
                                outcome += "Transaction Date Time UTC :: " + transactionOutcome.getTransactionDateTime();
                                writeLog(outcome);

                                if(CARD_TYPE_VISA.equals(transactionOutcome.getCardType()))
                                {
                                    animateVisaSensoryBranding();
                                }
                                else if(CARD_TYPE_MASTERCARD.equals(transactionOutcome.getCardType()))
                                {
                                    animateMastercardSensoryTransaction();
                                }
                                else if(CARD_TYPE_AMEX.equals(transactionOutcome.getCardType()))
                                {
                                    animateAmexSensoryTransaction();
                                }
                                else if(CARD_TYPE_JCB.equals(transactionOutcome.getCardType()))
                                {
                                    animateJCBSensoryTransaction();
                                }
                                else if(CARD_TYPE_DISCOVER.equals(transactionOutcome.getCardType()))
                                {
                                    animateDiscoverSensoryTransaction();
                                }
                                String packageName = "io.ionic.starter";
                                String className = "io.ionic.starter.MainActivity";
                                Intent intent = new Intent();
                                intent.setComponent(new ComponentName(packageName, className));

                                String log_data = tvLogArea.getText().toString();
                                intent.putExtra("MerchantHost", MerchantHost);
                                intent.putExtra("OpType", OpType);
                                intent.putExtra("Currency", Currency);
                                intent.putExtra("Amount", Amount);
                                intent.putExtra("OrderId", OrderId);
                                intent.putExtra("Channel", Channel);
                                intent.putExtra("Tid", Tid);
                                intent.putExtra("Log", log_data);
                                PackageManager packageManager = getPackageManager();
                                if (intent.resolveActivity(packageManager) != null) {
                                    startActivity(intent);
                                } else {
                                    // The activity doesn't exist or the app is not installed
                                }
                            }
                            else if (result == TransactionFailed)
                            {
                                if(transactionOutcome != null)
                                {
                                    String outcome = transactionOutcome.getStatusCode() + " - " + transactionOutcome.getStatusMessage();
                                    if (transactionOutcome.getTransactionID() != null && transactionOutcome.getTransactionID().length() > 0)
                                    {
                                        outcome += "\nTransaction ID :: " + transactionOutcome.getTransactionID() + "\n";
                                        outcome += "Reference No :: " + transactionOutcome.getReferenceNo() + "\n";
                                        outcome += "Approval code :: " + transactionOutcome.getApprovalCode() + "\n";
                                        outcome += "Card number :: " + transactionOutcome.getCardNo() + "\n";
                                        outcome += "Cardholder name :: " + transactionOutcome.getCardHolderName() + "\n";
                                        outcome += "Acquirer ID :: " + transactionOutcome.getAcquirerID() + "\n";
                                        outcome += "RRN :: " + transactionOutcome.getRrefNo() + "\n";
                                        outcome += "Trace No :: " + transactionOutcome.getTraceNo() + "\n";
                                        outcome += "Transaction Date Time UTC :: " + transactionOutcome.getTransactionDateTime();
                                    }
                                    writeLog(outcome);
                                }
                                else
                                {
                                    writeLog("Error ::" + result);
                                }
                                String packageName = "io.ionic.starter";
                                String className = "io.ionic.starter.MainActivity";
                                Intent intent = new Intent();
                                intent.setComponent(new ComponentName(packageName, className));

                                String log_data = tvLogArea.getText().toString();
                                intent.putExtra("MerchantHost", MerchantHost);
                                intent.putExtra("OpType", OpType);
                                intent.putExtra("Currency", Currency);
                                intent.putExtra("Amount", Amount);
                                intent.putExtra("OrderId", OrderId);
                                intent.putExtra("Channel", Channel);
                                intent.putExtra("Tid", Tid);
                                intent.putExtra("Log", log_data);
                                PackageManager packageManager = getPackageManager();
                                if (intent.resolveActivity(packageManager) != null) {
                                    startActivity(intent);
                                } else {
                                    // The activity doesn't exist or the app is not installed
                                }

                            }

                            toggleTransactionRunning(false);
                        }
                    });
                }

                @Override
                public void onTransactionUIEvent(int event) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (event == TransactionUIEvent.CardReadOk)
                            {
                                // you may customize card reads OK sound & vibration, below is some example
                                ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
                                toneGenerator.startTone(ToneGenerator.TONE_DTMF_P, 500);

                                Vibrator v = (Vibrator) MainActivity.this.getSystemService(Context.VIBRATOR_SERVICE);
                                if (v.hasVibrator())
                                {
                                    v.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
                                }
                                writeLog("Card read completed");
                            }
                            else if (event == TransactionUIEvent.RequestSignature)
                            {
                                writeLog("Signature is required");
                                btnUploadSignature.setEnabled(true);
                            }
                            else
                            {
                                switch (event)
                                {
                                    case TransactionUIEvent.PresentCard:
                                    {
                                        writeLog("Present your card");
                                        text_status.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.general_green_text));
                                        text_status.setText("Present your card");
                                    }
                                    break;
                                    case TransactionUIEvent.Authorising:
                                    {
                                        writeLog("Authorising...");
                                        text_status.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.colorBlack));
                                        text_status.setText("Authorising...");
                                    }
                                    break;
                                    case TransactionUIEvent.CardPresented:
                                    {
                                        writeLog("Card detected");
                                        text_status.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.general_green_text));
                                        text_status.setText("Card detected");
                                    }
                                    break;
                                    case TransactionUIEvent.CardReadError:
                                    {
                                        writeLog("Card read failed");
                                        text_status.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.colorRed));
                                        text_status.setText("Card read failed");
                                    }
                                    case TransactionUIEvent.CardReadRetry:
                                    {
                                        writeLog("Card read retry");
                                        text_status.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.colorRed));
                                        text_status.setText("Card read retry");
                                    }
                                    break;
                                    default:
                                        writeLog("onTransactionUIEvent :: " + event);
                                        break;
                                }
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void uploadSignature()
    {
        writeLog("uploadSignature()");

        String base64SignatureString = ""; // your signature image base64 string

        try {
            MPOSTransactionParams transactionalParams = MPOSTransactionParams.Builder.create()
                    .setSignature(base64SignatureString)
                    .build();

            SSMPOSSDK.getInstance().getTransaction().uploadSignature(transactionalParams);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void voidTransaction()
    {
        writeLog("voidTransaction()");
        try {
            MPOSTransactionParams transactionalParams = MPOSTransactionParams.Builder.create()
                    .setMPOSTransactionID(edtTrxID.getText().toString())
                    .build();

            SSMPOSSDK.getInstance().getTransaction().voidTransaction(this, transactionalParams, new MPOSTransaction.TransactionEvents() {
                @Override
                public void onTransactionResult(int result, MPOSTransactionOutcome transactionOutcome) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            writeLog("onTransactionResult :: " + result);
                            writeLog("onTransactionOutCome ::" + transactionOutcome.getStatusCode());
                            if(result == TransactionSuccessful) {
                                btnVoidTrx.setEnabled(false);
                                if (transactionOutcome != null && transactionOutcome.getTransactionID() != null && transactionOutcome.getTransactionID().length() > 0)
                                {
                                    String outcome = "Status :: " + transactionOutcome.getStatusCode() + " - " + (mapStatusCode(transactionOutcome.getStatusCode()).length()>0?mapStatusCode(transactionOutcome.getStatusCode()):transactionOutcome.getStatusMessage()) + "\n";
                                    outcome += "Transaction ID :: " + transactionOutcome.getTransactionID() + "\n";
                                    outcome += "Reference no :: " + transactionOutcome.getReferenceNo() + "\n";
                                    outcome += "Approval code :: " + transactionOutcome.getApprovalCode() + "\n";
                                    outcome += "Invoice no :: " + transactionOutcome.getInvoiceNo() + "\n";
                                    outcome += "AID :: " + transactionOutcome.getAid() + "\n";
                                    outcome += "Card type :: " + transactionOutcome.getCardType() + "\n";
                                    outcome += "Application label :: " + transactionOutcome.getApplicationLabel() + "\n";
                                    outcome += "Card number :: " + transactionOutcome.getCardNo() + "\n";
                                    outcome += "Cardholder name :: " + transactionOutcome.getCardHolderName()+ "\n";
                                    outcome += "RRN :: " + transactionOutcome.getRrefNo() + "\n";
                                    outcome += "Trace No :: " + transactionOutcome.getTraceNo()+ "\n";
                                    outcome += "Transaction Date Time UTC :: " + transactionOutcome.getTransactionDateTime();
                                    writeLog(outcome);
                                }
                            }
                            else {
                                if (transactionOutcome != null) {
                                    writeLog(transactionOutcome.getStatusCode() + " - " + transactionOutcome.getStatusMessage());
                                }
                            }
                        }
                    });
                }

                @Override
                public void onTransactionUIEvent(int event) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            writeLog("onTransactionUIEvent :: " + event);
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void refundTransaction()
    {
        writeLog("refundTransaction()");
        try {
            MPOSTransactionParams transactionalParams = MPOSTransactionParams.Builder.create()
                    .setMPOSTransactionID(edtTrxID.getText().toString())
                    .setAmount(edtAmtAuth.getText().toString())
                    .build();

            SSMPOSSDK.getInstance().getTransaction().refundTransaction(this, transactionalParams, new MPOSTransaction.TransactionEvents() {
                @Override
                public void onTransactionResult(int result, MPOSTransactionOutcome transactionOutcome) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            writeLog("onTransactionResult :: " + result);
                            writeLog("onTransactionOutCome ::" + transactionOutcome.getStatusCode());
                            if(result == TransactionSuccessful) {
                                btnRefundTrx.setEnabled(false);
                                if (transactionOutcome != null && transactionOutcome.getTransactionID() != null && transactionOutcome.getTransactionID().length() > 0)
                                {
                                    String outcome = "Status :: " + transactionOutcome.getStatusCode() + " - " + (mapStatusCode(transactionOutcome.getStatusCode()).length()>0?mapStatusCode(transactionOutcome.getStatusCode()):transactionOutcome.getStatusMessage()) + "\n";
                                    outcome += "Transaction ID :: " + transactionOutcome.getTransactionID() + "\n";
                                    outcome += "Reference no :: " + transactionOutcome.getReferenceNo() + "\n";
                                    outcome += "Approval code :: " + transactionOutcome.getApprovalCode() + "\n";
                                    outcome += "Invoice no :: " + transactionOutcome.getInvoiceNo() + "\n";
                                    outcome += "AID :: " + transactionOutcome.getAid() + "\n";
                                    outcome += "Card type :: " + transactionOutcome.getCardType() + "\n";
                                    outcome += "Application label :: " + transactionOutcome.getApplicationLabel() + "\n";
                                    outcome += "Card number :: " + transactionOutcome.getCardNo() + "\n";
                                    outcome += "Cardholder name :: " + transactionOutcome.getCardHolderName() + "\n";
                                    outcome += "RRN :: " + transactionOutcome.getRrefNo() + "\n";
                                    outcome += "Trace No :: " + transactionOutcome.getTraceNo() + "\n";
                                    outcome += "Transaction Date Time UTC :: " + transactionOutcome.getTransactionDateTime();
                                    writeLog(outcome);
                                }
                            }
                            else {
                                if (transactionOutcome != null) {
                                    writeLog(transactionOutcome.getStatusCode() + " - " + transactionOutcome.getStatusMessage());
                                }
                            }
                        }
                    });
                }

                @Override
                public void onTransactionUIEvent(int event) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            writeLog("onTransactionUIEvent :: " + event);
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void refundTransactionWithCardPresented()
    {
        writeLog("refundTransactionWithCardPresented()");

        if (edtAmtAuth.getText().toString() != null && Double.parseDouble(edtAmtAuth.getText().toString()) <= 0)
        {
            writeLog("Amount cannot be zero!");
            toggleTransactionRunning(false);
            return;
        }

        // run aync task as blocking.
        this.runOnUiThread(() -> {
//            clearLogs();
            writeLog("Amount, Authorised: " + edtAmtAuth.getText().toString());
        });

        try {
            _transactionOutcome = null;

            MPOSTransactionParams transactionalParams = MPOSTransactionParams.Builder.create()
                    .setReferenceNumber(edtRefNo.getText().toString())
                    .setAmount(edtAmtAuth.getText().toString())
                    .setCardRequiredForRefund(true)
                    .build();

            if (transactionalParams.isCardRequiredForRefund())
            {
                toggleTransactionRunning(true);
            }

            SSMPOSSDK.getInstance().getTransaction().refundTransaction(this, transactionalParams, new MPOSTransaction.TransactionEvents() {
                @Override
                public void onTransactionResult(int result, MPOSTransactionOutcome transactionOutcome) {
                    _transactionOutcome = transactionOutcome;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            writeLog("onTransactionResult :: " + result);
                            writeLog("onTransactionOutCome ::" + transactionOutcome.getStatusCode());

                            if(result == TransactionSuccessful)
                            {
                                edtTrxID.setText(transactionOutcome.getTransactionID());
                                btnVoidTrx.setEnabled(true);
                                btnGetTransactionStatus.setEnabled(true);

                                String outcome = "Transaction ID :: " + transactionOutcome.getTransactionID() + "\n";
                                outcome += "Approval code :: " + transactionOutcome.getApprovalCode() + "\n";
                                outcome += "Card number :: " + transactionOutcome.getCardNo() + "\n";
                                outcome += "Cardholder name :: " + transactionOutcome.getCardHolderName() + "\n";
                                outcome += "RRN :: " + transactionOutcome.getRrefNo();
                                writeLog(outcome);

                                if(CARD_TYPE_VISA.equals(transactionOutcome.getCardType()))
                                {
                                    animateVisaSensoryBranding();
                                }
                                else if(CARD_TYPE_MASTERCARD.equals(transactionOutcome.getCardType()))
                                {
                                    animateMastercardSensoryTransaction();
                                }
                                else if(CARD_TYPE_AMEX.equals(transactionOutcome.getCardType()))
                                {
                                    animateAmexSensoryTransaction();
                                }
                                else if(CARD_TYPE_JCB.equals(transactionOutcome.getCardType()))
                                {
                                    animateJCBSensoryTransaction();
                                }
                            }
                            else
                            {
                                if(transactionOutcome != null)
                                {
                                    writeLog(transactionOutcome.getStatusCode() + " - " + transactionOutcome.getStatusMessage());
                                }
                                else
                                {
                                    writeLog("Error ::" + result);
                                }
                            }

                            toggleTransactionRunning(false);
                        }
                    });
                }

                @Override
                public void onTransactionUIEvent(int event) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (event == TransactionUIEvent.CardReadOk)
                            {
                                // you may customize card reads OK sound & vibration, below is some example
                                ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
                                toneGenerator.startTone(ToneGenerator.TONE_DTMF_P, 500);

                                Vibrator v = (Vibrator) MainActivity.this.getSystemService(Context.VIBRATOR_SERVICE);
                                if (v.hasVibrator())
                                {
                                    v.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
                                }
                            }
                            writeLog("onTransactionUIEvent :: " + event);
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void getTransactionStatus()
    {
        writeLog("getTransactionStatus()");
        try {
            String trxID = edtTrxID.getText().toString();
            String referenceNo = edtRefNo.getText().toString();

            MPOSTransactionParams transactionalParams = MPOSTransactionParams.Builder.create().build();

            if (trxID.length() > 0)
            {
                transactionalParams = MPOSTransactionParams.Builder.create()
                        .setMPOSTransactionID(edtTrxID.getText().toString())
                        .build();
            }
            else if (referenceNo.length() > 0)
            {
                transactionalParams = MPOSTransactionParams.Builder.create()
                        .setReferenceNumber(edtRefNo.getText().toString())
                        .build();
            }

            SSMPOSSDK.getInstance().getTransaction().getTransactionStatus(this, transactionalParams, new MPOSTransaction.TransactionEvents() {
                @Override
                public void onTransactionResult(int result, MPOSTransactionOutcome transactionOutcome) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(result == TransactionSuccessful)
                            {
                                if (transactionOutcome.getStatusCode().equals(TRX_STATUS_APPROVED))
                                {
                                    btnVoidTrx.setEnabled(true);
                                }
                                else if (transactionOutcome.getStatusCode().equals(TRX_STATUS_SETTLED))
                                {
                                    btnRefundTrx.setEnabled(true);
                                }
                                String outcome = "Status :: " + transactionOutcome.getStatusCode() + " - " + (mapStatusCode(transactionOutcome.getStatusCode()).length()>0?mapStatusCode(transactionOutcome.getStatusCode()):transactionOutcome.getStatusMessage()) + "\n";
                                outcome += "Reference no :: " + transactionOutcome.getReferenceNo() + "\n";
                                outcome += "Amount auth :: " + transactionOutcome.getAmountAuthorized() + "\n";
                                outcome += "Transaction ID :: " + transactionOutcome.getTransactionID() + "\n";
                                outcome += "Transaction date :: " + transactionOutcome.getTransactionDate() + "\n";
                                outcome += "Batch no :: " + transactionOutcome.getBatchNo() + "\n";
                                outcome += "Approval code :: " + transactionOutcome.getApprovalCode() + "\n";
                                outcome += "Invoice no :: " + transactionOutcome.getInvoiceNo() + "\n";
                                outcome += "AID :: " + transactionOutcome.getAid() + "\n";
                                outcome += "Card type :: " + transactionOutcome.getCardType() + "\n";
                                outcome += "Application label :: " + transactionOutcome.getApplicationLabel() + "\n";
                                outcome += "Card number :: " + transactionOutcome.getCardNo() + "\n";
                                outcome += "Cardholder name :: " + transactionOutcome.getCardHolderName()+ "\n";
                                outcome += "Trace no :: " + transactionOutcome.getTraceNo()+ "\n";
                                outcome += "RRN :: " + transactionOutcome.getRrefNo() + "\n";
                                outcome += "Transaction Date Time UTC :: " + transactionOutcome.getTransactionDateTime();

                                writeLog(outcome);
                            }
                            else
                            {
                                if(transactionOutcome != null)
                                {
                                    writeLog(transactionOutcome.getStatusCode() + " - " + transactionOutcome.getStatusMessage());
                                }
                                else
                                {
                                    writeLog("Error ::" + result);
                                }
                            }
                        }
                    });
                }

                @Override
                public void onTransactionUIEvent(int event) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            writeLog("onTransactionUIEvent :: " + event);
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void performSettlement()
    {
        writeLog("performSettlement()");
        try {
            MPOSTransactionParams transactionalParams = MPOSTransactionParams.Builder.create()
                    .build();

            SSMPOSSDK.getInstance().getTransaction().performSettlement(this, transactionalParams, new MPOSTransaction.TransactionEvents() {
                @Override
                public void onTransactionResult(int result, MPOSTransactionOutcome transactionOutcome) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            writeLog("onTransactionResult :: " + result);
                            writeLog("onTransactionOutCome ::" + transactionOutcome.getStatusCode());
                            if(result != TransactionSuccessful && transactionOutcome != null) {
                                writeLog(transactionOutcome.getStatusCode() + " - " + transactionOutcome.getStatusMessage());
                            }
                        }
                    });
                }

                @Override
                public void onTransactionUIEvent(int event) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            writeLog("onTransactionUIEvent :: " + event);
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    // ============================================================================================
    // Sensory Methods
    // ============================================================================================

    private void updateSensoryBranding() {
        visaSensoryBrandingView.setBackdropColor(ContextCompat.getColor(this, R.color.colorWhite));
        visaSensoryBrandingView.setSoundEnabled(true);
        visaSensoryBrandingView.setHapticFeedbackEnabled(true);
        visaSensoryBrandingView.setCheckmarkMode(CheckmarkMode.CHECKMARK_WITH_TEXT);
        visaSensoryBrandingView.setCheckmarkText(CheckmarkTextOption.APPROVE);
        visaSensoryBrandingView.setLanguageCode("ja");
    }

    private void animateVisaSensoryBranding()
    {
        updateSensoryBranding();
        visaSensoryBrandingView.setVisibility(View.VISIBLE);
        layoutSensoryBranding.setVisibility(View.VISIBLE);

        visaSensoryBrandingView.animate( error -> {
            visaSensoryBrandingView.setBackdropColor(ContextCompat.getColor(this, R.color.transparent));
            visaSensoryBrandingView.setVisibility(View.GONE);
            layoutSensoryBranding.setVisibility(View.GONE);
            return null;
        });
    }

    private void animateMastercardSensoryTransaction()
    {
        layoutSensoryBranding.setVisibility(View.VISIBLE);
        sensoryBrandingVideoView.setVisibility(View.VISIBLE);

        String path = "android.resource://" + getPackageName() + "/" + R.raw.mc_sensory_transaction;
        sensoryBrandingVideoView.setVideoURI(Uri.parse(path));
        sensoryBrandingVideoView.setZOrderOnTop(true);
        sensoryBrandingVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
        {
            @Override
            public void onCompletion(MediaPlayer mp)
            {
                sensoryBrandingVideoView.suspend();
                layoutSensoryBranding.setVisibility(View.GONE);
                sensoryBrandingVideoView.setVisibility(View.GONE);
            }
        });
        sensoryBrandingVideoView.start();
    }

    private void animateAmexSensoryTransaction()
    {
        layoutSensoryBranding.setVisibility(View.VISIBLE);
        sensoryBrandingVideoView.setVisibility(View.VISIBLE);

        String path = "android.resource://" + getPackageName() + "/" + R.raw.amex_sensory_transaction;
        sensoryBrandingVideoView.setVideoURI(Uri.parse(path));
        sensoryBrandingVideoView.setZOrderOnTop(true);
        sensoryBrandingVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
        {
            @Override
            public void onCompletion(MediaPlayer mp)
            {
                sensoryBrandingVideoView.suspend();
                layoutSensoryBranding.setVisibility(View.GONE);
                sensoryBrandingVideoView.setVisibility(View.GONE);
            }
        });
        sensoryBrandingVideoView.start();
    }

    private void animateJCBSensoryTransaction()
    {
        layoutSensoryBranding.setVisibility(View.VISIBLE);
        sensoryBrandingVideoView.setVisibility(View.VISIBLE);

        String path = "android.resource://" + getPackageName() + "/" + R.raw.jcb_sensory_transaction;
        sensoryBrandingVideoView.setVideoURI(Uri.parse(path));
        sensoryBrandingVideoView.setZOrderOnTop(true);
        sensoryBrandingVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
        {
            @Override
            public void onCompletion(MediaPlayer mp)
            {
                sensoryBrandingVideoView.suspend();
                layoutSensoryBranding.setVisibility(View.GONE);
                sensoryBrandingVideoView.setVisibility(View.GONE);
            }
        });
        sensoryBrandingVideoView.start();
    }

    private void animateDiscoverSensoryTransaction()
    {
        layoutSensoryBranding.setVisibility(View.VISIBLE);
        sensoryBrandingVideoView.setVisibility(View.VISIBLE);

        String path = "android.resource://" + getPackageName() + "/" + R.raw.discover_sensory_transaction;
        sensoryBrandingVideoView.setVideoURI(Uri.parse(path));
        sensoryBrandingVideoView.setZOrderOnTop(true);
        sensoryBrandingVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
        {
            @Override
            public void onCompletion(MediaPlayer mp)
            {
                sensoryBrandingVideoView.suspend();
                layoutSensoryBranding.setVisibility(View.GONE);
                sensoryBrandingVideoView.setVisibility(View.GONE);
            }
        });
        sensoryBrandingVideoView.start();
    }

    private String mapStatusCode(String code)
    {
        switch (code)
        {
            case TRX_STATUS_APPROVED:
                return "Approved";

            case TRX_STATUS_REVERSED:
                return "Reversed";

            case TRX_STATUS_VOIDED:
                return "Voided";

            case TRX_STATUS_PENDING_SIGNATURE:
                return "Pending Signature";

            case TRX_STATUS_SETTLED:
                return "Settled";

            case TRX_STATUS_PENDING_TC:
                return "Pending TC";

            case TRX_STATUS_REFUNDED:
                return "Refunded";
        }

        return code;
    }

    @Override
    public void onRequestPermissionsResult(int resultCode, String[] permissions, int[] grantResult) {

        switch (resultCode) {
            case 1000:
                if (grantResult.length > 0 && grantResult[0] == PackageManager.PERMISSION_GRANTED){
                    writeLog("Permission granted");
                }
                else {
                    writeLog("Permission not granted, cant proceed");
                }
                break;
        }
    }
}
