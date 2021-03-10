package com.lyadirga.neredesinbakalm;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

public class GirisKayitActivity<valid> extends AppCompatActivity {


    private TextInputLayout tilEmail, tilSifre, tilKullaniciAdi;
    private TextInputEditText email, sifre, kullaniciadi;
    private ProgressBar progresBarCircle;
    private ImageView profilPhoto;
    private Uri profilPhotoUri = null;
    private static final int RESIM_SEC = 1;

    private FirebaseAuth mAuth;
    private StorageReference mStorageRef;
    private DatabaseReference databaseReference;
    private ValueEventListener databaseEventListener;
    private boolean profildegisecek = false;

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            progresBarCircle.setVisibility(View.VISIBLE);
            updateUI(user);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (databaseReference!=null && databaseEventListener!=null)
            databaseReference.removeEventListener(databaseEventListener);
    }

    private void updateUI(FirebaseUser user) {
        if (user == null) {
            kullaniciadi.setText(null);
            profilPhoto.setImageResource(R.drawable.avatar);
            email.setText(null);
            sifre.setText(null);
            return;
        }

        if (databaseReference == null) {
            databaseReference = FirebaseDatabase.getInstance().getReference().child("Kullanicilar")
                    .child(user.getUid());
        }

        ((Button) findViewById(R.id.buttonGiris)).setText("Takip Listesine Git");

        if (profildegisecek) {

            progresBarCircle.setVisibility(View.GONE);
            return;
        }

        databaseEventListener = databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progresBarCircle.setVisibility(View.GONE);

                String ad = snapshot.child("kullanici_adi").getValue(String.class);
                String url = snapshot.child("profil_url").getValue(String.class);

                kullaniciadi.setText(ad);
                Picasso.get().load(url).into(profilPhoto);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progresBarCircle.setVisibility(View.GONE);
            }
        });


    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_giris_kayit);

        mAuth = FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();

        tilEmail = findViewById(R.id.tilEmail);
        email = findViewById(R.id.email);

        tilKullaniciAdi = findViewById(R.id.tilKullaniciAdi);
        kullaniciadi = findViewById(R.id.kullaniciadi);

        tilSifre = findViewById(R.id.tilSifre);
        sifre = findViewById(R.id.sifre);

        progresBarCircle = findViewById(R.id.progresBarCircle);
        profilPhoto = findViewById(R.id.profil);

        if (mAuth.getCurrentUser() != null) {
            databaseReference = FirebaseDatabase.getInstance().getReference().child("Kullanicilar")
                    .child(mAuth.getCurrentUser().getUid());
        }

        profilPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent intent2 = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                //startActivityForResult(intent2,RESIM_CEK);
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Resim seçiniz"), RESIM_SEC);
                profildegisecek=true;

            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESIM_SEC && resultCode == RESULT_OK && data != null && data.getData() != null) {
            //databaseReference.removeEventListener(databaseEventListener);
            profilPhotoUri = data.getData();
            profilPhoto.setImageURI(null);
            profilPhoto.setImageURI(profilPhotoUri);

            //Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            //profilPhoto.setImageBitmap(bitmap);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, Menu.NONE, "Çıkış").setIcon(R.drawable.ic_baseline_exit_to_app_24).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mAuth.signOut();
        updateUI(null);
        return super.onOptionsItemSelected(item);
    }

    public void profilkaydet(View view) {


        tilKullaniciAdi.setError(null);

        if (profilPhotoUri == null || TextUtils.isEmpty(kullaniciadi.getText())) {

            if (profilPhotoUri == null)
                Toast.makeText(this, "Lütfen profil fotoğrafınızı seçiniz...", Toast.LENGTH_SHORT).show();

            if (TextUtils.isEmpty(kullaniciadi.getText()))
                tilKullaniciAdi.setError("Lütfen kullanıcı adınızı giriniş");

            return;
        }

        //Firebase profil bilgisi kayıt
        progresBarCircle.setVisibility(View.VISIBLE);

        String kullaniciAdi = kullaniciadi.getText().toString();
        String uzanti = getFileExtension(profilPhotoUri);

        StorageReference childRef = mStorageRef.child("KullaniciProfili").child(mAuth.getCurrentUser().getUid())
                .child(mAuth.getCurrentUser().getUid()).child(kullaniciAdi + "." + uzanti);
        childRef.putFile(profilPhotoUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                Task<Uri> result = taskSnapshot.getMetadata().getReference().getDownloadUrl();
                result.addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        databaseReference.child("kullanici_adi").setValue(kullaniciAdi);
                        databaseReference.child("kullanici_id").setValue(mAuth.getCurrentUser().getUid());
                        databaseReference.child("profil_url").setValue(uri.toString());

                       profildegisecek=false;
                    }
                });

                Toast.makeText(GirisKayitActivity.this, "Yuppi", Toast.LENGTH_SHORT).show();
                progresBarCircle.setVisibility(View.GONE);
            }
        });

    }

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));

    }

    public void kayitOl(View view) {
        if (!validateForm())
            return;

        progresBarCircle.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email.getText().toString(), sifre.getText().toString())
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progresBarCircle.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Toast.makeText(GirisKayitActivity.this, "Kayıt başarılı",
                                    Toast.LENGTH_SHORT).show();
                            ((Button) findViewById(R.id.buttonGiris)).setText("Takip Listesine Git");

                        } else {
                            Toast.makeText(GirisKayitActivity.this,
                                    "Authentication failed. " + task.getException(),
                                    Toast.LENGTH_SHORT).show();
                            Log.w("TAG", "createUserWithEmail:failure", task.getException());

                        }
                    }
                });
    }


    public void girisYap(View view) {
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        if (!validateForm()) {
            return;
        }

        progresBarCircle.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email.getText().toString(), sifre.getText().toString()).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                progresBarCircle.setVisibility(View.GONE);

                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    updateUI(user);
                    Toast.makeText(GirisKayitActivity.this, "Giriş başarılı", Toast.LENGTH_SHORT).show();
                    ((Button) findViewById(R.id.buttonGiris)).setText("Takip Listesine Git");
                } else {
                    Toast.makeText(GirisKayitActivity.this, "Giriş başarısız", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private boolean validateForm() {
        boolean valid = true;
        tilEmail.setError(null);
        tilSifre.setError(null);

        if (TextUtils.isEmpty(email.getText()) || TextUtils.isEmpty(sifre.getText())) {
            if (TextUtils.isEmpty(email.getText())) {
                tilEmail.setError("Lütfen mail adresinizi giriniz");
                valid = false;
            } else {
                if (!email.getText().toString().contains("@")) {
                    tilEmail.setError("Lütfen geçerli bir mail adresi giriniz");
                    valid = false;
                }
            }
        }

        if (TextUtils.isEmpty(sifre.getText())) {
            tilSifre.setError("Lütfen şifrenizi giriniz");
            valid = false;

        }
        return valid;
    }

}