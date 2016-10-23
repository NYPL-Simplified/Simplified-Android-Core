package org.nypl.simplified.cardcreator.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.nypl.simplified.cardcreator.Constants;
import org.nypl.simplified.cardcreator.Prefs;
import org.nypl.simplified.cardcreator.R;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ReviewFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ReviewFragment extends Fragment {


    public TextView username;
    public TextView pin;
    public TextView name;
    public TextView email;
    public TextView homeAddress;
    public TextView workSchoolAddress;
    public TextView labelWorkAddress;
    public TextView labelSchoolAddress;
    private Prefs mPrefs;


    public ReviewFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ConfimrationFragment.
     */
    public  ReviewFragment newInstance() {
        ReviewFragment fragment = new ReviewFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
        mPrefs = new Prefs(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_review, container, false);
        ((TextView) rootView.findViewById(android.R.id.title)).setText("Review");
        ((TextView) rootView.findViewById(android.R.id.text1)).setText(mPrefs.getString(Constants.CARD_TYPE_DATA_KEY));

        username = ((TextView) rootView.findViewById(R.id.username));
        pin = ((TextView) rootView.findViewById(R.id.pin));
        email = ((TextView) rootView.findViewById(R.id.email));
        name = ((TextView) rootView.findViewById(R.id.name));
        homeAddress = ((TextView) rootView.findViewById(R.id.address_home));
        workSchoolAddress = ((TextView) rootView.findViewById(R.id.address_work_school));
        labelSchoolAddress = ((TextView) rootView.findViewById(R.id.Label_address_school));
        labelWorkAddress = ((TextView) rootView.findViewById(R.id.label_address_work));


        if (mPrefs.getBoolean(Constants.WORK_IN_NY_DATA_KEY)) {
            labelWorkAddress.setVisibility(View.VISIBLE);
        } else {
            labelWorkAddress.setVisibility(View.GONE);
        }
        if (mPrefs.getBoolean(Constants.SCHOOL_IN_NY_DATA_KEY)) {
            labelSchoolAddress.setVisibility(View.VISIBLE);
        } else {
            labelSchoolAddress.setVisibility(View.GONE);
        }

        if (mPrefs.getBoolean(Constants.SCHOOL_IN_NY_DATA_KEY) || mPrefs.getBoolean(Constants.WORK_IN_NY_DATA_KEY)) {
            StringBuilder work = new StringBuilder();
            work.append(mPrefs.getString(Constants.STREET1_W_DATA_KEY) + "\n");
            if (mPrefs.getString(Constants.STREET2_W_DATA_KEY) != null) {
                work.append(mPrefs.getString(Constants.STREET2_W_DATA_KEY) + "\n");
            }
            work.append(mPrefs.getString(Constants.CITY_W_DATA_KEY) + "\n");
            work.append(mPrefs.getString(Constants.STATE_W_DATA_KEY) + "\n");
            work.append(mPrefs.getString(Constants.ZIP_W_DATA_KEY) + "\n");

            workSchoolAddress.setText(work.toString());

        }

        username.setText(mPrefs.getString(Constants.USERNAME_DATA_KEY));
        pin.setText(mPrefs.getString(Constants.PIN_DATA_KEY));
        name.setText(mPrefs.getString(Constants.NAME_DATA_KEY));
        email.setText(mPrefs.getString(Constants.EMAIL_DATA_KEY));

        StringBuilder home = new StringBuilder();
        home.append(mPrefs.getString(Constants.STREET1_H_DATA_KEY) + "\n");
        if (mPrefs.getString(Constants.STREET2_H_DATA_KEY) != null) {
            home.append(mPrefs.getString(Constants.STREET2_H_DATA_KEY) + "\n");
        }
        home.append(mPrefs.getString(Constants.CITY_H_DATA_KEY) + "\n");
        home.append(mPrefs.getString(Constants.STATE_H_DATA_KEY) + "\n");
        home.append(mPrefs.getString(Constants.ZIP_H_DATA_KEY) + "\n");

        homeAddress.setText(home.toString());


        return rootView;
    }

}
