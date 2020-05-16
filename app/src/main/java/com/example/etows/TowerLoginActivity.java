package com.example.etows;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class TowerLoginActivity extends AppCompatActivity {
    private EditText nEmail, nPassword;
    private Button nLogin, nRegistration;

    private FirebaseAuth nAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tower_login);

        nAuth = FirebaseAuth.getInstance();

        firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user != null) {
                    Intent intent = new Intent(TowerLoginActivity.this, TowerMapActivity.class);
                    startActivity(intent);
                    finish();
                    return;
                }
            }
        };

        nAuth.addAuthStateListener(firebaseAuthListener);

        nEmail = (EditText) findViewById(R.id.email);
        nPassword = (EditText) findViewById(R.id.password);

        nLogin = (Button) findViewById(R.id.login);
        nRegistration = (Button) findViewById(R.id.registration);

        nRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = nEmail.getText().toString();
                final String password = nEmail.getText().toString();

                nAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(TowerLoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()) {
                            Toast.makeText(TowerLoginActivity.this, "Failed to sign up: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        else {
                            String user_id = nAuth.getCurrentUser().getUid();
                            DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("Users").child("Towers").child(user_id).child("name");
                            current_user_db.setValue(email);

                            // For testing purposes
                            DatabaseReference current_user_db_phone = FirebaseDatabase.getInstance().getReference().child("Users").child("Towers").child(user_id).child("phone");
                            current_user_db_phone.setValue("91475726");
                            DatabaseReference current_user_db_car = FirebaseDatabase.getInstance().getReference().child("Users").child("Towers").child(user_id).child("car");
                            current_user_db_car.setValue("mazda");
                        }
                    }
                });
            }
        });

        nLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = nEmail.getText().toString();
                final String password = nEmail.getText().toString();

                nAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(TowerLoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()) {
                            Toast.makeText(TowerLoginActivity.this, "Failed to sign in", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });


    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        nAuth.removeAuthStateListener(firebaseAuthListener);
    }
}
