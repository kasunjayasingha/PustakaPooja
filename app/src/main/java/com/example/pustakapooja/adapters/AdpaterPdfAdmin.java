package com.example.pustakapooja.adapters;

import static com.example.pustakapooja.Constants.MAX_BYTES_PDF;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pustakapooja.MyApplication;
import com.example.pustakapooja.admin.PdfEditActivity;
import com.example.pustakapooja.databinding.RowPdfAdminBinding;
import com.example.pustakapooja.filters.FilterPdfAdmin;
import com.example.pustakapooja.models.ModelPdf;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class AdpaterPdfAdmin extends RecyclerView.Adapter<AdpaterPdfAdmin.HolderPdfAdmin> implements Filterable {
    //context
    private Context context;
    //arrayList to hold list of data of type ModlePdf
    public ArrayList<ModelPdf> pdfArrayList, filterList;

    //view binding row_pdf_admin.xml
    private RowPdfAdminBinding binding;

    private FilterPdfAdmin filter;

    private static final String TAG = "PDF_ADAPTER_TAG";

    private ProgressDialog progressDialog;

    public AdpaterPdfAdmin(Context context, ArrayList<ModelPdf> pdfArrayList) {
        this.context = context;
        this.pdfArrayList = pdfArrayList;
        this.filterList = pdfArrayList;

        //Setup Progress dialog
        progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("Please Wait");
        progressDialog.setCanceledOnTouchOutside(false);
    }

    @NonNull
    @Override
    public HolderPdfAdmin onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        binding = RowPdfAdminBinding.inflate(LayoutInflater.from(context), parent, false);

        return new HolderPdfAdmin(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull HolderPdfAdmin holder, int position) {

        //get data
        ModelPdf model = pdfArrayList.get(position);
        String title = model.getTitle();
        String description = model.getDescription();
        long timestamp = model.getTimestamp();

        //convert timestamp to dd/mm/yyyy
        String formattedDate = MyApplication.formatTimestamp(timestamp);

        //set data
        holder.titleTv.setText(title);
        holder.descriptionAdminTv.setText(description);
        holder.dateAdminTv.setText(formattedDate);

        //load further details like category, pdf from url, pdf size in separate functions.
        loadCategory(model, holder);
        loadPdfFromUrl(model, holder);
        loadPdfSize(model, holder);

        //handel click, show dialog with option 1) Edi, 2) Delete
        holder.moreAdminBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                moreOptionDialog(model, holder);
            }
        });
        

    }

    private void moreOptionDialog(ModelPdf model, HolderPdfAdmin holder) {
        String bookId = model.getId();
        String bookUrl = model.getUrl();
        String bookTitle = model.getTitle();
        //option to show in dialog
        String[] option = {"Edit", "Delete"};

        //Alert Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Choose Option")
                .setItems(option, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (i == 0){
                            //Edit click, open new activity to edit the book info
                            Intent intent = new Intent(context, PdfEditActivity.class);
                            intent.putExtra("bookId",bookId);
                            context.startActivity(intent);
                        }
                        else if(i ==1){

                            //Delete click
                            deleteBook(model, holder);
                        }
                    }
                })
                .show();
    }

    private void deleteBook(ModelPdf model, HolderPdfAdmin holder) {
        String bookId = model.getId();
        String bookUrl = model.getUrl();
        String bookTitle = model.getTitle();

        Log.d(TAG, "deleteBook: Deleteing...");
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


    private void loadPdfSize(ModelPdf model, HolderPdfAdmin holder) {
        //using url we can get file and its metadata from firebase storage

        String pdfurl = model.getUrl();

        StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfurl);
        ref.getMetadata()
                .addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                    @Override
                    public void onSuccess(StorageMetadata storageMetadata) {
                        //get size in bytes
                        double bytes = storageMetadata.getSizeBytes();
                        Log.d(TAG, "OnSuccess: "+model.getTitle() +" "+bytes);
                        //convert bytes to KB, MB
                        double kb = bytes/1024;
                        double mb = kb/1024;

                        if (mb >= 1){
                            holder.sizeAdminTv.setText(String.format("%.2f", mb)+" MB");
                        }
                        else if (kb >= 1){
                            holder.sizeAdminTv.setText(String.format("%.2f", kb)+" KB");
                        }
                        else {
                            holder.sizeAdminTv.setText(String.format("%.2f", bytes)+" bytes");
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

    private void loadPdfFromUrl(ModelPdf model, HolderPdfAdmin holder) {
        //using url we get file and its metadata from firebase storage
        String pdfUrl = model.getUrl();
        StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl);
        ref.getBytes(MAX_BYTES_PDF)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        Log.d(TAG, "OnSuccess: "+model.getTitle() +" successfully get the file");
                        //set to pdfview
                        holder.pdfView.fromBytes(bytes)
                                .pages(0) //show only first page
                                .spacing(5)
                                .swipeHorizontal(false)
                                .enableSwipe(false)
                                .onError(new OnErrorListener() {
                                    @Override
                                    public void onError(Throwable t) {
                                        holder.progressBarAdmin.setVisibility(View.INVISIBLE);
                                        Log.d(TAG, "OnError: "+t.getMessage());
                                    }
                                })
                                .onPageError(new OnPageErrorListener() {
                                    @Override
                                    public void onPageError(int page, Throwable t) {
                                        holder.progressBarAdmin.setVisibility(View.INVISIBLE);
                                        Log.d(TAG, "OnPageError: "+t.getMessage());
                                    }
                                })
                                .onLoad(new OnLoadCompleteListener() {
                                    @Override
                                    public void loadComplete(int nbPages) {
                                        holder.progressBarAdmin.setVisibility(View.INVISIBLE);
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

    private void loadCategory(ModelPdf model, HolderPdfAdmin holder) {

        //get category using categoryId
        String categoryId = model.getCategoryId();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("categories");
        ref.child(categoryId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get values
                        String category = ""+snapshot.child("category").getValue();

                        //set to category text view
                        holder.categoryAdminTv.setText(category);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    @Override
    public int getItemCount() {
        return pdfArrayList.size(); //return number of records | List size
    }

    @Override
    public Filter getFilter() {
        if (filter == null){
            filter = new FilterPdfAdmin(filterList, this);
        }
        return filter;
    }

    //view holder class for row_pdf_admin.xml
    class HolderPdfAdmin extends RecyclerView.ViewHolder{

        //UI View of row_pdf_admin.xml
        PDFView pdfView;
        ProgressBar progressBarAdmin;
        TextView titleTv, descriptionAdminTv, categoryAdminTv, sizeAdminTv, dateAdminTv;

        ImageButton moreAdminBtn;
        public HolderPdfAdmin(@NonNull View itemView) {
            super(itemView);

            //init ui view
            pdfView = binding.pdfView;
            progressBarAdmin = binding.progressBarAdmin;
            titleTv = binding.titleTv;
            descriptionAdminTv = binding.descriptionAdminTv;
            categoryAdminTv = binding.categoryAdminTv;
            sizeAdminTv = binding.sizeAdminTv;
            dateAdminTv = binding.dateAdminTv;
            moreAdminBtn = binding.moreAdminBtn;

        }

    }
}
