package com.example.gustavius.myfirstapp;

import android.net.Uri;
import android.os.Environment;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.content.Intent;
import android.util.Log;
import java.util.Date;
import android.view.View;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.app.Activity;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;



public class MainActivity extends AppCompatActivity {
    static final int REQUEST_IMAGE_CAPTURE = 1;

    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";
    private static final String CAMERA_DIR = "/dcim/";

    /**private ImageView imageView; //displayed image back to user
    private Bitmap imageBitmap; //send to the API**/

    private String currentPhotoPath; //path where image is storred in gallery


    /**
     * main function do not touch
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    /**
     * Captures photo
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }

        File f = null;
        try {
            f = createImageFile();
            currentPhotoPath = f.getAbsolutePath();
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
        } catch (IOException e) {
            e.printStackTrace();
            f = null;
            currentPhotoPath = null;
        }
    }

    /**
     * saves image as a bitmap, returns it as a display
     * @return
     * @throws IOException
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
        File albumF = getAlbumDir();
        File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
        return imageF;
    }

    /**
     * Creates an album directory
     * @return
     */
    private File getAlbumDir() {
        File storageDir = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            storageDir = new File (
                            Environment.getExternalStorageDirectory()
                            + CAMERA_DIR
                            + getString(R.string.album_name)
            );

            if (storageDir != null) {
                if (! storageDir.mkdirs()) {
                    if (! storageDir.exists()){
                        Log.d("ImageTranslate", "failed to create directory");
                        return null;
                    }
                }
            }

        } else {
            Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
        }

        return storageDir;
    }

    /**
     * button press listener
     */
    private void captureImage(){
        currentPhotoPath = null;
        dispatchTakePictureIntent();
    }

    /**
     * after button press. Should extract from gallery, process and output
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) { //ADDS PIC TO GALLERY AND DISPLAYS IT IN imageView
            galleryAddPic(); //add pic to gallery
            ImageView imageView = (ImageView) findViewById(R.id.capturedImage);
            imageView.setImageBitmap(BitmapFactory.decodeFile(currentPhotoPath));
        }
    }

    /**
     * adds pic to gallery
     */
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }


    /**
     * dynamically changes language displayed
     */
    public class SpinnerActivity extends Activity implements OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View view,
                                   int pos, long id) {
            // An item was selected. You can retrieve the selected item using
            // parent.getItemAtPosition(pos)
            if(parent.getItemAtPosition(pos) == 1) //spanish
            {

            }
            else if(parent.getItemAtPosition(pos) == 2) //iroquois
            {

            }
            else //french. position 0 and by default. Parce que nous sommes au Quebec ici.
            {

            }
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Another interface callback
        }
    }


}
