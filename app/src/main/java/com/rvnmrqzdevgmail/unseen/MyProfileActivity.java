package com.rvnmrqzdevgmail.unseen;

import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class MyProfileActivity extends AppCompatActivity {

    String TAG = "MyProfileActivity";
    Toolbar toolbar;
    FirebaseAuth mAuth;
    DatabaseReference userDBRef;

    TextView txtDisplayname, txtBio;
    ValueEventListener userValueEvenListener;
    SharedPreferences sharedPreferences;

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

        mAuth = FirebaseAuth.getInstance();

        //checking
        if(!isUserlogged()){
          noLoggedUserSignOut();
          return;
        }

        userDBRef = FirebaseDatabase.getInstance().getReference("Users").child(mAuth.getCurrentUser().getUid());

        txtDisplayname = findViewById(R.id.profileUsername);
        txtBio = findViewById(R.id.profileBio);
        sharedPreferences = getSharedPreferences(MySharedPref.SHAREDPREFNAME,MODE_PRIVATE);

        btnChangePhoto = findViewById(R.id.profile_btnChangePhoto);
        btnEditBio = findViewById(R.id.profile_btnUpdateBio);

        btnEditBio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editBio();
            }
        });

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

                txtDisplayname.setText(display_name);
                txtBio.setText(bio);

            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG,"Database Error: "+databaseError.getMessage());
            }
        };

        setDisplay();


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

        final EditText txtInput = dialogView.findViewById(R.id.dialog_txtInput);
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
               //save bio either it is empty or not
                userDBRef.child("bio").setValue(txtInput.getText().toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
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
        builder.setTitle("Update Bio");
        builder.setView(dialogView);
        // create alert dialog
        alertDialog = builder.create();
        alertDialog.show();

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        userDBRef.removeEventListener(userValueEvenListener);
    }

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