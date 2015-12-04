package com.example.joe.criminalintent;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

/**
 * Created by Joe on 10/5/2015.
 */
public class CrimeFragment extends Fragment {

    public static final String ARG_CRIME_ID = "crime_id";
    private static final String DIALOG_DATE = "DialogDate";
    private static final String DIALOG_TIME = "DialogTime";
    private static final String DIALOG_SUSPECT = "DialogSuspect";

    private static final int REQUEST_DATE = 0;
    private static final int REQUEST_TIME = 1;
    private static final int REQUEST_CONTACT = 2;
    private static final int REQUEST_PHOTO = 3;
    private static final String TAG = "CrimeFragment";


    private Crime mCrime;
    private EditText mTitleField;
    private Button mDateButton;
    private Button mTimeButton;
    private CheckBox mSolvedCheckBox;
    private Button mReportButton;
    private Button mSuspectButton;
    private Button mCallSuspectButton;
    private ImageButton mPhotoButton;
    private ImageView mPhotoView;
    private File mPhotoFile;


    private static String[] PERMISSIONS_CONTACT = {Manifest.permission.READ_CONTACTS};

    public static CrimeFragment newInstance(UUID crimeId){
        Bundle args = new Bundle();
        args.putSerializable(ARG_CRIME_ID, crimeId);

        CrimeFragment fragment = new CrimeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UUID crimeId = (UUID) getArguments().getSerializable(ARG_CRIME_ID);
        mCrime = CrimeLab.get(getActivity()).getCrime(crimeId);
        setHasOptionsMenu(true);
        mPhotoFile = CrimeLab.get(getActivity()).getPhotoFile(mCrime);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_crime, container, false);

        mTitleField = (EditText) v.findViewById(R.id.crime_title);
        mTitleField.setText(mCrime.getTitle());
        mTitleField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCrime.setTitle(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mDateButton = (Button) v.findViewById(R.id.crime_date);
        updateDate();
        mDateButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                FragmentManager manager = getFragmentManager();
                DatePickerFragment dialog = DatePickerFragment.newInstance(mCrime.getDate());
                dialog.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
                dialog.show(manager, DIALOG_DATE);
            }
        });

        mTimeButton = (Button) v.findViewById(R.id.crime_time);
        updateTime();
        mTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager manager = getFragmentManager();
                TimePickerFragment dialog = TimePickerFragment.newInstance(mCrime.getDate());
                dialog.setTargetFragment(CrimeFragment.this, REQUEST_TIME);
                dialog.show(manager, DIALOG_TIME);
            }
        });

        mSolvedCheckBox = (CheckBox) v.findViewById(R.id.crime_solved);
        mSolvedCheckBox.setChecked(mCrime.isSolved());
        mSolvedCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCrime.setSolved(isChecked);
            }
        });

        mReportButton = (Button) v.findViewById(R.id.crime_report);
        mReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, getCrimeReport());
                i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject));
                i = Intent.createChooser(i, getString(R.string.send_report));
                startActivity(i);*/
                ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText(getCrimeReport())
                        .setSubject(getString(R.string.crime_report_subject))
                        .setChooserTitle(R.string.send_report)
                        .startChooser();


            }
        });

        final Intent pickContact = new Intent(Intent.ACTION_PICK,
                ContactsContract.Contacts.CONTENT_URI);
        mSuspectButton = (Button) v.findViewById(R.id.crime_suspect);
        mSuspectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(pickContact, REQUEST_CONTACT);
            }
        });


        mCallSuspectButton = (Button) v.findViewById(R.id.call_suspect);
        mCallSuspectButton.setEnabled(false);
        mCallSuspectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ContextCompat.checkSelfPermission(getActivity(),
                        Manifest.permission.READ_CONTACTS)
                        != PackageManager.PERMISSION_GRANTED){

                    Log.i(TAG, "Contact permissions has NOT been granted. Requesting permissions.");
                    requestContactsPermissions();
                }else {
                    Log.i(TAG,
                            "Contact permissions have already been granted. Displaying contact details.");

                    callSuspect();
                }
            }
        });

        if(mCrime.getSuspect() != null){
            mSuspectButton.setText(mCrime.getSuspect());
            updateCallSuspectButton();
        }

        PackageManager packageManager = getActivity().getPackageManager();
        if(packageManager.resolveActivity(pickContact,
                PackageManager.MATCH_DEFAULT_ONLY) == null){
            mSuspectButton.setEnabled(false);
        }

        mPhotoButton = (ImageButton) v.findViewById(R.id.crime_camera);

        final Intent captureImage = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        boolean canTakePhoto = mPhotoFile != null &&
                captureImage.resolveActivity(packageManager) != null;
        mPhotoButton.setEnabled(canTakePhoto);

        if(canTakePhoto){
            Uri uri = Uri.fromFile(mPhotoFile);
            captureImage.putExtra(MediaStore.EXTRA_OUTPUT,
                    uri);
        }

        mPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(captureImage, REQUEST_PHOTO);
            }
        });

        mPhotoView = (ImageView) v.findViewById(R.id.crime_photo);
        mPhotoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mPhotoFile!= null && mPhotoFile.exists())
                {
                    FragmentManager fragmentManager = getFragmentManager();

                    SuspectImageFragment.newInstance(mPhotoFile).show(fragmentManager, DIALOG_SUSPECT);
                }
            }
        });
        try {
            updatePhotoView();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return v;
    }

    private void requestContactsPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                Manifest.permission.READ_CONTACTS)) {

            Log.i(TAG,
                    "Displaying contacts permission rationale to provide additional context.");

            Toast.makeText(getActivity(), "Permission Available", Toast.LENGTH_SHORT);
            ActivityCompat.requestPermissions(getActivity(), PERMISSIONS_CONTACT, REQUEST_CONTACT);
        }else {
            ActivityCompat.requestPermissions(getActivity(), PERMISSIONS_CONTACT, REQUEST_CONTACT);
        }
    }

    private void callSuspect() {
        Log.i(TAG, "call suspect started");
        Uri contentUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;

        String selectClause = ContactsContract.CommonDataKinds.Phone._ID + " = ?";

        String[] fields = {ContactsContract.CommonDataKinds.Phone.NUMBER};
        String[] selectParams = {Long.toString(mCrime.getContactId())};

        Cursor cursor = getActivity().getContentResolver().query(contentUri, fields, selectClause, selectParams, null);


        if(cursor != null && cursor.getCount() > 0)
        {
            try
            {
                cursor.moveToFirst();

                String number = cursor.getString(0);

                Uri phoneNumber = Uri.parse("tel:" + number);

                Intent intent = new Intent(Intent.ACTION_DIAL, phoneNumber);

                startActivity(intent);

                Log.i(TAG, "intent started");
            }
            finally
            {
                cursor.close();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        CrimeLab.get(getActivity()).updateCrime(mCrime);
    }

    private void updateTime() {
        mTimeButton.setText(DateFormat.format("h:mm a", mCrime.getDate()));
    }

    private void updateDate() {
        mDateButton.setText(DateFormat.format("EEEE,  MMM dd, yyyy", mCrime.getDate()));
    }

    private String getCrimeReport(){
        String solvedString = null;
        if(mCrime.isSolved()){
            solvedString = getString(R.string.crime_report_solved);
        }else{
            solvedString = getString(R.string.crime_report_unsolved);
        }

        String dateFormat = "EEE, MMM dd";
        String dataString = DateFormat.format(dateFormat, mCrime.getDate()).toString();

        String suspect = mCrime.getSuspect();
        if(suspect == null){
            suspect = getString(R.string.crime_report_no_suspect);
        }else{
            suspect = getString(R.string.crime_report_suspect, suspect);
        }

        String report = getString(R.string.crime_report, mCrime.getTitle(), dataString,
                solvedString, suspect);

        return report;
    }

    private void updatePhotoView() throws IOException {
        if (mPhotoFile == null || !mPhotoFile.exists()){
            mPhotoView.setImageDrawable(null);
        }else {
            Bitmap bitmap = PictureUtils.getScaledBitmap(
                    mPhotoFile.getPath(), getActivity());
            mPhotoView.setImageBitmap(bitmap);
        }
    }

    @Override 
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != Activity.RESULT_OK){
            return;
        }


        if (requestCode == REQUEST_DATE){
            Date date = (Date) data
                    .getSerializableExtra(DatePickerFragment.EXTRA_DATE);
            mCrime.setDate(date);
            updateDate();
        }else if (requestCode == REQUEST_TIME){
            Date date = (Date) data
                    .getSerializableExtra(DatePickerFragment.EXTRA_DATE);
            mCrime.setDate(date);
            updateTime();
        }else if(requestCode == REQUEST_CONTACT
                && data != null){
            Uri contactUri = data.getData();
            // Specify which fields you want your query to return
            //values for
            String[] queryFields = new String[]{
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts._ID
            };
            // Perform your query - the contactUri is like a "where"
            //clause here
            Cursor c = getActivity().getContentResolver()
                    .query(contactUri, queryFields, null, null, null);

            try{
                // Double-check that you actually got results
                if(c.getCount() == 0){
                    return;
                }

                // Pull out the first column of the first row of data -
                // that is your suspect's name.
                c.moveToFirst();
                String suspect = c.getString(0);
                long contactId = c.getLong(1);
                mCrime.setSuspect(suspect);
                mCrime.setContactId(contactId);
                mSuspectButton.setText(suspect);

                updateCallSuspectButton(); //enables the button and changes its text

            }finally {
                c.close();
            }
        }else  if (requestCode == REQUEST_PHOTO){
            try {
                updatePhotoView();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void updateCallSuspectButton() {
        mCallSuspectButton.setEnabled(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_crime, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_item_delete_crime:
                CrimeLab.get(getActivity()).deleteCrime(mCrime);
                getActivity().finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CONTACT) {
            Log.i(TAG, "Received response for contact permissions request.");

            // We have requested multiple permissions for contacts, so all of them need to be
            // checked.
            /*if (PermissionUtil.verifyPermissions(grantResults)) {
                // All required permissions have been granted, display contacts fragment.
                Snackbar.make(mLayout, R.string.permision_available_contacts,
                        Snackbar.LENGTH_SHORT)
                        .show();
            } else {
                Log.i(TAG, "Contacts permissions were NOT granted.");
                Snackbar.make(mLayout, R.string.permissions_not_granted,
                        Snackbar.LENGTH_SHORT)
                        .show();
            }*/

        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
