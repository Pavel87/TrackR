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
import com.google.firebase.firestore.SetOptions;

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

    public static void fetchFirebaseData(Context context, List<LocationRecord> listUserRecords,
            FirebaseDownloadCompleteListener listener) {
        FirebaseHandler.listener = listener;

        boolean useOldDatabase = false;
        for (LocationRecord userRecord : listUserRecords) {
            if (userRecord.getId() == 4 || userRecord.getId() == -10) {
                // retrieve using firestorm
                useFirestorm(context, listUserRecords, userRecord);
            } else if (userRecord.getId() == -1) {
                // retrieve using firebase real time database
                useFirestorm(context, listUserRecords, userRecord);
                useOldDatabase = true;
            } else if (userRecord.getId() < 4) {
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
    private static void useFirestorm(final Context context,
            final List<LocationRecord> listUserRecords, final LocationRecord userRecord) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (userRecord.getTimestamp() + 5 * 60 * 1000L > System.currentTimeMillis()) {
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
                Log.e("FIRESTORM", recoveredUser.toString());

                // check if timestamps are same and if yes then don't
                // update loc record to save duplicate porcessing
                if (userRecord.getTimestamp() == recoveredUser.getTimestamp()) {
                    if (userRecord.getAddress().equals("")
                            || userRecord.getAddress().equals(
                                    context.getResources().getString(R.string.address_not_found))
                            || userRecord.getAddress().equals(
                                    context.getResources().getString(R.string.address_loc_error))) {
                        String address = getAddress(context, rowID, userRecord.getLatitude(),
                                userRecord.getLongitude());
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
                if (userRecord.getId() != -10) {
                    userRecord.setId(recoveredUser.getId());
                }
                String address = getAddress(context, rowID, userRecord.getLatitude(),
                        userRecord.getLongitude());
                if (address != null) {
                    userRecord.setAddress(address);
                }
                listUserRecords.set(rowID, userRecord);

                Log.d("FIRESTORM",
                        " USER UPDATED: " + userRecord.getSafeId() + ", " + userRecord.toString());
                Utility.saveJsonStringToFile(context.getFilesDir() + Constants.JSON_LOC_FILE_NAME,
                        Utility.createJsonArrayStringFromUserRecords(listUserRecords));
                if (listener != null) {
                    listener.onDownloadCompleteListener(rowID);
                    listener.updateMap();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("FIRESTORM", "FAILURE TO FETCH USER FROM DB: " + userRecord.getSafeId() + ", "
                        + userRecord.toString());
                e.printStackTrace();
            }
        });
    }

    /**
     * @param context
     * @param listUserRecords
     */
    public static void useFirebase(final Context context,
            final List<LocationRecord> listUserRecords) {

        if (database == null || dbReference == null) {
            database = FirebaseDatabase.getInstance();
            dbReference = database.getReferenceFromUrl("https://trackr1.firebaseio.com/");
        }

        dbReference.goOnline();
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
                                    int batteryLevel = -1;
                                    //TODO remove this later this year 2018
                                    if (idLong != null) {
                                        // batteryLevelShould be sent if id is not null
                                        batteryLevel = (int) Double.parseDouble(String.valueOf(
                                                snapshot.child("batteryLevel").getValue()));
                                    }

                                    double latitude = (double) snapshot.child("latitude")
                                            .getValue();
                                    double longitude = (double) snapshot.child("longitude")
                                            .getValue();
                                    long timeStamp = (long) snapshot.child("timestamp").getValue();

                                    // cellQuality will be null on older app versions
                                    Long cellQualityLong = (Long) snapshot.child("cellQuality")
                                            .getValue();
                                    int cellQuality = 0;
                                    if (cellQualityLong != null) {
                                        cellQuality = cellQualityLong.intValue();
                                    }

                                    Log.i("FIREBASE", "Recovered data from FB for id: " + i
                                            + " alias: " + listUserRecords.get(i).getAlias());

                                    if (listUserRecords.get(i).getTimestamp() > timeStamp) {
                                        continue;
                                    }
                                    // check if timestamps are same and if yes then don't
                                    // update loc record to save duplicate porcessing
                                    if (listUserRecords.get(i).getTimestamp() == timeStamp) {
                                        if (listUserRecords.get(i).getAddress().equals("")
                                                || listUserRecords.get(i).getAddress()
                                                        .equals(context.getResources().getString(
                                                                R.string.address_not_found))
                                                || listUserRecords.get(i).getAddress()
                                                        .equals(context.getResources().getString(
                                                                R.string.address_loc_error))) {
                                            String address = getAddress(context, i,
                                                    listUserRecords.get(i).getLatitude(),
                                                    listUserRecords.get(i).getLongitude());

                                            if (address != null) {
                                                listUserRecords.get(i).setAddress(address);
                                                if (listener != null) {
                                                    listener.onDownloadCompleteListener(i);
                                                }
                                            }
                                        }
                                        continue;
                                    }

                                    int id = -10;
                                    if (listUserRecords.get(i).getId() != -10) {
                                        id = idLong.intValue();
                                    }
                                    // Store location and request addres translation
                                    listUserRecords.get(i).updateLocationRecord(id, latitude,
                                            longitude, timeStamp, batteryLevel, cellQuality);
                                    String address = getAddress(context, i,
                                            listUserRecords.get(i).getLatitude(),
                                            listUserRecords.get(i).getLongitude());
                                    if (address != null) {
                                        listUserRecords.get(i).setAddress(address);
                                    }
                                    Utility.saveJsonStringToFile(
                                            context.getFilesDir() + Constants.JSON_LOC_FILE_NAME,
                                            Utility.createJsonArrayStringFromUserRecords(
                                                    listUserRecords));
                                    if (listener != null) {
                                        listener.onDownloadCompleteListener(i);
                                    }
                                }
                            }
                        }
                    }
                    dbReference.goOffline();
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

    /**
     * Method used to retrieve Address via geocoder
     * 
     * @param context
     * @param rowId
     * @param latitude
     * @param longitude
     * @return
     */
    private static String getAddress(Context context, int rowId, double latitude,
            double longitude) {
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

    /**
     * This is util method to delete unused ID from Firebase DB
     */
    protected static void deleteUnusedIdFromFb() {
        final long timeThreshold = System.currentTimeMillis() - Constants.OLD_ID_THRESHOLD; // 10
                                                                                            // days
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("devs").whereLessThanOrEqualTo("timestamp", timeThreshold).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d("FIRESTORM", document.getId() + " => DELETED");
                                db.collection("devs").document(document.getId()).delete();
                            }
                        } else {
                            Log.d("FIRESTORM", "Error getting documents: ", task.getException());
                        }
                    }
                });
        deleteUnusedIdsOnFirebase(timeThreshold);
    }

    // This method will delete unused ID from Firebase DB
    private static void deleteUnusedIdsOnFirebase(final long timeThreshold) {

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference dbReference = database
                .getReferenceFromUrl("https://trackr1.firebaseio.com/");

        dbReference.goOnline();
        dbReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    long timestamp = timeThreshold - 1;
                    try {
                        // Some stored keys might be in weird format likely because people use //\\
                        timestamp = (long) snapshot.child("timestamp").getValue();
                    } catch (Exception ex) {
                        Log.d(Constants.TAG, "Error while deleting old records: " + ex.getMessage()
                                + " " + snapshot.toString());
                    }
                    String id = snapshot.getKey();
                    if (timestamp < timeThreshold) {
                        Log.d(Constants.TAG, id + " ID was not updated in last 7 days - FIREBASE");
                        dbReference.child(id).removeValue();
                        // firebase.child(id).removeValue();
                    }
                }
                dbReference.goOffline();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(Constants.TAG, "DELETING UNUSED IDs WAS CANCELED");
            }
        });
    }

    /**
     * UPLOAD LOCATION RECORD TO FIRESTORM AND FIREBASE
     *
     * @param locationTxObject
     * @param trackingID
     */
    protected static void fireUpload(final LocationTxObject locationTxObject,
            final String trackingID, final TrackLocationUpdateListener listener) {
        try {
            Log.d(Constants.TAG, "FIRE Upload");

            /** FIRESTORM **/
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            // Add a new document with a generated ID
            db.collection("devs").document(trackingID).set(locationTxObject, SetOptions.merge())
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("FIRESTORM", "Upload for " + trackingID);
                            if (listener != null) {
                                listener.newLocationUploadFinished();
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("FIRESTORM", "Error uploading TrackeR data", e);
                        }
                    });

            /** FIREBASE **/
            if (database == null || dbReference == null) {
                database = FirebaseDatabase.getInstance();
                dbReference = database.getReferenceFromUrl("https://trackr1.firebaseio.com/");
            }
            dbReference.goOnline();
            Log.d(TAG, "FIREBASE - attempt to update location");
            dbReference.keepSynced(false);
            dbReference.child(trackingID).child("batteryLevel")
                    .setValue(locationTxObject.getBatteryLevel());
            dbReference.child(trackingID).child("latitude")
                    .setValue(locationTxObject.getLatitude());
            dbReference.child(trackingID).child("longitude")
                    .setValue(locationTxObject.getLongitude());
            dbReference.child(trackingID).child("timestamp")
                    .setValue(locationTxObject.getTimestamp());
            dbReference.child(trackingID).child("cellQuality")
                    .setValue(locationTxObject.getCellQuality());
            dbReference.child(trackingID).child("id").setValue(locationTxObject.getId());
            dbReference.goOffline();
        } catch (Exception e) {
            e.printStackTrace();
            if (dbReference != null) {
                dbReference.goOffline();
            }
        }
    }

}
