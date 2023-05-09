package com.example.pustakapooja.filters;

import android.widget.Filter;

import com.example.pustakapooja.adapters.AdapterCategory;
import com.example.pustakapooja.adapters.AdpaterPdfAdmin;
import com.example.pustakapooja.models.ModelCategory;
import com.example.pustakapooja.models.ModelPdf;

import java.util.ArrayList;

public class FilterPdfAdmin extends Filter {
    //ArrayList in which we want to search
    ArrayList<ModelPdf> filterList;
    //adpter in which filter need to be implemented
    AdpaterPdfAdmin adpaterPdfAdmin;

    //constructor
    public FilterPdfAdmin(ArrayList<ModelPdf> filterList, AdpaterPdfAdmin adapterPdfAdmin) {
        this.filterList = filterList;
        this.adpaterPdfAdmin = adapterPdfAdmin;
    }

    @Override
    protected FilterResults performFiltering(CharSequence charSequence) {
        FilterResults results = new FilterResults();
        //value should not be null and empty
        if (charSequence != null && charSequence.length() > 0){
            //change to uppercase or lowercase to avoid case sensitivity
            charSequence = charSequence.toString().toLowerCase();
            ArrayList<ModelPdf> filteredModels = new ArrayList<>();

            for (int i = 0; i <filterList.size(); i++){
                //Validate
                if (filterList.get(i).getTitle().toLowerCase().contains(charSequence)){
                    //add to filtered list
                    filteredModels.add(filterList.get(i));
                }
            }

            results.count = filteredModels.size();
            results.values = filteredModels;
        }else {
            results.count = filterList.size();
            results.values = filterList;
        }
        return results;
    }

    @Override
    protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
        //apply filter changes
        adpaterPdfAdmin.pdfArrayList = (ArrayList<ModelPdf>)filterResults.values;

        //notify changes
        adpaterPdfAdmin.notifyDataSetChanged();
    }
}
