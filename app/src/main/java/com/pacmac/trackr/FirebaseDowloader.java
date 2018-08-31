package com.pacmac.trackr;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public final class FirebaseDowloader {


    public static void fetchFirebaseData(Context context, List<LocationRecord> userRecords, FirebaseDownloadCompleteListener listener) {





    }

    public interface FirebaseDownloadCompleteListener {
        void onDownloadCompleteListener(int id);
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

    private static void useFirestorm(List<LocationRecord> userRecords) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        for (LocationRecord userRecord : userRecords) {
            DocumentReference docRef = db.collection("users").document(id);
            docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    documentSnapshot.getId()
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

//
//    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
//
//                        for (int i = 0; i < userRecords.size(); i++) {
//                            if (snapshot.getKey().equals(userRecords.get(i).getSafeId())) {
//                                // Processing received data
//                                if (snapshot.hasChildren()) {
//
//                                    Map<String, Object> toDelete = new HashMap<>();
//
//                                    Long idLong = ((Long) snapshot.child("id").getValue());
//                                    double batteryLevel = -1;
//                                    if (idLong != null) {
//                                        // batteryLevelShould be sent if id is not null
//                                        batteryLevel = Double.parseDouble(String.valueOf(snapshot.child("batteryLevel").getValue()));
//                                    }
//                                    double latitude = (double) snapshot.child("latitude").getValue();
//                                    double longitude = (double) snapshot.child("longitude").getValue();
//                                    long timeStamp = (long) snapshot.child("timestamp").getValue();
//
//                                    // cellQuality will be null on older app versions
//                                    Long cellQualityLong = (Long) snapshot.child("cellQuality").getValue();
//                                    int cellQuality = 0;
//                                    if (cellQualityLong != null) {
//                                        cellQuality = cellQualityLong.intValue();
//                                    }
//
//
//                                    Log.i(Constants.TAG, "Recovered data from FB for id: " + i + " alias: " + userRecords.get(i).getAlias());
//
//                                    // check if timestamps are same and if yes then don't
//                                    // update loc record to save duplicate porcessing
//
//                                    if (userRecords.get(i).getTimestamp() == timeStamp) {
//                                        if (userRecords.get(i).getAddress().equals("")
//                                                || userRecords.get(i).getAddress().equals(getResources().getString(R.string.address_not_found))
//                                                || userRecords.get(i).getAddress().equals(getResources().getString(R.string.address_loc_error))) {
//                                            getAddress(i);
//                                        }
//                                        continue;
//                                    }
//                                    // Store location and request addres translation
//                                    userRecords.get(i).updateLocationRecord(latitude, longitude, timeStamp, batteryLevel, cellQuality);
//                                    mAdapter.notifyItemChanged(i);
//                                    getAddress(i);
//                                    Utility.saveJsonStringToFile(getFilesDir() + Constants.JSON_LOC_FILE_NAME,
//                                            Utility.createJsonArrayStringFromUserRecords(userRecords));
//
//                                }
//
//                            }
//                        }
//                    }

}

