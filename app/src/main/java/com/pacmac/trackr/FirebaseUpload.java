package com.pacmac.trackr;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

/**
 * Created by pacmac on 2018-08-30.
 */

public final class FirebaseUpload {


    protected static void firestormUpload(final LocationTxObject locationTxObject, final String trackingID) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Add a new document with a generated ID
        db.collection("devs")
                .document(trackingID)
                .set(locationTxObject, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.e("FIRESTORM_PACMAC", "Data uploaded with ID: " + trackingID);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("FIRESTORM_PACMAC", "Error uploading trackr data", e);
                    }
                });

    }


    private void testFirestormREAD() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference docRef = db.collection("users").document("pavelTest3");
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                LocationTxObject locationTxObject = documentSnapshot.toObject(LocationTxObject.class);
                Log.e("FIRESTORM_PACMAC", locationTxObject.toString());
            }
        });
    }

    private void testFirestormReadMulti() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String[] ids = new String[]{"pavelTest3", "pavelTest4", "pavelTest5"};

        for (String id : ids) {
            DocumentReference docRef = db.collection("users").document(id);
            docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    LocationTxObject locationTxObject = documentSnapshot.toObject(LocationTxObject.class);
                    Log.e("FIRESTORM_PACMAC", locationTxObject.toString());
                }
            })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            e.printStackTrace();
                        }
                    });
        }
    }

}
