package com.example.pustakapooja;

import static android.content.ContentValues.TAG;

import java.util.ArrayList;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.pustakapooja.adapters.AdapterPdfUser;
import com.example.pustakapooja.databinding.FragmentBookUserBinding;
import com.example.pustakapooja.models.ModelPdf;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BookUserFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BookUserFragment extends Fragment {

    //that er passed while creating instance of this fragment
    private String categoryId, category, uid;

    private ArrayList<ModelPdf> pdfArrayList;
    private AdapterPdfUser adapterPdfUser;

    //view binding
    private FragmentBookUserBinding binding;


    public BookUserFragment() {
        // Required empty public constructor
    }

    public static BookUserFragment newInstance(String categoryId, String category, String uid) {
        BookUserFragment fragment = new BookUserFragment();
        Bundle args = new Bundle();
        args.putString("categoryId", categoryId);
        args.putString("category", category);
        args.putString("uid", uid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categoryId = getArguments().getString("categoryId");
            category = getArguments().getString("category");
            uid = getArguments().getString("uid");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate/bind the layout for this fragment
        binding = FragmentBookUserBinding.inflate(LayoutInflater.from(getContext()), container, false);

        Log.d(TAG, "onCreateView: category: " + category);

        if(category.equals("All")){
            //load all books
            loadAllBooks();
        }
        else if(category.equals("Most Viewed")){
            //load most viewed books
            loadMostViewedBooks("viewsCount");
        }
        else{
            //load selected category books
            loadBooksByCategory();
        }

        //Search
        binding.searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //called as anf when user type any letter
                try{
                    adapterPdfUser.getFilter().filter(charSequence);
                }
                catch (Exception e){
                    Log.d(TAG, "onTextChanged: " + e.getMessage());
                }

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        return binding.getRoot();
    }
    
    private void loadAllBooks() {
        //init list
        pdfArrayList = new ArrayList<>();

        //get all books
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //clear list before adding data
                pdfArrayList.clear();
                for(DataSnapshot ds: snapshot.getChildren()){
                    ModelPdf modelPdf = ds.getValue(ModelPdf.class);
                    pdfArrayList.add(modelPdf);
                }
                //setup adapter
                adapterPdfUser = new AdapterPdfUser(getContext(), pdfArrayList);
                //set adapter to recycler view
                binding.bookRv.setAdapter(adapterPdfUser);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadMostViewedBooks(String orderBy) {
        //init list
        pdfArrayList = new ArrayList<>();

        //get all books
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.orderByChild(orderBy).limitToLast(5)//Load 5 most viewed books
        .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //clear list before adding data
                pdfArrayList.clear();
                for(DataSnapshot ds: snapshot.getChildren()){
                    ModelPdf modelPdf = ds.getValue(ModelPdf.class);
                    pdfArrayList.add(modelPdf);
                }
                //setup adapter
                adapterPdfUser = new AdapterPdfUser(getContext(), pdfArrayList);
                //set adapter to recycler view
                binding.bookRv.setAdapter(adapterPdfUser);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadBooksByCategory() {
        //init list
        pdfArrayList = new ArrayList<>();

        //get all books
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.orderByChild("categoryId").equalTo(categoryId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //clear list before adding data
                        pdfArrayList.clear();
                        for(DataSnapshot ds: snapshot.getChildren()){
                            ModelPdf modelPdf = ds.getValue(ModelPdf.class);
                            pdfArrayList.add(modelPdf);
                        }
                        //setup adapter
                        adapterPdfUser = new AdapterPdfUser(getContext(), pdfArrayList);
                        //set adapter to recycler view
                        binding.bookRv.setAdapter(adapterPdfUser);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
}