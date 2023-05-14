package com.example.pustakapooja.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pustakapooja.MyApplication;
import com.example.pustakapooja.admin.PdfReadAdminActivity;
import com.example.pustakapooja.databinding.RowPdfUserBinding;
import com.example.pustakapooja.filters.FillterPdfUser;
import com.example.pustakapooja.models.ModelPdf;
import com.example.pustakapooja.user.PdfReadUserActivity;
import com.github.barteksc.pdfviewer.PDFView;

import java.util.ArrayList;

import org.w3c.dom.Text;

public class AdapterPdfUser extends RecyclerView.Adapter<AdapterPdfUser.HolderPdfUser> implements Filterable {

    private Context context;
    public ArrayList<ModelPdf> pdfArrayList, filterList;
    private FillterPdfUser filter;

    private RowPdfUserBinding binding;

    private static final String TAG = "ADAPTER_PDF_USER_TAG";

    public AdapterPdfUser(Context context, ArrayList<ModelPdf> pdfArrayList) {
        this.context = context;
        this.pdfArrayList = pdfArrayList;
        this.filterList = pdfArrayList;
    }

    @NonNull
    @Override
    public HolderPdfUser onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //bind the row_pdf_user.xml layout
        binding = RowPdfUserBinding.inflate(LayoutInflater.from(context), parent, false);
        return new HolderPdfUser(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull HolderPdfUser holder, int position) {

        //Get data, set data, handle view clicks in this method

        //get data
        ModelPdf modelPdf = pdfArrayList.get(position);
        String bookId = modelPdf.getId();
        String title = modelPdf.getTitle();
        String description = modelPdf.getDescription();
        String pdfUrl = modelPdf.getUrl();
        String categoryId = modelPdf.getCategoryId();
        long timestamp = modelPdf.getTimestamp();

        //Convert timestamp to dd/mm/yyyy hh:mm am/pm
//        String date = MyApplication.formatTimestamp(timestamp);

        //set data
        holder.titleTv.setText(title);
        holder.descriptionUserTv.setText(description);
//        holder.dateUserTv.setText(date);

        MyApplication.loadPdfFromUrlSinglePage(""+pdfUrl, ""+title, holder.pdfView, holder.progressBarUser);
        MyApplication.loadCategory(categoryId, holder.categoryUserTv);
        MyApplication.loadPdfSize(""+pdfUrl, ""+title, holder.sizeUserTv);
        MyApplication.loadPdfPageCount(holder.pdfView, ""+pdfUrl, binding.pagesCountTv);


//        handle click show pdf details activity
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, PdfReadUserActivity.class);
                intent.putExtra("bookId", bookId);
                context.startActivity(intent);
                


            }
        });
        


    }

    @Override
    public int getItemCount() {
        return pdfArrayList.size(); // return number of records i.e. size of list
    }

    @Override
    public Filter getFilter() {
        if (filter == null){
            filter = new FillterPdfUser(filterList, this);
        }
        return filter;
    }

    class HolderPdfUser extends RecyclerView.ViewHolder {

        TextView titleTv, descriptionUserTv, dateUserTv, categoryUserTv, sizeUserTv;
        PDFView pdfView;
        ProgressBar progressBarUser;

        public HolderPdfUser(@NonNull View itemView) {
            super(itemView);

            //init ui views
            titleTv = binding.titleTv;
            descriptionUserTv = binding.descriptionUserTv;
//            dateUserTv = binding.dateUserTv;
            categoryUserTv = binding.categoryUserTv;
            sizeUserTv = binding.sizeUserTv;
            pdfView = binding.pdfView;
            progressBarUser = binding.progressBarUser;

        }
    }
}
