package org.nypl.simplified.cardcreator.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import org.nypl.simplified.cardcreator.Constants;
import org.nypl.simplified.cardcreator.Prefs;
import org.nypl.simplified.cardcreator.R;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AddressFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AddressFragment extends Fragment {


    private Prefs mPrefs;

    public AddressFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment AgeFragment.
     */

    public  AddressFragment newInstance() {
        AddressFragment fragment = new AddressFragment();
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

        View rootView = inflater.inflate(R.layout.fragment_address, container, false);
        ((TextView) rootView.findViewById(android.R.id.title)).setText("Out-of-State Address");
        ((TextView) rootView.findViewById(android.R.id.text1)).setText("Since you do not live in New York, you must work or attend school in New York to qualify for a library card.");


        ((RadioButton) rootView.findViewById(R.id.liveInNYC)).setChecked(mPrefs.getBoolean(Constants.LIVE_IN_NY_DATA_KEY));
        ((RadioButton) rootView.findViewById(R.id.workInNYC)).setChecked(mPrefs.getBoolean(Constants.WORK_IN_NY_DATA_KEY));
        ((RadioButton) rootView.findViewById(R.id.goToSchoolInNYC)).setChecked(mPrefs.getBoolean(Constants.SCHOOL_IN_NY_DATA_KEY));


        return rootView;
    }


}
