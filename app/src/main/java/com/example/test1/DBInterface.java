package com.example.test1;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

/**
 * DBInterface class is an class used for interacting between the main application and
 * the database.
 */

class DBInterface {


    private static final String TAG = "DBSTORE";
    private static final String ROOM = "room_store";
    private static final String NEXTCODE = "next_short";
    private static final String PREFIX = "anchor";
    private static final int INTIALSHORT = 0;
    private final DatabaseReference rootDBref;
    /**
     * Interface to check for new short codes in the database
     */
    public interface shortCodeGenerate {
        void onShortAvailable(Integer shortCode);
    }

    /**
     * Interface to check for a new Cloud Anchor ID from the database
     */
    public interface anchorGenerate {
        void onAnchorAvailable(String cloudAnchorID);
    }


    /**
     * Class constructor putting the database online
     * @param context for the interface
     */
    DBInterface(Context context) {
        FirebaseApp firebase = FirebaseApp.initializeApp(context);
        rootDBref = FirebaseDatabase.getInstance(firebase).getReference().child(ROOM);
        DatabaseReference.goOnline();
    }


    /**
     * Method to generate the short code for the primary key of the DB
     * Completes an transaction on the element containing the next short code
     * Increments this by one
     * @param generate from the generate short code interface
     */
    void nextShort(shortCodeGenerate generate) {
        rootDBref.child(NEXTCODE).runTransaction(
                new Transaction.Handler() {
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                        Integer shortCode = mutableData.getValue(Integer.class);
                        if (shortCode == null) {
                            shortCode = INTIALSHORT - 1;
                        }
                        mutableData.setValue(shortCode + 1);
                        return Transaction.success(mutableData);
                    }

                    @Override
                    public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {
                        if (!b) {
                            Log.e(TAG, "Database error", databaseError.toException());
                            generate.onShortAvailable(null);
                        } else {
                            generate.onShortAvailable(dataSnapshot.getValue(Integer.class));
                        }
                    }
                }
        );

    }

    /**
     * Method which gets the Cloud Anchor ID using the short code parsed into it
     * @param shortCode Integer
     * @param generate Listener for a new anchor
     */
    void anchorID(int shortCode, anchorGenerate generate) {
        rootDBref.child(PREFIX + shortCode).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        generate.onAnchorAvailable(String.valueOf(dataSnapshot.getValue()));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Database error - operation cancelled", databaseError.toException());
                        generate.onAnchorAvailable(null);
                    }
                }
        );
    }

    /**
     * Method to push all of the data into the Database
     * Sets the prefix and short code as the key and the remaining data as the value
     * @param shortCode integer used as the unique id to identify each anchor
     * @param imagePath relative path of the user selected image
     * @param userText string of text added by the user
     * @param cloudAnchorId unique ID for the cloud anchor
     */
    void storeUsingShortCode(int shortCode, String imagePath, String userText, String cloudAnchorId) {
        rootDBref.child(PREFIX + shortCode).setValue(cloudAnchorId + "," + imagePath + "," + userText);
    }

}
