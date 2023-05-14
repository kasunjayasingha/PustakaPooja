package com.example.pustakapooja;

import static android.content.ContentValues.TAG;
import static com.example.pustakapooja.Constants.MAX_BYTES_PDF;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.pustakapooja.adapters.AdpaterPdfAdmin;
import com.example.pustakapooja.databinding.ActivityPdfReadUserBinding;
import com.example.pustakapooja.models.ModelPdf;
import com.example.pustakapooja.user.PdfReadUserActivity;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

//application class runs before launcher activity
public class MyApplication extends Application {
    private static ActivityPdfReadUserBinding binding;
    @Override
    public void onCreate() {
        super.onCreate();
    }

    //convert timestamp to proper date format
    public static final String formatTimestamp(long timestamp){
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(timestamp);
        //format timestamp to dd/mm/yyyy
        String date = DateFormat.format("dd/MM/yyyy", cal).toString();
        return date;
    }

    public static void deleteBook(Context context, String bookId, String bookUrl, String bookTitle) {

        String TAG = "DELETE_BOOK_TAG";

        Log.d(TAG, "deleteBook: Deleteing...");
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("Please Wait");
        progressDialog.setMessage("Deleting "+bookTitle+" ...");
        progressDialog.show();

        Log.d(TAG, "deleteBook: Deleteing from storage");
        StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl);
        storageReference.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: Deleted from storage");

                        Log.d(TAG, "onSuccess: now deleting info from db");
                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Books");
                        reference.child(bookId)
                                .removeValue()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Log.d(TAG, "onSuccess: Deleted from db too");
                                        progressDialog.dismiss();
                                        Toast.makeText(context, "Book Deleted Successfully...", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {

                                        Log.d(TAG, "onFailure: Failed to delete from db due to "+e.getMessage());
                                        progressDialog.dismiss();
                                        Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: Failed to delete from storage due to " + e.getMessage());
                        progressDialog.dismiss();
                        Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    //load pdf size
    public static void loadPdfSize(String pdfUrl, String pdfTitle, TextView sizeAdminTv) {
        String TAG = "PDF_SIZE_TAG";
        //using url we can get file and its metadata from firebase storage
        StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl);
        ref.getMetadata()
                .addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                    @Override
                    public void onSuccess(StorageMetadata storageMetadata) {
                        //get size in bytes
                        double bytes = storageMetadata.getSizeBytes();
                        Log.d(TAG, "OnSuccess: "+pdfTitle +" "+bytes);
                        //convert bytes to KB, MB
                        double kb = bytes/1024;
                        double mb = kb/1024;

                        if (mb >= 1){
                            sizeAdminTv.setText(String.format("%.2f", mb)+" MB");
                        }
                        else if (kb >= 1){
                            sizeAdminTv.setText(String.format("%.2f", kb)+" KB");
                        }
                        else {
                            sizeAdminTv.setText(String.format("%.2f", bytes)+" bytes");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed getting metadata
                        Log.d(TAG, "OnFailure: "+e.getMessage());
                    }
                });
    }

    //pdf load
    public static void loadPdfFromUrlSinglePage(String pdfUrl, String pdfTitle, PDFView pdfView, ProgressBar progressBarAdmin) {
        String TAG = "PDF_LOAD_SINGLE_TAG";
        //using url we get file and its metadata from firebase storage

        StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl);
        ref.getBytes(MAX_BYTES_PDF)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        Log.d(TAG, "OnSuccess: "+pdfTitle +" successfully get the file");
                        //set to pdfview
                        pdfView.fromBytes(bytes)
                                .pages(0) //show only first page
                                .spacing(5)
                                .swipeHorizontal(false)
                                .enableSwipe(false)
                                .onError(new OnErrorListener() {
                                    @Override
                                    public void onError(Throwable t) {
                                        progressBarAdmin.setVisibility(View.INVISIBLE);
                                        Log.d(TAG, "OnError: "+t.getMessage());
                                    }
                                })
                                .onPageError(new OnPageErrorListener() {
                                    @Override
                                    public void onPageError(int page, Throwable t) {
                                        progressBarAdmin.setVisibility(View.INVISIBLE);
                                        Log.d(TAG, "OnPageError: "+t.getMessage());
                                    }
                                })
                                .onLoad(new OnLoadCompleteListener() {
                                    @Override
                                    public void loadComplete(int nbPages) {
                                        progressBarAdmin.setVisibility(View.INVISIBLE);
                                        Log.d(TAG, "LoadComplete: pdf loaded");
                                    }
                                })
                                .load();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "OnFailure: failed getting file from url due to "+e.getMessage());


                    }
                });
    }

    //Load category
    public static void loadCategory(String categoryId, TextView categoryAdminTv) {

        //get category using categoryId

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("categories");
        ref.child(categoryId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get values
                        String category = ""+snapshot.child("category").getValue();

                        //set to category text view
                        categoryAdminTv.setText(category);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    public static void incrementBookViewCount(String bookId){
        //1) get book views count
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String viewsCount = ""+snapshot.child("viewsCount").getValue();
                        //in case views count was null replace it with 0
                        if (viewsCount.equals("null") || viewsCount.isEmpty()){
                            viewsCount = "0";
                        }

                        //2) increment views count
                        long newViewsCount = Long.parseLong(viewsCount) + 1;
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("viewsCount", +newViewsCount);

                        //3) update views count
                        ref.child(bookId)
                                .updateChildren(hashMap)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        //views count updated
                                        Log.d(TAG, "onSuccess: views count updated");
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        //failed updating views count
                                        Log.d(TAG, "onFailure: failed updating views count due to "+e.getMessage());
                                    }
                                });

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    public static void loadPdfPageCount(PDFView pdfView, String pdfUrl, TextView pagesCountTv){
        //load pdf file from firebase storage using url
        StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl);
        ref.getBytes(MAX_BYTES_PDF)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onSuccess(byte[] bytes) {
                        //file received

                        //load pdf pages using pdfView Library
                        pdfView.fromBytes(bytes).load();
                        int pageCount = pdfView.getPageCount();
                        pagesCountTv.setText(""+pageCount);



                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull  Exception e) {
                        //failed receive pdf file

                    }
                });
    }

    public static void addToFavorite(Context context, String bookId, boolean favouriteStatus){
        // we can add only if user is logged in
        //1) check if user is logged in
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        if(firebaseAuth.getCurrentUser() == null){
            //user not logged in
            Toast.makeText(context, "Please login to add to favorite", Toast.LENGTH_SHORT).show();

        }
        else{
            long timestamp = System.currentTimeMillis();

            //setup data to add in firebase database of current user for favorite book
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("bookId", ""+bookId);
            hashMap.put("timestamp", ""+timestamp);
            hashMap.put("favourite_status", ""+favouriteStatus);

            //save to db
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");
            ref.child(Objects.requireNonNull(firebaseAuth.getUid())).child("Favorites").child(bookId)
                    .setValue(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            //added to favorite
                            Toast.makeText(context, "Added to favorite", Toast.LENGTH_SHORT).show();
                            PdfReadUserActivity.enableFavourite();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull  Exception e) {
                            //failed adding to favorite
                            Toast.makeText(context, "Failed to add to favorite due to "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }

    }

    public static void removeFromFavorite(Context context, String bookId){
        // we can remove it if user is logged in
        //1) check if user is logged in
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        if(firebaseAuth.getCurrentUser() == null){
            //user not logged in
            Toast.makeText(context, "You need to login in", Toast.LENGTH_SHORT).show();

        }
        else{


            //remove from db
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");
            ref.child(Objects.requireNonNull(firebaseAuth.getUid())).child("Favorites").child(bookId)
                    .removeValue()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            //added to favorite
                            Toast.makeText(context, "Remove from your favorite List", Toast.LENGTH_SHORT).show();
                            PdfReadUserActivity.disableFavourite();

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull  Exception e) {
                            //failed adding to favorite
                            Toast.makeText(context, "Failed to remove from favorite list due to "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }

    }

}
