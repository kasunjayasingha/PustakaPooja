package com.example.pustakapooja.admin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.pustakapooja.R;
import com.example.pustakapooja.databinding.ActivityPdfEditBinding;
import com.example.pustakapooja.login_register.LoginActivity;
import com.example.pustakapooja.login_register.Register;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Text;


public class PdfEditActivity extends AppCompatActivity {

    private ActivityPdfEditBinding binding;

    //book id get from intent started from AdapterPdfAdmin
    private String bookId;

    //progress dialog
    private ProgressDialog progressDialog;

    private ArrayList<String> categoryTitleArrayList, categoryIdArrayList;

    private static final String TAG = "BOOK_EDIT_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        bookId = getIntent().getStringExtra("bookId");

        //Setup Progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait");
        progressDialog.setCanceledOnTouchOutside(false);
        
        loadCategories();
        loadBookInfo();

        //handle click, pick category
        binding.CategoryTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                categoryDialog();
            }
        });

        //handle click, go to previous screen
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(PdfEditActivity.this, CategoryActivity.class));
                finish();
            }
        });

        //handle click, begin update
        binding.updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateData();
                
            }
        });
        
        

    }

    private String title = "", description = "";
    private void validateData() {
        //get data
        title = binding.addBookEt.getText().toString().trim();
        description = binding.descriptionEt.getText().toString().trim();

        //validate data
        if (TextUtils.isEmpty(title)){
            Toast.makeText(this, "Enter Title...", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(description)){
            Toast.makeText(this, "Enter Description...", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(selectedCategoryId)){
            Toast.makeText(this, "Pick Category...", Toast.LENGTH_SHORT).show();
        }
        else {
            updatePdf();
        }

    }

    private void updatePdf() {
        Log.d(TAG, "updatePdf: Updating pdf info to db...");

        //show progress
        progressDialog.setMessage("Updating book info...");
        progressDialog.show();

        //setup data to update to db
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("title", ""+title);
        hashMap.put("description", ""+description);
        hashMap.put("categoryId", ""+selectedCategoryId);

        //start update
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId)
                .updateChildren(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {

                        Log.d(TAG, "onSuccess: Book info updated to db...");
                        //update success
                        progressDialog.dismiss();
                        Toast.makeText(PdfEditActivity.this, "Book info Updated...", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(PdfEditActivity.this, CategoryActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {

                        Log.d(TAG, "onFailure: failed ti update due to "+e.getMessage());
                        //update failed
                        progressDialog.dismiss();
                        Toast.makeText(PdfEditActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
        
    }

    private void loadBookInfo() {
        Log.d(TAG, "loadBookInfo: Loading book info...");

        DatabaseReference refBooks = FirebaseDatabase.getInstance().getReference("Books");
        refBooks.child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get book info
                        selectedCategoryId = ""+snapshot.child("categoryId").getValue();
                        String description = ""+snapshot.child("description").getValue();
                        String title = ""+snapshot.child("title").getValue();

                        binding.addBookEt.setText(title);
                        binding.descriptionEt.setText(description);

                        Log.d(TAG, "onDataChange: Loading Category Info");
                        DatabaseReference refBookCategory = FirebaseDatabase.getInstance().getReference("categories");
                        refBookCategory.child(selectedCategoryId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        String category = ""+snapshot.child("category").getValue();
                                        binding.CategoryTv.setText(category);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                        

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
        
    }

    private  String selectedCategoryId = "", selectedCategoryTitle = "";

    private void categoryDialog(){
        //make string array from arraylist of string
        String[] categoriesArray = new String[categoryTitleArrayList.size()];
        for (int i=0; i<categoryTitleArrayList.size(); i++){
            categoriesArray[i] = categoryTitleArrayList.get(i);
        }

        //AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Category")
                .setItems(categoriesArray, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //get selected item
                        selectedCategoryId = categoryIdArrayList.get(i);
                        selectedCategoryTitle = categoryTitleArrayList.get(i);

                        //set selected item
                        binding.CategoryTv.setText(selectedCategoryTitle);
                    }
                })
                .show();
    }
    private void loadCategories() {
        Log.d(TAG, "loadCategories: Loading categories...");

        categoryIdArrayList = new ArrayList<>();
        categoryTitleArrayList = new ArrayList<>();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("categories");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryIdArrayList.clear();
                categoryTitleArrayList.clear();

                for (DataSnapshot ds: snapshot.getChildren()){
                    String id = ""+ds.child("id").getValue();
                    String category = ""+ds.child("category").getValue();

                    categoryIdArrayList.add(id);
                    categoryTitleArrayList.add(category);

                    Log.d(TAG, "onDataChange: ID: "+id);
                    Log.d(TAG, "onDataChange: Category: "+category);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}