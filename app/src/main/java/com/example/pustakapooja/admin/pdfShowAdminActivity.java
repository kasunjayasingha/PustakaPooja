package com.example.pustakapooja.admin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;

import com.example.pustakapooja.R;
import com.example.pustakapooja.adapters.AdpaterPdfAdmin;
import com.example.pustakapooja.databinding.ActivityPdfShowAdminBinding;
import com.example.pustakapooja.models.ModelPdf;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class pdfShowAdminActivity extends AppCompatActivity {

    private ActivityPdfShowAdminBinding binding;
    //arrayList to hold list of data of type ModlePdf
    private ArrayList<ModelPdf> pdfArrayList;
    //adapter
    private AdpaterPdfAdmin adpaterPdfAdmin;

    private String categoryId, categoryTitle;

    private static final String TAG = "PDF_LIST_TAG";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfShowAdminBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //get data from intent
        Intent intent = getIntent();
        categoryId = intent.getStringExtra("categoryId");
        categoryTitle = intent.getStringExtra("categoryTitle");

        //set pdf category
        binding.subTitleTv.setText(categoryTitle);
        loadOdfList();

        //Search
        binding.bookSearchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //search as and when user type each letter
                try {
                    adpaterPdfAdmin.getFilter().filter(charSequence);
                }
                catch (Exception e){
                    Log.d(TAG, "onTextChanged: "+e.getMessage());
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        //handle click, go to previous activity
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(pdfShowAdminActivity.this, CategoryActivity.class));
                finish();
            }
        });
    }

    private void loadOdfList() {
        //init list before adding data
        pdfArrayList = new ArrayList<>();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.orderByChild("categoryId").equalTo(categoryId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        pdfArrayList.clear();
                        for (DataSnapshot ds: snapshot.getChildren()){
                            //get data
                            ModelPdf model = ds.getValue(ModelPdf.class);

                            //add to List
                            pdfArrayList.add(model);

                            Log.d(TAG, "OnDataChange: "+model.getId()+" "+model.getTitle());
                        }
                        //setup adapter
                        adpaterPdfAdmin = new AdpaterPdfAdmin(pdfShowAdminActivity.this, pdfArrayList);
                        binding.bookRv.setAdapter(adpaterPdfAdmin);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
}