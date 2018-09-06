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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

public final class FirebaseHandler {


    public interface FirebaseDownloadCompleteListener {
        void onDownloadCompleteListener(int id);

        void updateMap();
    }

    private static final String TAG = "FirebaseHandler";

    private static FirebaseDatabase database = null;
    private static DatabaseReference dbReference = null;

    private static FirebaseDownloadCompleteListener listener;

    public static void fetchFirebaseData(Context context, List<LocationRecord> listUserRecords, FirebaseDownloadCompleteListener listener) {
        FirebaseHandler.listener = listener;

        boolean useOldDatabase = false;
        for (LocationRecord userRecord : listUserRecords) {
            if (userRecord.getId() >= 3) {
                // retrieve using firestorm
                useFirestorm(context, listUserRecords, userRecord);
            } else if (userRecord.getId() == -1) {
                //retrieve using firebase real time database
                useFirestorm(context, listUserRecords, userRecord);
                useOldDatabase = true;
            } else if (userRecord.getId() < 3) {
                useOldDatabase = true;
            }
        }

        if (useOldDatabase) {
            useFirebase(context, listUserRecords);
        }
    }


    /**
     * @param context
     * @param listUserRecords
     * @param userRecord
     */
    private static void useFirestorm(final Context context, final List<LocationRecord> listUserRecords, final LocationRecord userRecord) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (userRecord.getTimestamp() + 15 * 60 * 1000L > System.currentTimeMillis()) {
            return;
        }
        DocumentReference docRef = db.collection("devs").document(userRecord.getSafeId());
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {

                int rowID = listUserRecords.indexOf(userRecord);
                LocationTxObject recoveredUser = documentSnapshot.toObject(LocationTxObject.class);
                if (recoveredUser == null) {
                    return;
                }
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
                            if (listener != null) {
                                listener.onDownloadCompleteListener(rowID);
                            }
                        }
                    }
                    return;
                }
                if (userRecord.getTimestamp() > recoveredUser.getTimestamp()) {
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
                listUserRecords.set(rowID, userRecord);

                Log.d("FIRESTORM_PACMAC", " USER UPDATED: " + userRecord.getSafeId()
                        + ", " + userRecord.toString());
                Utility.saveJsonStringToFile(context.getFilesDir() + Constants.JSON_LOC_FILE_NAME,
                        Utility.createJsonArrayStringFromUserRecords(listUserRecords));
                if (listener != null) {
                    listener.onDownloadCompleteListener(rowID);
                    listener.updateMap();
                }

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


    /**
     * @param context
     * @param listUserRecords
     */
    public static void useFirebase(final Context context, final List<LocationRecord> listUserRecords) {

        if (database == null || dbReference == null) {
            database = FirebaseDatabase.getInstance();
            dbReference = database.getReferenceFromUrl("https://trackr1.firebaseio.com/");
        }

        dbReference.goOnline();
        Log.d(TAG, "Firebase goes online");
        dbReference.keepSynced(false);

        dbReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                        for (int i = 0; i < listUserRecords.size(); i++) {
                            if (snapshot.getKey().equals(listUserRecords.get(i).getSafeId())) {
                                // Processing received data
                                if (snapshot.hasChildren()) {

                                    Long idLong = ((Long) snapshot.child("id").getValue());
                                    double batteryLevel = -1;
                                    if (idLong != null) {
                                        // batteryLevelShould be sent if id is not null
                                        batteryLevel = Double.parseDouble(String.valueOf(snapshot.child("batteryLevel").getValue()));
                                    }
                                    double latitude = (double) snapshot.child("latitude").getValue();
                                    double longitude = (double) snapshot.child("longitude").getValue();
                                    long timeStamp = (long) snapshot.child("timestamp").getValue();

                                    // cellQuality will be null on older app versions
                                    Long cellQualityLong = (Long) snapshot.child("cellQuality").getValue();
                                    int cellQuality = 0;
                                    if (cellQualityLong != null) {
                                        cellQuality = cellQualityLong.intValue();
                                    }


                                    Log.i(Constants.TAG, "Recovered data from FB for id: " + i + " alias: " + listUserRecords.get(i).getAlias());

                                    if (listUserRecords.get(i).getTimestamp() > timeStamp)  {
                                        return;
                                    }
                                    // check if timestamps are same and if yes then don't
                                    // update loc record to save duplicate porcessing
                                    if (listUserRecords.get(i).getTimestamp() == timeStamp) {
                                        if (listUserRecords.get(i).getAddress().equals("")
                                                || listUserRecords.get(i).getAddress().equals(context.getResources().getString(R.string.address_not_found))
                                                || listUserRecords.get(i).getAddress().equals(context.getResources().getString(R.string.address_loc_error))) {
                                            String address = getAddress(context, i, listUserRecords.get(i).getLatitude(), listUserRecords.get(i).getLongitude());

                                            if (address != null) {
                                                listUserRecords.get(i).setAddress(address);
                                                if (listener != null) {
                                                    listener.onDownloadCompleteListener(i);
                                                }
                                            }
                                        }
                                        continue;
                                    }

                                    // Store location and request addres translation
                                    listUserRecords.get(i).updateLocationRecord(latitude, longitude, timeStamp, batteryLevel, cellQuality);
                                    String address = getAddress(context, i, listUserRecords.get(i).getLatitude(), listUserRecords.get(i).getLongitude());
                                    if (address != null) {
                                        listUserRecords.get(i).setAddress(address);
                                    }
                                    Utility.saveJsonStringToFile(context.getFilesDir() + Constants.JSON_LOC_FILE_NAME,
                                            Utility.createJsonArrayStringFromUserRecords(listUserRecords));
                                    if (listener != null) {
                                        listener.onDownloadCompleteListener(i);
                                    }
                                }
                            }
                        }
                    }
                    dbReference.goOffline();
                    Log.i(Constants.TAG, "Firebase goes offline");
                    // Update location markers on the map.
                    if (listener != null) {
                        listener.updateMap();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                dbReference.goOffline();
                Log.i(Constants.TAG, "Update Cancelled1" + databaseError.getMessage());
                Log.i(Constants.TAG, "Update Cancelled2" + databaseError.getDetails());

            }
        });
    }

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


    /**
     * @param context
     * @param rowId
     * @param latitude
     * @param longitude
     * @return
     */
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

