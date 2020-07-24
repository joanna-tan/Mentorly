package com.example.mentorly.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;

import com.example.mentorly.BitmapScaler;
import com.example.mentorly.BuildConfig;
import com.example.mentorly.DeviceDimensionsHelper;
import com.example.mentorly.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

import static android.app.Activity.RESULT_OK;


public class AddPictureDialog extends DialogFragment {

    private static final String TAG = "AddPictureDialog";
    private ImageView ivPreviewPicture;
    private Button btnSubmit;
    private Button btnCapture;
    private TextView tvPreviewHeader;

    // handle capture image info
    public static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 242;
    File photoFile;
    String photoFileName = "photo.jpg";

    public AddPictureDialog() {
    }

    public static AddPictureDialog newInstance(String title) {
        AddPictureDialog frag = new AddPictureDialog();
        Bundle args = new Bundle();
        args.putString("title", title);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_picture_dialog, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ivPreviewPicture = view.findViewById(R.id.ivPreviewCapture);
        btnSubmit = view.findViewById(R.id.btnSubmitCapture);
        btnCapture = view.findViewById(R.id.btnCaptureImage);
        tvPreviewHeader = view.findViewById(R.id.tvPreviewPicture);

        ivPreviewPicture.setVisibility(View.GONE);

        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Launch camera and hide the submit button
                onLaunchCamera();
            }
        });

        //get the photo from the preview and return it to the parent fragment
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (photoFile == null || ivPreviewPicture.getDrawable() == null) {
                    Toast.makeText(getContext(), "There is no image!", Toast.LENGTH_SHORT).show();
                    return;
                }
                else {
                    sendBackResult();
                }
            }
        });
    }

    // Defines the listener interface
    public interface AddPictureDialogListener {
        void onFinishAddPictureDialog(File photo);
    }

    // Call this method to send the data back to the parent fragment
    public void sendBackResult() {
        // Notice the use of `getTargetFragment` which will be set when the dialog is displayed
        AddPictureDialogListener listener = (AddPictureDialogListener) getTargetFragment();
        listener.onFinishAddPictureDialog(this.photoFile);
        dismiss();
    }

    // Launches camera intent and saves image to file directory
    private void onLaunchCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Create a File reference
        photoFile = getPhotoFileUri(photoFileName);
        // wrap File object into a content provider
        Uri fileProvider = FileProvider.getUriForFile(Objects.requireNonNull(getContext()),
                BuildConfig.APPLICATION_ID + ".provider", photoFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider);
        // As long as intent is not null, app will not crash
        if (intent.resolveActivity(getContext().getPackageManager()) != null) {
            // Start the image capture intent to take photo
            startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
        }
    }

    // Returns the File for a photo stored on disk given the fileName
    private File getPhotoFileUri(String fileName) {
        // Get safe storage directory for photos
        File mediaStorageDir = new File(Objects.requireNonNull(getContext()).getExternalFilesDir(Environment.DIRECTORY_PICTURES), TAG);
        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            Log.d(TAG, "Failed to create directory");
        }
        // Return the file target for the photo based on filename
        return new File(mediaStorageDir.getPath() + File.separator + fileName);
    }

    // Once the picture is taken, resize and save it to a file path; then display preview
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Verify that request code is for image capture
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // by this point we have the camera photo on disk
                Bitmap rawTakenImage = BitmapFactory.decodeFile(photoFile.getAbsolutePath());

                File resizedUri = null;
                try {
                    resizedUri = resizeBitmap(rawTakenImage);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Picture wasn't saved!", Toast.LENGTH_SHORT).show();
                }
                assert resizedUri != null;
                Bitmap takenImage = BitmapFactory.decodeFile(resizedUri.getAbsolutePath());

                // Load the taken image into a preview
                ivPreviewPicture.setVisibility(View.VISIBLE);
                btnSubmit.setVisibility(View.VISIBLE);
                tvPreviewHeader.setText("Preview");
                btnCapture.setText("Try again");
                ivPreviewPicture.setImageBitmap(takenImage);


            } else { // Result was a failure
                Toast.makeText(getContext(), "Picture wasn't taken!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Called in onActivityResult to resize bitmap for scale
    private File resizeBitmap(Bitmap rawTakenImage) throws IOException {
        // Get height or width of screen at runtime
        int screenWidth = DeviceDimensionsHelper.getDisplayWidth(Objects.requireNonNull(getContext()));

        //Resize bitmap
        Bitmap resizedBitmap = BitmapScaler.scaleToFitWidth(rawTakenImage, screenWidth);
        //Configure byte output stream
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        //Compress the image further
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 40, bytes);
        //Create a new file for the resized bitmap
        File resizedFile = getPhotoFileUri(photoFileName + "_resize");

        resizedFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(resizedFile);
        // Write the bytes of the bitmap to file
        fos.write(bytes.toByteArray());
        fos.close();

        return resizedFile;
    }
}