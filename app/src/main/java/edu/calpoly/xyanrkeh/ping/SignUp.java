package edu.calpoly.xyanrkeh.ping;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Created by xiang on 11/20/2016.
 */

public class SignUp extends Activity {

    private static final int MIN_CHAR = 6;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseAuth mAuth;
    private EditText eMail;
    private EditText pswd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_email_signup);
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d("LOGINLOG", "onAuthStateChanged:signed_in:" + user.getUid());
                    Intent mapInt = new Intent(SignUp.this, MapsActivity.class);
                    startActivity(mapInt);
                } else {
                    // User is signed out
                    Log.d("LOGINLOG", "onAuthStateChanged:signed_out");
                }
            }
        };
        eMail = (EditText) findViewById(R.id.sign_up_email_field);
        pswd = (EditText) findViewById(R.id.sign_up_password_field);

        Button signIn = (Button) findViewById(R.id.sign_in_button);
        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logInUser(eMail.getText().toString().trim(), pswd.getText().toString());
            }
        });

        Button signUp = (Button) findViewById(R.id.sign_up_button);
        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createUser(eMail.getText().toString().trim(), pswd.getText().toString());
            }
        });

        Button cancel = (Button) findViewById(R.id.sign_up_cancel_button);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent back = new Intent(SignUp.this, MainActivity.class);
                startActivity(back);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    private void createUser(String email, String password) {
        if (validate(email, password)) {
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            Log.d("LOGINLOG", "createUserWithEmail:onComplete:" + task.isSuccessful());

                            // If sign in fails, display a message to the user. If sign in succeeds
                            // the auth state listener will be notified and logic to handle the
                            // signed in user can be handled in the listener.
                            if (!task.isSuccessful()) {
                                Log.d("LOGINLOG", "createUserWithEmail:failed", task.getException());
                                AlertDialog.Builder builder = new AlertDialog.Builder(SignUp.this);
                                builder.setTitle("Account Creation Failed");
                                builder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                                builder.show();
                            }
                        }
                    });
        }
    }

    private boolean validate(String email, String password) {
        boolean result = true;
        if (password.length() < MIN_CHAR || password.contains(" ")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(SignUp.this);
            TextView req = new TextView(SignUp.this);
            req.setText("Passwords must be longer than 6 characters and may not contain any spaces.");
            builder.setTitle(R.string.invalid);
            builder.setView(req);
            builder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.show();
            result = false;
        } else if (!email.contains("@") || !email.contains(".")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(SignUp.this);
            TextView req = new TextView(SignUp.this);
            req.setText("The email entered was invalid.");
            builder.setTitle(R.string.invalid_email);
            builder.setView(req);
            builder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.show();
            result = false;
        }
        return result;
    }

    private void logInUser(String email, String password) {
        if (validate(email, password)) {
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            Log.d("LOGINLOG", "signInWithEmail:onComplete:" + task.isSuccessful());

                            // If sign in fails, display a message to the user. If sign in succeeds
                            // the auth state listener will be notified and logic to handle the
                            // signed in user can be handled in the listener.
                            if (!task.isSuccessful()) {
                                Log.d("LOGINLOG", "signInWithEmail:failed", task.getException());
                                AlertDialog.Builder builder = new AlertDialog.Builder(SignUp.this);
                                builder.setTitle("Login Failed");
                                builder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                                builder.show();
                            }
                        }
                    });
        }
    }
}
