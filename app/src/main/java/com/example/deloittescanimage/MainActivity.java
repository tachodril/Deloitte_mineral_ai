package com.example.deloittescanimage;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.renderscript.Sampler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {

    private TextView instructions, sendButton, addUrlText, classText, status, confidenceScore;
    private CardView addPhoto;
    private ImageView imageView, addUrlImg;


    Bitmap imageBitmap;
    private Uri filePath;
    private boolean ifImageAdded = false;
    private final int PICK_IMAGE_REQUEST = 71;
    private final int CAMERA_RESULT = 1888;
    private static final int PERMISSION_REQUEST_CODE = 200;

    private FirebaseStorage firebaseStorage;
    private StorageReference sref;
    private StorageTask uploadTask;

    private String imageUrl;
    private ProgressDialog progressDialog;
    private String curClass = "-1";
    private String confidence = "-1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseStorage = FirebaseStorage.getInstance();
        sref = firebaseStorage.getReference("images");
        init();
        receiveClicks();
    }

    private void receiveClicks() {

        addUrlText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrlInput();
            }
        });

        addUrlImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrlInput();
            }
        });

        addPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!ifImageAdded) {
                    Toast.makeText(MainActivity.this, "Add a image first", Toast.LENGTH_SHORT).show();
                    return;
                }

                progressDialog.show();
                Long l = System.currentTimeMillis() / 1000;
                String timeStamp = l.toString();

                final StorageReference path = sref.child(timeStamp + ".jpg");

                imageView.setDrawingCacheEnabled(true);
                imageView.buildDrawingCache();
                Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] data = baos.toByteArray();

                uploadTask = path.putBytes(data);
                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        Toast.makeText(MainActivity.this, "Image Upload Failed", Toast.LENGTH_SHORT).show();
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // API call
                    }
                });

                Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }

                        // Continue with the task to get the download URL
                        return path.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            Uri downloadUri = task.getResult();
                            imageUrl = downloadUri.toString();
                            Log.e("then: ", imageUrl);
                            fetchFromApi();
                            //progressDialog.dismiss();
                        } else {
                            // Handle failures
                            // ...
                        }
                    }
                });
            }
        });
    }

    private void openUrlInput() {
        final EditText inputEditTextField = new EditText(this);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Enter URL")
                //.setMessage("Message")
                .setView(inputEditTextField)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String editTextInput = inputEditTextField.getText().toString();
                        Log.d("onclick", "editext value is: " + editTextInput);

                        Picasso.get().load(editTextInput).into(imageView, new Callback() {
                            @Override
                            public void onSuccess() {
                                ifImageAdded = true;
                            }

                            @Override
                            public void onError(Exception e) {

                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
    }

    private void fetchFromApi() {
        try {
            RequestQueue requestQueue = Volley.newRequestQueue(this);
            String URL = "https://bp7vd1oa4m.execute-api.us-east-1.amazonaws.com/Prod/invocations/";
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("url", imageUrl);
            final String requestBody = jsonBody.toString();

            StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.i("VOLLEY response", response);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("VOLLEY error", error.toString());
                    progressDialog.dismiss();
                }
            }) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }

                @Override
                public byte[] getBody() throws AuthFailureError {
                    try {
                        return requestBody == null ? null : requestBody.getBytes("utf-8");
                    } catch (UnsupportedEncodingException uee) {
                        VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                        return null;
                    }
                }

                @Override
                protected Response<String> parseNetworkResponse(NetworkResponse response) {
                    String responseString = "";
                    if (response != null) {
                        responseString = String.valueOf(response.statusCode);
                        // can get more details such as response.headers
                        //Log.e("parseNetworkResponse: ",response.toString());

                        try {
                            String json = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                            Log.e("NetworkResponse_new1", json);
                            JSONObject obj = new JSONObject(json);
                            Log.e("NetworkResponse_2", obj.toString());
                            curClass = obj.getString("class");
                            confidence = String.valueOf(obj.getDouble("confidence"));
                            setText();
                            //Toast.makeText(MainActivity.this, ""+curClass+" with "+confidence, Toast.LENGTH_SHORT).show();
                        } catch (UnsupportedEncodingException | JSONException e) {
                            e.printStackTrace();
                        }
                        progressDialog.dismiss();
                    }
                    return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
                }
            };

            requestQueue.add(stringRequest);
        } catch (JSONException e) {
            e.printStackTrace();
            progressDialog.dismiss();
        }
    }

    private void setText() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int x=4;
                if(x>confidence.length()) x=confidence.length();
                classText.setText(curClass);
                confidenceScore.setText(Character.toUpperCase(confidence.charAt(0))+confidence.substring(1,x)+"%");
            }
        });
    }

    private void init() {
        sendButton = findViewById(R.id.send_btn);
        instructions = findViewById(R.id.instructions);
        instructions.setText("Repeat {" + "\n" + "  addPhoto();" + "\n"
                + "  clickOnSendButton();" + "\n" + "}");
        addPhoto = findViewById(R.id.card_add_photo);
        imageView = findViewById(R.id.show_image);
        addUrlText = findViewById(R.id.add_url_text);
        addUrlImg = findViewById(R.id.add_url_img);
        classText = findViewById(R.id.class_text);
        status = findViewById(R.id.result_tag);
        confidenceScore = findViewById(R.id.confidence_score);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait");
        progressDialog.setMessage("Uploading image...");
    }

    private void selectImage() {
        final CharSequence[] options = {"Take a Photo", "Choose from Gallery", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Upload your Shopping List");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (options[item].equals("Take a Photo")) {
                    //Camera Button
                    if (checkCameraPermission()) {
                        //main logic or main code
                        openCamera();
                    } else {
                        requestPermission();
                    }


                } else if (options[item].equals("Choose from Gallery")) {
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
                } else if (options[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    public void openCamera() {
        PackageManager pm = getPackageManager();

        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            //i.putExtra(MediaStore.EXTRA_OUTPUT, MyFileContentProvider.CONTENT_URI);
            startActivityForResult(i, CAMERA_RESULT);
        } else {
            Toast.makeText(getBaseContext(), "Camera is not available", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.e("onActivityResult: ", "level 0 " + resultCode + " " + requestCode);
        if (resultCode == Activity.RESULT_OK && requestCode == CAMERA_RESULT) {
            Log.e("onActivityResult: ", "level 1");
            File out = new File(getFilesDir(), "newImage.jpg");
            if (!out.exists()) {
                Toast.makeText(getBaseContext(), "Error while capturing image!", Toast.LENGTH_LONG).show();
                return;
            }
            Log.e("onActivityResult: ", out.getAbsolutePath());
            Log.e("onActivityResult: ", "level 2");
            Bitmap mBitmap = BitmapFactory.decodeFile(out.getAbsolutePath());

            Bitmap bitmap = (Bitmap) data.getExtras().get("data");

            //rotating bitMap by 90 degrees
//            float degrees = 90;
//            Matrix matrix = new Matrix();
//            matrix.setRotate(degrees);
//            Bitmap output=mBitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);
            imageView.setImageBitmap(bitmap);
            imageBitmap = bitmap;
            ifImageAdded = true;

        } else if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            filePath = data.getData();
            Log.e("onActivityResult: ", "level 3");
            try {
                imageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                imageView.setImageBitmap(imageBitmap);
                ifImageAdded = true;
            } catch (Exception e) {

            }
        }


        if (ifImageAdded) {
//            detectTextBtn.setEnabled(true);
//            detectTextBtn.setAlpha(0.9f);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    openCamera();
//                    Toast.makeText(getApplicationContext(), "Permission granted", Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            showMessageOKCancel("Camera permission required to scan shopping list!",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermission();
                                            }
                                        }
                                    });
                        }
                    }
                }
                break;
            }
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                PERMISSION_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            return false;
        }
        return true;
    }
}
