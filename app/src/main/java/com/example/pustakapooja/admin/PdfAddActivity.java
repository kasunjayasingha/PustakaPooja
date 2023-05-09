package com.example.pustakapooja.admin;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pustakapooja.databinding.ActivityPdfAddBinding;
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

import java.util.ArrayList;
import java.util.HashMap;

public class PdfAddActivity extends AppCompatActivity {

    //setup view binding
    private ActivityPdfAddBinding binding;

    private FirebaseAuth firebaseAuth;

    //progress dialog
    private ProgressDialog progressDialog;

    private Uri pdfUri;

    private static final int PDF_PICK_CODE = 1000;

    //TAG for debugging
    private static final String TAG = "ADD_PDF_TAG";

    //arrayList to hold pdf categories
   private ArrayList<String> categoryTitleArrayList, categoryIdArrayList;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfAddBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        firebaseAuth = FirebaseAuth.getInstance();
        loadPdfCategories();

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait");
        progressDialog.setCanceledOnTouchOutside(false);

        //handle click, go to previous activity
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(PdfAddActivity.this, DashboardAdminActivity.class));
                finish();
            }
        });

        //attach pdf
        binding.attachBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pdfPickIntent();
            }
        });
        //pick categories
        binding.CategoryTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                categoryPickDialog();
            }
        });

        //upload pdf
        binding.uploaadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateData();
            }
        });
    }

    private String title = "", description = "";
    private void validateData() {
        //Step 1: Validate data
        Log.d(TAG, "validateData: validating data..");
        //get data
        title = binding.addBookEt.getText().toString().trim();
        description = binding.descriptionEt.getText().toString().trim();


        //Validate data
        if (TextUtils.isEmpty(title)){
            Toast.makeText(this, "Enter Title...", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(description)) {
            Toast.makeText(this, "Enter Description...", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(selectedCategoryTitle)) {
            Toast.makeText(this, "Select Category...", Toast.LENGTH_SHORT).show();
        } else if (pdfUri == null) {
            Toast.makeText(this, "Pick pdf...", Toast.LENGTH_SHORT).show();
        }
        else {
            //uploading
            uploadPdfToStorage();
        }
    }

    private void uploadPdfToStorage() {
        //Step 2:Upload pdf to firebase storage
        Log.d(TAG, "uploadpdfToStorage: uploading to storage...");

        //show progress
        progressDialog.setMessage("Uploading pdf...");
        progressDialog.show();

        //timestamp
        long timestamp = System.currentTimeMillis();

        //path of pdf in firebase storage
        String filePathAndName = "Book/" + timestamp;
        //storage reference
        StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathAndName);
        storageReference.putFile(pdfUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.d(TAG, "onSuccess: PDF upoaded to storage...");
                        Log.d(TAG, "onSuccess: getting pdf url");

                        //get pdf url
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());
                        String uploadPdfUrl = ""+uriTask.getResult();

                        //upload to firebase db
                        uploadPdfInfoToDb(uploadPdfUrl, timestamp);
;                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Log.d(TAG, "onFailure: PDF upload failed due to"+e.getMessage());
                        Toast.makeText(PdfAddActivity.this, "PDF upload failed due to"+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void uploadPdfInfoToDb(String uploadPdfUrl, long timestamp) {
        //Step 3:Upload pdf info to firebase db
        Log.d(TAG, "uploadPdfToStorage: uploading to storage...");

        progressDialog.setMessage("Uploading pdf info...");
        String uid = firebaseAuth.getUid();

        //setup data to upload
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("uid", ""+uid);
        hashMap.put("id", ""+timestamp);
        hashMap.put("title", ""+title);
        hashMap.put("description", ""+description);
        hashMap.put("categoryId", ""+selectedCategoryId);
        hashMap.put("url", ""+uploadPdfUrl);
        hashMap.put("timestamp",+timestamp);

        //db reference: DB > Books
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(""+timestamp)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {

                        progressDialog.dismiss();
                        Log.d(TAG, "onSuccess: Successfully uploaded...");
                        Toast.makeText(PdfAddActivity.this, "Successfully uploaded", Toast.LENGTH_SHORT).show();
                        binding.addBookEt.setText("");
                        binding.descriptionEt.setText("");
                        binding.CategoryTv.setText("");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Log.d(TAG, "onFalilure: Failed to upload to db due to "+e.getMessage());
                        Toast.makeText(PdfAddActivity.this, "Failed to upload to db due to"+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void loadPdfCategories() {
        Log.d(TAG, "LoadPdfCategories: Loading pdf categories...");
        categoryIdArrayList = new ArrayList<>();
        categoryTitleArrayList = new ArrayList<>();

        //db reference to load categories... db
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("categories");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryTitleArrayList.clear();
                categoryIdArrayList.clear();
                for (DataSnapshot ds: snapshot.getChildren()){

                    //get id and title of category
                    String categoryId = ""+ds.child("id").getValue();
                    String categoryTitle = ""+ds.child("category").getValue();

                    //add to respective arrayLists
                    categoryTitleArrayList.add(categoryTitle);
                    categoryIdArrayList.add(categoryId);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

//    selected category id and category title
    private String selectedCategoryId, selectedCategoryTitle;
    private void categoryPickDialog() {
        Log.d(TAG, "categoryPickDialog: showing category pick dialog");

        //get string array of categories from arrayList
        String[] categoriesArray = new String[categoryTitleArrayList.size()];
        for (int i = 0; i < categoryTitleArrayList.size(); i++){
            categoriesArray[i] = categoryTitleArrayList.get(i);
        }
        //alter dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Category")
                .setItems(categoriesArray, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //handle item click
                        //get clicked item from list
                        selectedCategoryTitle = categoryTitleArrayList.get(i);
                        selectedCategoryId = categoryIdArrayList.get(i);
                        //set to category textview
                        binding.CategoryTv.setText(selectedCategoryTitle);

                        Log.d(TAG, "onClick: Selected Category: "+selectedCategoryId+" "+selectedCategoryTitle);
                    }
                })
                .show();
        
    }

    private void pdfPickIntent() {
        Log.d(TAG, "pdfPickIntent: starting pdf pick intent");

        Intent intent = new Intent();
        intent.setType("application/pdf");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select pdf"),PDF_PICK_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK){
            if (requestCode == PDF_PICK_CODE){
                Log.d(TAG, "onActivityResult: PDF Picked ");

                pdfUri = data.getData();
                Log.d(TAG, "onActivityResult: URI: "+pdfUri);
            }
        }else{
            Log.d(TAG, "onActivityResult: cancelled picking pdf");
            Toast.makeText(this, "cancelled picking pdf", Toast.LENGTH_SHORT).show();

        }
    }
}