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
 * Use the {@link ConfirmationFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ConfirmationFragment extends Fragment {


    public TextView barcode;
    public TextView username;
    public TextView pin;
    private Prefs mPrefs;


    public ConfirmationFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ConfimrationFragment.
     */
    public  ConfirmationFragment newInstance() {
        ConfirmationFragment fragment = new ConfirmationFragment();
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
        View rootView = inflater.inflate(R.layout.fragment_confimration, container, false);
        ((TextView) rootView.findViewById(android.R.id.title)).setText("Your Card Information");
        ((TextView) rootView.findViewById(android.R.id.text1)).setText(mPrefs.getString(Constants.MESSAGE_DATA_KEY));

        barcode = ((TextView) rootView.findViewById(R.id.barcode));
        username = ((TextView) rootView.findViewById(R.id.username));
        pin = ((TextView) rootView.findViewById(R.id.pin));

        barcode.setText(mPrefs.getString(Constants.BARCODE_DATA_KEY));
        username.setText(mPrefs.getString(Constants.USERNAME_DATA_KEY));
        pin.setText(mPrefs.getString(Constants.PIN_DATA_KEY));

        return rootView;
    }

}
