package com.clarifai.android.starter.api.v2.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import butterknife.BindView;
import clarifai2.api.ClarifaiResponse;
import clarifai2.dto.input.ClarifaiInput;
import clarifai2.dto.input.image.ClarifaiImage;
import clarifai2.dto.model.ConceptModel;
import clarifai2.dto.model.output.ClarifaiOutput;
import clarifai2.dto.prediction.Concept;
import com.clarifai.android.starter.api.v2.App;
import com.clarifai.android.starter.api.v2.ClarifaiUtil;
import com.clarifai.android.starter.api.v2.R;
import com.clarifai.android.starter.api.v2.adapter.RecognizeConceptsAdapter;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;


import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class RecognizeConceptsActivity extends BaseActivity {

//////////////////////////////////////////////////////////////////////////////////////////////////
//////Activity Lifecycle ////////////////////////////////////////////////////////////////////////

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    checkPermissions();
  }

  @Override protected void onStart() {
    super.onStart();

    resultsList.setLayoutManager(new LinearLayoutManager(this));
    resultsList.setAdapter(adapter);
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode != RESULT_OK) {
      return;
    }
    switch (requestCode) {
      case REQUEST_IMAGE_CAPTURE: {
        if (resultCode == RESULT_OK) {
          galleryAddPic();
          final byte[] imageBytes = ClarifaiUtil.retrieveSelectedImage(this, currentPhotoPath);
          if (imageBytes != null) {
            onImagePicked(imageBytes);
          }
        }
        break;
      }
    }
  }

  @Override protected int layoutRes() { return R.layout.activity_recognize; }

/////////////////////////////////////////////////////////////////////////////////////////////////
//////Permissions///////////////////////////////////////////////////////////////////////////////

  protected void checkPermissions(){
    boolean hasPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    if (!hasPermission) {
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
    }
    else{
      captureImage();
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    switch (requestCode)
    {
      case REQUEST_WRITE_STORAGE: {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
          captureImage();
        } else
        {
          Toast.makeText(this, "The app was not allowed to write to your storage. Hence, it cannot function properly. Please consider granting it this permission", Toast.LENGTH_LONG).show();
        }
      }
    }
  }

//////////////////////////////////////////////////////////////////////////////////////////////////
///////Setup Image File///////////////////////////////////////////////////////////////////////////

  static final int REQUEST_IMAGE_CAPTURE = 1;
  private static final int REQUEST_WRITE_STORAGE = 112;

  private static final String JPEG_FILE_PREFIX = "IMG_";
  private static final String JPEG_FILE_SUFFIX = ".jpg";
  private static final String CAMERA_DIR = "/dcim/";

  private String currentPhotoPath;

  private void captureImage(){
    currentPhotoPath = null;
    dispatchTakePictureIntent();
  }

  private void dispatchTakePictureIntent() {
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

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

    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
      startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }
  }

  private File createImageFile() throws IOException {
    // Create an image file name
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
    File albumF = getAlbumDir();
    File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
    return imageF;
  }

  private File getAlbumDir() {
    File storageDir = null;

    if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
      storageDir = new File (
              Environment.getExternalStorageDirectory()
                      + CAMERA_DIR
                      + getString(R.string.album_name)
      );

      if (storageDir != null) {
        storageDir.mkdirs();
        if (! storageDir.exists()) {
          Log.d("ImageTranslate", "failed to create directory");
          return null;
        }
      }
    }
    else {
      Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
    }
    return storageDir;
  }

  private void galleryAddPic() {
    Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
    File f = new File(currentPhotoPath);
    Uri contentUri = Uri.fromFile(f);
    mediaScanIntent.setData(contentUri);
    this.sendBroadcast(mediaScanIntent);
  }


//////////////////////////////////////////////////////////////////////////////////////////////////
//////////Clarifai API/////////////////////////////////////////////////////////////////////////////

  // the list of results that were returned from the API
  @BindView(R.id.resultsList) RecyclerView resultsList;

  // the view where the image the user selected is displayed
  @BindView(R.id.image) ImageView imageView;

  // switches between the text prompting the user to hit the FAB, and the loading spinner
  @BindView(R.id.switcher) ViewSwitcher switcher;

  @NonNull private final RecognizeConceptsAdapter adapter = new RecognizeConceptsAdapter();


  private void onImagePicked(@NonNull final byte[] imageBytes) {
    // Now we will upload our image to the Clarifai API
    setBusy(true);

    // Make sure we don't show a list of old concepts while the image is being uploaded
    adapter.setData(Collections.<Concept>emptyList());

    new AsyncTask<Void, Void, ClarifaiResponse<List<ClarifaiOutput<Concept>>>>() {
      @Override protected ClarifaiResponse<List<ClarifaiOutput<Concept>>> doInBackground(Void... params) {
        // The default Clarifai model that identifies concepts in images
        final ConceptModel generalModel = App.get().clarifaiClient().getDefaultModels().generalModel();

        // Use this model to predict, with the image that the user just selected as the input
        return generalModel.predict()
                .withInputs(ClarifaiInput.forImage(ClarifaiImage.of(imageBytes)))
                .executeSync();
      }

      @Override protected void onPostExecute(ClarifaiResponse<List<ClarifaiOutput<Concept>>> response) {
        setBusy(false);
        if (!response.isSuccessful()) {
          showErrorSnackbar(R.string.error_while_contacting_api);
          return;
        }
        final List<ClarifaiOutput<Concept>> predictions = response.get();
        if (predictions.isEmpty()) {
          showErrorSnackbar(R.string.no_results_from_api);
          return;
        }
        adapter.setData(predictions.get(0).data());
        imageView.setImageBitmap(BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length));
      }

      private void showErrorSnackbar(@StringRes int errorString) {
        Snackbar.make(
                root,
                errorString,
                Snackbar.LENGTH_INDEFINITE
        ).show();
      }
    }.execute();
  }

  private void setBusy(final boolean busy) {
    runOnUiThread(new Runnable() {
      @Override public void run() {
        switcher.setDisplayedChild(busy ? 1 : 0);
        imageView.setVisibility(busy ? GONE : VISIBLE);
      }
    });
  }
}