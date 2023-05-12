package com.example.pustakapooja.admin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.pustakapooja.Constants;
import com.example.pustakapooja.MyApplication;
import com.example.pustakapooja.R;
import com.example.pustakapooja.databinding.ActivityPdfReadAdminBinding;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class PdfReadAdminActivity extends AppCompatActivity {

    //view binding
    private ActivityPdfReadAdminBinding binding;

    String bookId;

    private static final String TAG = "PDF_VIEW_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfReadAdminBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        //get data from intent e.g. bookId
        Intent intent = getIntent();
        bookId = intent.getStringExtra("bookId");
        Log.d(TAG, "OnCreate: BookId: "+bookId);

        loadBookDetails();
        //increase book view count, whenever this page starts
        MyApplication.incrementBookViewCount(bookId);

        //Back
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(PdfReadAdminActivity.this, CategoryActivity.class));
                finish();
            }
        });
    }

    private void loadBookDetails() {
        Log.d(TAG, "loadBookDetails: Get pdf URL from db...");
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get data
                        String title = "" + snapshot.child("title").getValue();
                        String categoryId = "" + snapshot.child("categoryId").getValue();
                        String viewsCount = "" + snapshot.child("viewsCount").getValue();
                        String url = "" + snapshot.child("url").getValue();
                        Log.d(TAG, "onDataChange: PDF URL: "+url);
                        String timestamp = "" + snapshot.child("timestamp").getValue();

                        MyApplication.loadCategory( ""+categoryId, binding.categoryTv);

                        //set data
                        binding.bookTopicTv.setText(title);
                        binding.viewCountTv.setText(viewsCount.replace("null", "N/A"));

                        //Load pdf using that url from firebase storage
                        loadBookFromUrl(url);

                     
                        
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void loadBookFromUrl(String url) {
        Log.d(TAG, "loadBookFromUrl: Get pdf from Storage...");
        StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(url);
        ref.getBytes(Constants.MAX_BYTES_PDF)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        //load pdf using bytes
                        binding.pdfView.fromBytes(bytes)
                                .swipeHorizontal(false)// set false to scroll vertical, set to swipe horizontal
                                .onError(new OnErrorListener() {
                                    @Override
                                    public void onError(Throwable t) {
                                        Log.d(TAG, "OnError: "+t.getMessage());
                                        //show error message
                                        Toast.makeText(PdfReadAdminActivity.this, ""+t.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .onPageError(new OnPageErrorListener() {
                                    @Override
                                    public void onPageError(int page, Throwable t) {
                                        Log.d(TAG, "OnPageError: "+t.getMessage());
                                        //show error message
                                        Toast.makeText(PdfReadAdminActivity.this, ""+t.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .onPageChange(new OnPageChangeListener() {
                                    @Override
                                    public void onPageChanged(int page, int pageCount) {
                                        //set current and total pages in toolbar
                                        binding.pageNo.setText(page + 1 + "|" + pageCount);
                                        Log.d(TAG, "onPageChanged: "+ page + 1 + "|" + pageCount);



                                    }
                                })
                                .load();
                        //hide progress
                        binding.progressBar.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "OnFailure: "+e.getMessage());
                        //hide progress
                        binding.progressBar.setVisibility(View.GONE);
                        //show error message
                        Toast.makeText(PdfReadAdminActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }
}