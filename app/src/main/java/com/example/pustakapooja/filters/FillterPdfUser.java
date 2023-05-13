package com.example.pustakapooja.filters;

import android.widget.Filter;

import com.example.pustakapooja.adapters.AdapterPdfUser;
import com.example.pustakapooja.models.ModelPdf;

import java.util.ArrayList;

public class FillterPdfUser extends Filter {

    // arraylist in which we want to search
    ArrayList<ModelPdf> filterList;
    // adapter in which filter need to be implementd
    AdapterPdfUser adapterPdfUser;

    // constructor
    public FillterPdfUser(ArrayList<ModelPdf> filterList, AdapterPdfUser adapterPdfUser) {
        this.filterList = filterList;
        this.adapterPdfUser = adapterPdfUser;
    }

    @Override
    protected FilterResults performFiltering(CharSequence charSequence) {
        FilterResults results = new FilterResults();

        //Value to be searched should not be null/empty
        if (charSequence != null && charSequence.length() > 0) {
            //change to upper case, to make case insensitive
            charSequence = charSequence.toString().toUpperCase();
            //store our filtered list
            ArrayList<ModelPdf> filteredModels = new ArrayList<>();
            for (int i = 0; i < filterList.size(); i++) {
                //check, search by title
                if (filterList.get(i).getTitle().toUpperCase().contains(charSequence)) {
                    //add filtered data to list
                    filteredModels.add(filterList.get(i));
                }
            }
            results.count = filteredModels.size();
            results.values = filteredModels;
        } else {
            //when search bar is empty, return all data
            results.count = filterList.size();
            results.values = filterList;
        }
        return results;
    }

    @Override
    protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
        //apply filter changes
        adapterPdfUser.pdfArrayList = (ArrayList<ModelPdf>) filterResults.values;

        //notify changes
        adapterPdfUser.notifyDataSetChanged();
    }
}
