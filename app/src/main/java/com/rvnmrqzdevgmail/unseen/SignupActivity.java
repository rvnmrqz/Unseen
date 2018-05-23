package com.rvnmrqzdevgmail.unseen;

import android.app.Dialog;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.scottyab.showhidepasswordedittext.ShowHidePasswordEditText;

import java.util.HashMap;

public class SignupActivity extends AppCompatActivity {


    Toolbar toolbar;
    String TAG = "RegistrationActivity";
    ImageView pictureBox;
    EditText txtEmail, txtDisplayname;
    ShowHidePasswordEditText txtPassword;
    Button btnSignup;

    Dialog dialog;

    private FirebaseAuth mAuth;
    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        toolbar = findViewById(R.id.reg_appbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Create Account");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mAuth = FirebaseAuth.getInstance();

        txtEmail = findViewById(R.id.regEmail);
        txtDisplayname = findViewById(R.id.regDisplayname);
        txtPassword = findViewById(R.id.regPassword);
        btnSignup = findViewById(R.id.btnSubmit);

        dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_loading);
        dialog.setCancelable(false);


        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!formValid()) return;
                signUp();
            }
        });

    }

    private void signUp(){
        try {
            showLoading(true);
            mAuth.createUserWithEmailAndPassword(txtEmail.getText().toString(), txtPassword.getText().toString())
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information

                                FirebaseUser user = task.getResult().getUser();
                                String uid = user.getUid();

                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(txtDisplayname.getText().toString()).build();
                                user.updateProfile(profileUpdates);

                                Log.d(TAG, "onComplete: UID: "+uid);
                                Toast.makeText(SignupActivity.this, "Logged: "+(mAuth.getCurrentUser().getUid()!=null), Toast.LENGTH_SHORT).show();

                                //saving info to realtime database
                                HashMap<String,String> userMap = new HashMap<>();
                                userMap.put("display_name", txtDisplayname.getText().toString());
                                userMap.put("email",txtEmail.getText().toString());
                                userMap.put("bio","Hi let's be friends"); //default, change this later
                                userMap.put("image","default_link");//default, change this later
                                userMap.put("image_thumb","default_link");//default, change this later

                                mDatabase.child("Users").child(uid).setValue(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        showLoading(false);
                                        if(task.isSuccessful()){
                                            Toast.makeText(SignupActivity.this, "Sign-up successful", Toast.LENGTH_LONG).show();
                                            finish();
                                        }else{
                                            Toast.makeText(SignupActivity.this, "Sign-up failed", Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
                            } else {
                                // If creating account fails, display a message to the user.
                                showLoading(false);
                                Toast.makeText(SignupActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }catch (Exception ee){
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
            Log.d(TAG,"Submit Error: "+ee.getMessage());
        }

    }


    private boolean formValid(){

        if(!txtEmail.getText().toString().contains("@")){
            return  false;
        }

        if(txtDisplayname.getText().toString().trim().length()==0){
            return  false;
        }

        if(txtPassword.getText().toString().length()==0){
            return  false;
        }

        return true;
    }

    private void showLoading(boolean show){
        if(show) dialog.show();
        else dialog.hide();
    }

}
