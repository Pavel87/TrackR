package com.pacmac.trackr;

import android.content.Context;
import android.location.Geocoder;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

public final class FirebaseHandler {

    private static FirebaseDownloadCompleteListener listener;

    public static void fetchFirebaseData(Context context, List<LocationRecord> userRecords, FirebaseDownloadCompleteListener listener) {
        FirebaseHandler.listener = listener;
        useFirestorm(context, userRecords);
    }

    public interface FirebaseDownloadCompleteListener {
        void onDownloadCompleteListener(int id);
    }


    private static void useFirestorm(final Context context, final List<LocationRecord> userRecords) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        for (final LocationRecord userRecord : userRecords) {

            if(userRecord.getTimestamp() + 15 * 60 * 1000L > System.currentTimeMillis()) {
                continue;
            }
            DocumentReference docRef = db.collection("devs").document(userRecord.getSafeId());
            docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {

                    int rowID = userRecords.indexOf(userRecord);
                    LocationTxObject recoveredUser = documentSnapshot.toObject(LocationTxObject.class);
                    Log.e("FIRESTORM_PACMAC", recoveredUser.toString());

                    // check if timestamps are same and if yes then don't
                    // update loc record to save duplicate porcessing
                    if (userRecord.getTimestamp() == recoveredUser.getTimestamp()) {
                        if (userRecord.getAddress().equals("")
                                || userRecord.getAddress().equals(context.getResources().getString(R.string.address_not_found))
                                || userRecord.getAddress().equals(context.getResources().getString(R.string.address_loc_error))) {
                            String address = getAddress(context, rowID, userRecord.getLatitude(), userRecord.getLongitude());
                            if (address != null) {
                                userRecord.setAddress(address);
                                listener.onDownloadCompleteListener(rowID);
                            }
                        }
                        return;
                    }

                    userRecord.setBatteryLevel(recoveredUser.getBatteryLevel());
                    userRecord.setCellQuality(recoveredUser.getCellQuality());
                    userRecord.setTimestamp(recoveredUser.getTimestamp());
                    userRecord.setLatitude(recoveredUser.getLatitude());
                    userRecord.setLongitude(recoveredUser.getLongitude());
                    userRecord.setId(recoveredUser.getId());
                    String address = getAddress(context, rowID, userRecord.getLatitude(), userRecord.getLongitude());
                    if (address != null) {
                        userRecord.setAddress(address);
                    }
                    userRecords.set(rowID, userRecord);

                    Log.d("FIRESTORM_PACMAC", " USER UPDATED: " + userRecord.getSafeId()
                            + ", " + userRecord.toString());
                    Utility.saveJsonStringToFile(context.getFilesDir() + Constants.JSON_LOC_FILE_NAME,
                            Utility.createJsonArrayStringFromUserRecords(userRecords));
                    listener.onDownloadCompleteListener(rowID);

                }
            })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d("FIRESTORM_PACMAC", "FAILURE TO FETCH USER FROM DB: " + userRecord.getSafeId()
                                    + ", " + userRecord.toString());
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
    // check if timestamps are same and if yes then don't
    // update loc record to save duplicate porcessing
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
//private void testFirestormREAD() {
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//
//        DocumentReference docRef = db.collection("users").document("pavelTest3");
//        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
//            @Override
//            public void onSuccess(DocumentSnapshot documentSnapshot) {
//                LocationTxObject locationTxObject = documentSnapshot.toObject(LocationTxObject.class);
//                Log.e("FIRESTORM_PACMAC", locationTxObject.toString());
//            }
//        });
//    }

    private static String getAddress(Context context, int rowId, double latitude, double longitude) {
        if (Geocoder.isPresent()) {
            Thread t = new Thread(new AddressResolverRunnable(context, rowId, latitude, longitude));
            t.setName("AddressResolverTrackR");
            t.setDaemon(true);
            t.start();
        } else {
            return context.getResources().getString(R.string.not_available);
        }
        return null;
    }

    //     This is util method to delete unused ID from Firebase DB
    protected static void deleteUnusedIdFromFb(Context context) {
        final long timeThreshold = System.currentTimeMillis() - Constants.OLD_ID_THRESHOLD; // 10 days
        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("devs")
                .whereLessThanOrEqualTo("timestamp", timeThreshold)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d("FIRESTORM_PACMAC", document.getId() + " => " + document.getData());
                                db.collection("devs").document(document.getId()).delete();
                            }
                        } else {
                            Log.d("FIRESTORM_PACMAC", "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

}

