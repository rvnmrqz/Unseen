package com.rvnmrqzdevgmail.unseen;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Random;

import de.hdodenhof.circleimageview.CircleImageView;


public class MyProfileActivity extends AppCompatActivity {

    String TAG = "MyProfileActivity";
    Toolbar toolbar;
    FirebaseAuth mAuth;
    DatabaseReference userDBRef;
    StorageReference mStorageRef;

    TextView txtDisplayname, txtBio;
    CircleImageView circleImageView;
    ValueEventListener userValueEvenListener;
    SharedPreferences sharedPreferences;


    ProgressDialog progressDialog;
    Button btnEditBio,btnChangePhoto;
    AlertDialog alertDialog;
    AlertDialog.Builder builder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);

        toolbar = findViewById(R.id.myprofile_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("My Profile");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mAuth = FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();

        progressDialog = new ProgressDialog(this);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.setTitle("Uploading Image");
        progressDialog.setMessage("Please wait while we upload your image");

        //checking
        if (!isUserlogged()) {
            noLoggedUserSignOut();
            return;
        }

        userDBRef = FirebaseDatabase.getInstance().getReference("Users").child(mAuth.getCurrentUser().getUid());

        circleImageView = findViewById(R.id.profileImg);
        txtDisplayname = findViewById(R.id.profileUsername);
        txtBio = findViewById(R.id.profileBio);
        sharedPreferences = getSharedPreferences(MySharedPref.SHAREDPREFNAME, MODE_PRIVATE);

        btnChangePhoto = findViewById(R.id.profile_btnChangePhoto);
        btnEditBio = findViewById(R.id.profile_btnUpdateBio);

        btnEditBio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editBio();
            }
        });
        btnChangePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updatePhoto();
            }
        });


        setUserValueEvenListener();
        setDisplay();
    }

    private void setUserValueEvenListener(){
        userValueEvenListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot userdata) {
                Log.d(TAG,"\n\nUser Data CHANGED "+userdata);
                String email = userdata.child("email").getValue().toString();
                String display_name = userdata.child("display_name").getValue().toString();
                String bio = userdata.child("bio").getValue().toString();
                String imgthumb = userdata.child("image_thumb").getValue().toString();
                String img = userdata.child("image").getValue().toString();
                saveinSharedPref(email,display_name,bio,imgthumb,img);

                Picasso.get()
                        .load(img)
                        .placeholder(R.drawable.default_avatar)
                        .error(R.drawable.default_avatar)
                        .into(circleImageView);

                txtDisplayname.setText(display_name);
                txtBio.setText(bio);

            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG,"Database Error: "+databaseError.getMessage());
            }
        };
    }

    private void setDisplay(){
        if(isUserlogged()){
            txtDisplayname.setText(sharedPreferences.getString(MySharedPref.USERNAME,"{Username}"));
            txtBio.setText(sharedPreferences.getString(MySharedPref.BIO,"{Bio}"));
            userDBRef.addValueEventListener(userValueEvenListener);
        }
    }

    private void saveinSharedPref(String email, String username, String bio, String imgthumb, String img){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(MySharedPref.USERNAME,username);
        editor.putString(MySharedPref.EMAIL,email);
        editor.putString(MySharedPref.BIO,bio);
        editor.putString(MySharedPref.IMG,img);
        editor.putString(MySharedPref.IMGTHUMB,imgthumb);
        editor.commit();
    }

    private void editBio(){
        LayoutInflater inflater = getLayoutInflater();
        View dialogView =  inflater.inflate(R.layout.dialog_input, null);

        TextView txtTitle = dialogView.findViewById(R.id.dialog_title);
        txtTitle.setText("Update Bio");
        final EditText txtInput = dialogView.findViewById(R.id.dialog_txtInput);
        final ProgressBar progressBar = dialogView.findViewById(R.id.dialog_progressbar);
        TextView txtDone = dialogView.findViewById(R.id.dialog_btnDone);
        TextView txtCancel = dialogView.findViewById(R.id.dialog_btnCancel);



        //set display
        if(!TextUtils.isEmpty(txtBio.getText().toString())){
            txtInput.setText(txtBio.getText().toString());
            txtInput.setSelection(txtInput.getText().toString().length());
        }


        txtDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
               //save bio either it is empty or not
                userDBRef.child("bio").setValue(txtInput.getText().toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressBar.setVisibility(View.INVISIBLE);
                        if(task.isComplete()){
                            alertDialog.dismiss();
                            Toast.makeText(MyProfileActivity.this,"Bio is updated",Toast.LENGTH_SHORT).show();
                        }
                        else Toast.makeText(MyProfileActivity.this, "Failed to update bio", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        txtCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        // create alert dialog
        alertDialog = builder.create();
        alertDialog.show();

    }

    private void updatePhoto(){
        /*
        // start picker to get image for cropping and then use the image in cropping activity
            CropImage.activity()
            .setGuidelines(CropImageView.Guidelines.ON)
            .start(this);

        // start cropping activity for pre-acquired image saved on the device
            CropImage.activity(imageUri)
             .start(this);

        // for fragment (DO NOT use `getActivity()`)
            CropImage.activity()
            .start(getContext(), this);
         */
        CropImage.activity()
                .setActivityTitle("Update Photo")
                .setAspectRatio(1,1)
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(this);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                uploadImage(resultUri);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Toast.makeText(this, "Cropping failed, try again", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadImage(Uri imgUri){

        progressDialog.show();
        String imgFilename = mAuth.getCurrentUser().getUid()+".jpg";
        StorageReference storeRef = mStorageRef.child("profile_images").child(imgFilename);

        storeRef.putFile(imgUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get a URL to the uploaded content
                        Uri downloadUrl = taskSnapshot.getDownloadUrl();
                        Log.d(TAG,downloadUrl.toString());
                        updateURLLink(downloadUrl.toString());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        progressDialog.dismiss();
                        Log.d(TAG,"Failed to upload image: "+exception.getMessage());
                        Toast.makeText(MyProfileActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void updateURLLink(String downloadUrl){
        Log.d(TAG,"Download URL "+downloadUrl);

        userDBRef.child("image").setValue(downloadUrl).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progressDialog.dismiss();
                if(task.isComplete())
                    Toast.makeText(MyProfileActivity.this, "Image uploaded", Toast.LENGTH_SHORT).show();
                else{
                    Toast.makeText(MyProfileActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        userDBRef.removeEventListener(userValueEvenListener);
    }

    //FOR CHECKING USER
    private boolean isUserlogged()
    {
        return  (mAuth.getCurrentUser().getUid()!=null);
    }
    private void noLoggedUserSignOut(){
        //no logged user, go to login
        Toast.makeText(this, "No logged user", Toast.LENGTH_SHORT).show();
        mAuth.signOut();
        Intent loginIntent = new Intent(MyProfileActivity.this,LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(loginIntent);
    }
}
