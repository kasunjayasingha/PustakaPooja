package com.example.pustakapooja.admin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import com.example.pustakapooja.adapters.AdapterCategory;
import com.example.pustakapooja.adapters.AdpaterPdfAdmin;
import com.example.pustakapooja.databinding.ActivityCategoryBinding;
import com.example.pustakapooja.models.ModelCategory;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class CategoryActivity extends AppCompatActivity {

    private ActivityCategoryBinding binding;
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;

    //arrayList to store category
    private ArrayList<ModelCategory> categoryArrayList;
    //adpter
    private AdapterCategory adapterCategory;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCategoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        loadCategory();

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait");
        progressDialog.setCanceledOnTouchOutside(false);

        //search
        binding.CategoryEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //Called as and when user type each letter
                try {
                    adapterCategory.getFilter().filter(charSequence);
                }
                catch (Exception e){

                }

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        binding.addCategoryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateData();
            }
        });
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(CategoryActivity.this, DashboardAdminActivity.class));
                finish();
            }
        });
    }

    private void loadCategory() {
        //init arrayList
        categoryArrayList = new ArrayList<>();
        //get all categories from firebaase > categories
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("categories");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //clear arraylist before adding data into it
                categoryArrayList.clear();
                for(DataSnapshot ds: snapshot.getChildren()){
                    //get data
                    ModelCategory model = ds.getValue(ModelCategory.class);
                    //add to arrayList
                    categoryArrayList.add(model);
                }
                //setup adapter
                adapterCategory = new AdapterCategory(CategoryActivity.this, categoryArrayList);
                //set adapter to recyclerview
                binding.categariesRv.setAdapter(adapterCategory);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private String category = "";
    private void validateData() {
        category = binding.CategoryEt.getText().toString().toLowerCase().trim();
        if (TextUtils.isEmpty(category)){
            Toast.makeText(this, "Please enter category..!", Toast.LENGTH_SHORT).show();
        }else {
            addCategoryFirebase();
        }
    }

    private void addCategoryFirebase() {
        progressDialog.setMessage("Adding Category...");
        progressDialog.show();

        long timestamp = System.currentTimeMillis();

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("id", ""+timestamp);
        hashMap.put("category", ""+category);
        hashMap.put("timestamp", timestamp);
        hashMap.put("uid", ""+firebaseAuth.getUid());

        //add to firebase db... Database Root > Categories > category info
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("categories");
        ref.orderByChild("category").equalTo(category).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    progressDialog.dismiss();
                    Toast.makeText(CategoryActivity.this, "Category already exists in database", Toast.LENGTH_SHORT).show();
                } else {
                    ref.child(""+timestamp)
                            .setValue(hashMap)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    progressDialog.dismiss();
                                    Toast.makeText(CategoryActivity.this, "Category Added Successfully...", Toast.LENGTH_SHORT).show();
                                    binding.CategoryEt.setText("");

                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    progressDialog.dismiss();
                                    Toast.makeText(CategoryActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }
}