package com.example.pustakapooja.adapters;

import static com.example.pustakapooja.Constants.MAX_BYTES_PDF;

import android.app.AlertDialog;
import android.app.Application;
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
import com.example.pustakapooja.admin.PdfReadAdminActivity;
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
    String pdfId;

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

        if (model != null) {
            pdfId = model.getId();
            String categoryId = model.getCategoryId();
            String pdfUrl = model.getUrl();
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
            MyApplication.loadCategory(""+categoryId, holder.categoryAdminTv);
            MyApplication.loadPdfFromUrlSinglePage(""+pdfUrl, ""+title, holder.pdfView, holder.progressBarAdmin);
            MyApplication.loadPdfSize(""+pdfUrl, ""+title, holder.sizeAdminTv);



            // Rest of the code...
        } else {
            // Handle the case when the modelPdf object is null
            // You can log an error message or perform any necessary action
        }

        //handel click, show dialog with option 1) Edi, 2) Delete
        holder.moreAdminBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                moreOptionDialog(model, holder);
            }
        });

        //handle book/pdf click, open pdf details page, pass pdf/book id to get details of it
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //open pdf details activity
                Intent intent = new Intent(context, PdfReadAdminActivity.class);
                intent.putExtra("bookId", pdfId);
                context.startActivity(intent);
            }
        });
        

    }

    private void moreOptionDialog(ModelPdf model, HolderPdfAdmin holder) {
        final String bookId = model.getId();
        final String bookUrl = model.getUrl();
        final String bookTitle = model.getTitle();
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
                            MyApplication.deleteBook(context, ""+bookId, ""+bookUrl, ""+bookTitle);
                            // deleteBook(model, holder);
                        }
                    }
                })
                .show();
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
