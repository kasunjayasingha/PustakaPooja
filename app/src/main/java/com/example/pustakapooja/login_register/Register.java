package com.example.pustakapooja.login_register;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pustakapooja.databinding.ActivityRegisterBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class Register extends AppCompatActivity {



    private ActivityRegisterBinding binding;
    //Firebase auth
    private FirebaseAuth firebaseAuth;

    //progress dialog
    private ProgressDialog progressDialog;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());



        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance();

        //Setup Progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait");
        progressDialog.setCanceledOnTouchOutside(false);


        //back Button
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
//
        //handle click, begin register
        binding.registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateData();
            }
        });
    }

    private String name = "";
    private String email = "";
    private String password = "";

    private void validateData() {
        //get data
        name = binding.nameEdit.getText().toString().trim();
        email = binding.emailet.getText().toString().trim();
        password = binding.passwordet.getText().toString().trim();
        String conpassword = binding.conpasswordet.getText().toString().trim();

        //Validate Data
        if (TextUtils.isEmpty(name)){
            Toast.makeText(this, "Enter your Name..!", Toast.LENGTH_SHORT).show();
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid Email Pattern..!", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Enter Password..!", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(conpassword)) {
            Toast.makeText(this, "Enter Confirm Password..!", Toast.LENGTH_SHORT).show();
        } else if (!password.equals(conpassword)) {
            Toast.makeText(this, "Password doesn't match..!", Toast.LENGTH_SHORT).show();
        }
        else {
            createUserAccount();
        }


    }

    private void createUserAccount() {
        //show progress
        progressDialog.setMessage("Creating Account...");
        progressDialog.show();

        //Create user in firebase
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        Toast.makeText(getApplicationContext(), "WEDa", Toast.LENGTH_SHORT).show();
                        //account create success now add in firebase reltime database
                        updatUserInfo();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //account create failure
                        progressDialog.dismiss();
                        Toast.makeText(Register.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });

    }

    private void updatUserInfo() {
        progressDialog.setMessage("Saving user info...");

        //timeStamp
        long timestamp = System.currentTimeMillis();

        //get current user id, since user is registered so we get now
        String uid = firebaseAuth.getUid();

        //setup data to ass in db
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("uid", uid);
        hashMap.put("email", email);
        hashMap.put("name", name);
        hashMap.put("profileImage", "");
        hashMap.put("userType", "user");//possible values are user, admin: will make admin manually in firebase realtime database
        hashMap.put("timestamp", timestamp);

        //set data to db
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");
        ref.child(uid)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //data added to db
                        progressDialog.dismiss();
                        Toast.makeText(Register.this, "Account Created Successfully..", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(Register.this, LoginActivity.class));
                        finish();


                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //data failed adding to db
                        progressDialog.dismiss();
                        Toast.makeText(Register.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });


    }
}