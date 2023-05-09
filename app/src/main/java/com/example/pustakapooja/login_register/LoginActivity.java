package com.example.pustakapooja.login_register;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.example.pustakapooja.admin.DashboardAdminActivity;
import com.example.pustakapooja.databinding.ActivityLoginBinding;
import com.example.pustakapooja.user.DashboardUserActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        //Setup Progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait");
        progressDialog.setCanceledOnTouchOutside(false);

        //Register Screen button
        binding.registertxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this, Register.class));
            }
        });

        //Start Login
        binding.loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateData();
            }
        });
    }
    private String email = "", password = "";
    private void validateData() {
        email = binding.emailet.getText().toString().trim();
        password = binding.passwordet.getText().toString().trim();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid Email..!", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Enter Password..!", Toast.LENGTH_SHORT).show();
        }else {
            loginUser();
        }
    }

    private void loginUser() {
        progressDialog.setMessage("Logging In..");
        progressDialog.show();

        //Login User
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        checkUser();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(LoginActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUser() {
        progressDialog.setMessage("Checking User...");
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        //Check in db
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");
        ref.child(firebaseUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        progressDialog.dismiss();
                        //get usertype
                        String userType = ""+snapshot.child("userType").getValue();
                        //check user Type
                        if(userType.equals("user")){
                            startActivity(new Intent(LoginActivity.this, DashboardUserActivity.class));
                            finish();
                        } else if (userType.equals("admin")) {
                            startActivity(new Intent(LoginActivity.this, DashboardAdminActivity.class));
                            finish();
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        progressDialog.dismiss();
                        Toast.makeText(LoginActivity.this, ""+error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }
}