package com.example.img2dcm_andriod;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public List<String> jpeg2dcmFileList = new ArrayList<>();
    public List<String> dcmFileList = new ArrayList<>();
    private final int REQUEST_PHONE_PERMISSIONS = 0;


    private void setPermission() {
        final List<String> permissionsList = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED))
                permissionsList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (permissionsList.size() == 0) {
            } else {
                requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                        REQUEST_PHONE_PERMISSIONS);
            }
        } else {
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPermission();
        setContentView(R.layout.activity_main);


        //Choose JPG file to convert
        Button fileSelect = findViewById(R.id.file_select);
        fileSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                DialogProperties properties = new DialogProperties();
                properties.selection_mode = DialogConfigs.SINGLE_MODE;
                properties.selection_type = DialogConfigs.FILE_SELECT;
                properties.root = new File(DialogConfigs.DEFAULT_DIR);
                properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
                properties.offset = new File(DialogConfigs.DEFAULT_DIR);
                properties.extensions = new String[]{"jpg" , "png" , "bmp" , "jpeg"};
                FilePickerDialog dialog = new FilePickerDialog(MainActivity.this,properties);
                dialog.setTitle("Select a File");
                dialog.setDialogSelectionListener(new DialogSelectionListener() {
                    @Override
                    public void onSelectedFilePaths(String[] files) {
                        //files is the array of the paths of files selected by the Application User.
                        for (int i = 0 ; i< files.length ; i++) {
                            jpeg2dcmFileList.add(files[i]);
                            String dcmFilename = files[i]+".dcm";
                            jpeg2dcmFileList.add(dcmFilename);
                        }
                    }
                });
                dialog.show();
            }
        });

        //Convert JPG to DICOM
        Button genDicom = findViewById(R.id.gen_dicom);
        genDicom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (jpeg2dcmFileList.size() > 0) {
                    String jpegFilename = jpeg2dcmFileList.get(0);
                    String dicomFilename = jpeg2dcmFileList.get(1);
                    new jpeg2DCMAsyncTask().execute(jpegFilename,dicomFilename);
                } else {
                    Toast.makeText(MainActivity.this, "請選擇jpeg檔案", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //Choose DICOM file to upload
        Button dcmSelect = findViewById(R.id.dcm_select);
        dcmSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogProperties properties = new DialogProperties();
                properties.selection_mode = DialogConfigs.SINGLE_MODE;
                properties.selection_type = DialogConfigs.FILE_SELECT;
                properties.root = new File(DialogConfigs.DEFAULT_DIR);
                properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
                properties.offset = new File(DialogConfigs.DEFAULT_DIR);
                properties.extensions = new String[]{"dcm"};
                FilePickerDialog dialog = new FilePickerDialog(MainActivity.this,properties);
                dialog.setTitle("Select a File");
                dialog.setDialogSelectionListener(new DialogSelectionListener() {
                    @Override
                    public void onSelectedFilePaths(String[] files) {
                        //files is the array of the paths of files selected by the Application User.
                        for (int i = 0 ; i< files.length ; i++) {
                            dcmFileList.add(files[i]);
                        }
                    }
                });
                dialog.show();
            }
        });

        //Upload DICOM file to CStore
        Button scuDicom = findViewById(R.id.scu_dicom);
        scuDicom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dcmFileList.size() > 0) {
                    String dcmPath = dcmFileList.get(0);
                    new dcm2SCUAsyncTask().execute(dcmPath);
                } else {
                    Toast.makeText(MainActivity.this, "請選擇dicom檔案", Toast.LENGTH_SHORT).show();
                }
            }
        });


    }


    private Boolean uploadDCM (String dcmPath) throws Exception {
        DcmStoreSCU scu = new DcmStoreSCU();
        scu.initServerInfo("ANDROIDSCU","60.249.179.121",4242);
        scu.sendDicomFile(dcmPath);
        return true;
    }

    private Boolean convertJpeg2DCM (String srcPath , String desPath) throws Exception {
        ImageToDicomService imageToDicomService = new ImageToDicomService();
        imageToDicomService.convertJpg2Dcm(srcPath, desPath);
        return true;
    }

    private class jpeg2DCMAsyncTask extends AsyncTask<String,Integer,Boolean>{

        @Override
        protected Boolean doInBackground(String... strings) {

            try {
                return convertJpeg2DCM(strings[0],strings[1]);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

        }

        @Override
        protected void onPostExecute(Boolean result) {
            Toast.makeText(MainActivity.this, "Converted", Toast.LENGTH_SHORT).show();
        }

    }

    private class dcm2SCUAsyncTask extends AsyncTask<String,Integer,Boolean>{

        @Override
        protected Boolean doInBackground(String... strings) {

            try {
                return uploadDCM(strings[0]);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

        }

        @Override
        protected void onPostExecute(Boolean result) {
            Toast.makeText(MainActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
        }

    }

}