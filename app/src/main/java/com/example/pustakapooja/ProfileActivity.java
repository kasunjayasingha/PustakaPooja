package com.example.pustakapooja;

import static java.security.AccessController.getContext;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.pustakapooja.adapters.AdapterPdfUser;
import com.example.pustakapooja.adapters.AdpaterPdfAdmin;
import com.example.pustakapooja.admin.DashboardAdminActivity;
import com.example.pustakapooja.admin.pdfShowAdminActivity;
import com.example.pustakapooja.databinding.ActivityDashboardUserBinding;
import com.example.pustakapooja.databinding.ActivityProfileBinding;
import com.example.pustakapooja.login_register.LoginActivity;
import com.example.pustakapooja.models.ModelPdf;
import com.example.pustakapooja.user.DashboardUserActivity;
import com.example.pustakapooja.user.PdfReadUserActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Objects;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;

    private ArrayList<ModelPdf>pdfArrayList;

    private FirebaseAuth firebaseAuth;

    private static final String TAG = "PROFILE_TAG";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        loadUserDetails();

        binding.profileEditBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ProfileActivity.this, ProfileEditActivity.class));
                finish();
            }
        });

        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Check in db
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");
                ref.child(firebaseUser.getUid())
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {

                                //get usertype
                                String userType = ""+snapshot.child("userType").getValue();
                                //check user Type
                                if(userType.equals("user")){
                                    startActivity(new Intent(ProfileActivity.this, DashboardUserActivity.class));
                                    finish();
                                } else if (userType.equals("admin")) {
                                    startActivity(new Intent(ProfileActivity.this, DashboardAdminActivity.class));
                                    finish();
                                }

                            }

                            @Override
                            public void onCancelled(DatabaseError error) {

                            }
                        });

            }
        });
    }

    private void loadUserDetails() {
        Log.d(TAG, "loadUserDetails: Loading user details..." + firebaseAuth.getUid());

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");
        ref.child(firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = ""+snapshot.child("name").getValue();
                String email = ""+snapshot.child("email").getValue();
                String profileImage = ""+snapshot.child("profileImage").getValue();
                String accountType = ""+snapshot.child("userType").getValue();
                String timestamp = ""+snapshot.child("timestamp").getValue();

                loadFavBooks();

                String formattedDate = MyApplication.formatTimestamp(Long.parseLong(timestamp));

                binding.nameTv.setText(name);
                binding.emailTv.setText(email);
                binding.memberDateTv.setText(formattedDate);
                binding.accountType.setText(accountType);


                try {
                    Glide.with(ProfileActivity.this)
                            .load(profileImage)
                            .placeholder(R.drawable.ic_person_gray)
                            .into(binding.profileImg);
                }
                catch (Exception e) {
                    binding.profileImg.setImageResource(R.drawable.ic_person_gray);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadFavBooks(){
        pdfArrayList=new ArrayList<>();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");
        ref.child(Objects.requireNonNull(firebaseAuth.getUid())).child("Favorites")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        pdfArrayList.clear();
                        int count = 0;

                        for(DataSnapshot ds:snapshot.getChildren()){
                            String bookId =""+ds.child("bookId").getValue();
                            count += 1;

                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books").child(bookId);
                            ref.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    ModelPdf modelPdf = snapshot.getValue(ModelPdf.class);
                                    pdfArrayList.add(modelPdf);

                                    //setup adapter
                                    AdapterPdfUser adapterPdfUser = new AdapterPdfUser(ProfileActivity.this, pdfArrayList);
                                    //set adapter to recycler view
                                    binding.booksRv.setAdapter(adapterPdfUser);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                        }
                        binding.favoriteCountTv.setText(""+count);

                    
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
}