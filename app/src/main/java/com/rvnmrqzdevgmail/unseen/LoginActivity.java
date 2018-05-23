package com.rvnmrqzdevgmail.unseen;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.scottyab.showhidepasswordedittext.ShowHidePasswordEditText;

public class LoginActivity extends AppCompatActivity {


    String TAG = "LoginActivity";
    TextView txtForgotPassword;
    EditText txtUsername;
    ShowHidePasswordEditText txtPassword;
    Button btnSignIn;
    TextView txtSignUp;

    AlertDialog.Builder builder;
    AlertDialog alertDialog;

    TextView dialogMessage;
    DatabaseReference mDatabase;
    FirebaseAuth mAuth;
    SharedPreferences sharedPreferences;

    ProgressBar progressBar;

    @Override
    protected void onStart() {
        super.onStart();
        if(mAuth.getCurrentUser()!=null){
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        btnSignIn = findViewById(R.id.btnSignIn);
        txtUsername = findViewById(R.id.txtUsername);
        txtPassword = findViewById(R.id.txtPassword);
        txtForgotPassword = findViewById(R.id.txtForgotPassword);
        txtSignUp = findViewById(R.id.txtSignup);
        progressBar = findViewById(R.id.login_progressbar);

        sharedPreferences = getSharedPreferences(MySharedPref.SHAREDPREFNAME,MODE_PRIVATE);

        //CLICK LISTENERS
        signInListener();
        txtSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { startActivity(new Intent(LoginActivity.this,SignupActivity.class)); }
        });
        txtForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFogotPassDialog();
            }
        });
    }

    private boolean formValid(){
        String username = txtUsername.getText().toString();
        String password = txtPassword.getText().toString();
        if(username.isEmpty()){
            txtUsername.setError("Must not be empty");
            txtUsername.requestFocus();
            return false;
        }
        if(password.isEmpty()){
            txtPassword.setError("Must not be empty");
            txtPassword.requestFocus();
            return false;
        }
        return true;
    }

    private void signInListener(){
        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(formValid()){
                    Log.d(TAG,"Signing-in");
                    final String email = txtUsername.getText().toString();
                    final String password = txtPassword.getText().toString();
                    showLoading(true);
                    signInWithEmailAndPassword(email,password);
                }
            }
        });

    }

    //removed
    private void signInWithUsername(String username, final String password){
        //find the email by username then go to signInWithEmailAndPassword

        mDatabase = FirebaseDatabase.getInstance().getReference();
        Query query = mDatabase.child("Users").orderByChild("username").equalTo(username);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for (DataSnapshot user : dataSnapshot.getChildren())
                    {
                        //TODO get the data here
                        String email = (String) user.child("email").getValue();
                        dialogMessage.setText("Authenticating...");
                        signInWithEmailAndPassword(email,password);
                    }
                }else{
                    Toast.makeText(LoginActivity.this, "Username not found", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                return;
            }
        });
    }

    private void signInWithEmailAndPassword(String email, String password){
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            final DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(task.getResult().getUser().getUid());
                            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot userdata) {
                                    String email = userdata.child("email").getValue().toString();
                                    String display_name = userdata.child("display_name").getValue().toString();
                                    String bio = userdata.child("bio").getValue().toString();
                                    String imgthumb = userdata.child("image_thumb").getValue().toString();
                                    String img = userdata.child("image").getValue().toString();
                                    saveinSharedPref(email,display_name,bio,imgthumb,img);

                                    showLoading(false);
                                    //OPEN MAIN UI
                                    startActivity(new Intent(LoginActivity.this,MainActivity.class));
                                    finish();
                                }
                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    showLoading(false);
                                    Toast.makeText(LoginActivity.this, "Failed to get user's info", Toast.LENGTH_LONG).show();
                                }
                            });
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.d(TAG, "signInWithEmail:failed");
                            showLoading(false);
                            Toast.makeText(LoginActivity.this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
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

    private void showLoading(boolean show){
        if(show) progressBar.setVisibility(View.VISIBLE);
        else progressBar.setVisibility(View.INVISIBLE);

        btnSignIn.setEnabled(!show);
    }

    private void showFogotPassDialog(){

        LayoutInflater inflater = getLayoutInflater();
        View forgotPassDialog =  inflater.inflate(R.layout.dialog_input, null);

        final EditText txtEmail = forgotPassDialog.findViewById(R.id.dialog_txtInput);
        txtEmail.setHint("Type your email here");
        TextView txtDone = forgotPassDialog.findViewById(R.id.dialog_btnDone);
        TextView txtCancel = forgotPassDialog.findViewById(R.id.dialog_btnCancel);

        txtDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(txtEmail.getText().toString().contains("@")){
                    requestPassReset(txtEmail.getText().toString());
                }else{
                    Toast.makeText(LoginActivity.this, "Email is not valid", Toast.LENGTH_SHORT).show();
                }
            }
        });

        txtCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");
        builder.setView(forgotPassDialog);
        // create alert dialog
        alertDialog = builder.create();
        alertDialog.show();
    }

    private void requestPassReset(String email){
        alertDialog.setCancelable(false);
        Toast.makeText(this,"Please Wait",Toast.LENGTH_LONG).show();

        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                alertDialog.setCancelable(true);
                if(task.isSuccessful()){
                    alertDialog.dismiss();
                    new AlertDialog.Builder(LoginActivity.this).setTitle("Password Reset").setMessage("We have sent you a password reset link to your email").show();

                }else{
                    Toast.makeText(LoginActivity.this,getErrorMessage(task.getException().getMessage().toString()) , Toast.LENGTH_SHORT).show();
                }
            }
        });
    }



    private String getErrorMessage(String err){

        Log.d(TAG,"GetErrorMessage: "+err);

        if(err.contains("ERROR_EMAIL_ALREADY_IN_USE")){
            return "Email is already in use";
        }
        if(err.contains("ERROR_WEAK_PASSWORD")){
            return "Password too weak";
        }
        if(err.contains("ERROR_INVALID_EMAIL")){
            return "Invalid email format";
        }
        if(err.contains("INVALID_EMAIL")){
            return "Invalid email format";
        }
        if(err.contains("ERROR_USER_DISABLED")){
            return "Your account is currently disabled";
        }
        if(err.contains("EMAIL_NOT_FOUND")){
            return "Email not registered";
        }
        if(err.contains("ERROR_USER_NOT_FOUND")){
            return "User not found";
        }
        if(err.contains("USER_NOT_FOUND")){
            return "User not found";
        }
        if(err.contains("ERROR_WRONG_PASSWORD")){
            return "Incorrect Password";
        }
        return "Unknown Error";
    }
}
