package com.torch;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.support.v4.app.ActivityCompat;

//Use the Android device's flash light as a Torch. Enables strobing of the light on a new thread
public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 123;
    boolean hasCameraFlash = false;
    ToggleButton btnTorchLight;
    TextView turnTheTorchOnOffTextView;
    TextView frequencyTextView;
    boolean torchTurnedOn = false;
    boolean constantStrobing = false;
    long blinkDelay;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);

        hasCameraFlash = getPackageManager().
                hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        turnTheTorchOnOffTextView = (TextView) findViewById(R.id.turnTheTorchOnOffTextView);
        turnTheTorchOnOffTextView.setText("Turn the torch on");

        frequencyTextView = (TextView) findViewById(R.id.frequencyTextView);
        frequencyTextView.setText("Strobe frequency : 0");

        btnTorchLight = (ToggleButton) findViewById(R.id.toggleTorchlightButton);
        btnTorchLight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (hasCameraFlash) {
                    turnTorchOnOrOff(checked);
                    turnTorchOnOrOffTextViewUpdate(checked);
                }
                else {
                    Toast.makeText(MainActivity.this, "No flash available on your device",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });


        SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                blinkDelay = calculateFrequency(progress);
                frequencyTextView.setText("Strobe frequency : "+progress);

                if(constantStrobing == false) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //strobe the torch light 4 times
                            int count = 0;
                            while (count < 4) {
                                blinkTorch(blinkDelay);
                                count++;
                            }
                            turnTorchOnOrOff(torchTurnedOn);  //leave the torch on or off afterwards, as selected by the user
                        }
                    }).start();
                }else{
                    runConstantStrobing();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });


        CheckBox constantStrobingCheckBox = (CheckBox) findViewById(R.id.constantStrobingCheckBox);
        constantStrobingCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                constantStrobing = checked;
                runConstantStrobing();
            }
        });

    };


    //Constantly strobe the torch light in a new thread
    private void runConstantStrobing()
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(constantStrobing == true) {
                    blinkTorch(blinkDelay);
                }
                turnTorchOnOrOff(torchTurnedOn);  //leave the torch on or off afterwards, as selected by the user
            }
        }).start();
    }


    //Turn the torch on
    private void torchLightOn()
    {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = cameraManager.getCameraIdList()[0];
            cameraManager.setTorchMode(cameraId, true);
        } catch (CameraAccessException e) {
        }
    }


    //Turn the torch off
    private void torchLightOff()
    {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = cameraManager.getCameraIdList()[0];
            cameraManager.setTorchMode(cameraId, false);
        } catch (CameraAccessException e) {
        }
    }


    //Update the TextView to show whether the torch is turned on or off
    public void turnTorchOnOrOffTextViewUpdate(boolean checked)
    {
        if (checked) {
            turnTheTorchOnOffTextView.setText("Turn the torch off");
        } else {
            turnTheTorchOnOffTextView.setText("Turn the torch on");
        }
    }


    //Decide whether to turn the torch on or off based on the toggle flag
    public void turnTorchOnOrOff(boolean checked)
    {
        if (checked) {
            torchTurnedOn = true;
            torchLightOn();
        } else {
            torchTurnedOn = false;
            torchLightOff();
        }
    }


    //calculate the delay between each strobe of the torch light
    private long calculateFrequency(int frequency)
    {
        long blinkDelay = frequency * 3;
        return blinkDelay;
    }


    //Strobe the torch light on and off 1 time
    private void blinkTorch(long blinkDelay)
    {
        String torchSequence = "10";  //1=torch On, 0=torch Off

        for (int i = 0; i < torchSequence.length(); i++) {
            if (torchSequence.charAt(i) == '1') {
                torchLightOn();
            }else {
                torchLightOff();
            }
            try {
                Thread.sleep(blinkDelay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    //Request permission to use the device camera
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    hasCameraFlash = getPackageManager().
                            hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
                } else {
                    btnTorchLight.setEnabled(false);
                    Toast.makeText(MainActivity.this, "Permission Denied for the Camera", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

}
