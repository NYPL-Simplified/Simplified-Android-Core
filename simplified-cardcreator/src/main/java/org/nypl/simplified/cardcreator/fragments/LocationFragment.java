package org.nypl.simplified.cardcreator.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.nypl.simplified.cardcreator.R;
import org.nypl.simplified.cardcreator.listener.InputListenerType;
import org.nypl.simplified.cardcreator.listener.LocationListenerType;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LocationFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LocationFragment extends Fragment {


    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM2 = "region";
    private static final String ARG_PARAM3 = "status";
    public ProgressBar mProgressBar;
    public Button mFetchAddressButton;
    private String mRegion;
    private String mStatus;

    public LocationFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param region Parameter 1.
     * @return A new instance of fragment LocationFragment.
     */

    public  LocationFragment newInstance(String region, String status) {
        LocationFragment fragment = new LocationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM2, region);
        args.putString(ARG_PARAM3, status);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mRegion = getArguments().getString(ARG_PARAM2);
            mStatus = getArguments().getString(ARG_PARAM3);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_location, container, false);
        ((TextView) rootView.findViewById(android.R.id.title)).setText("Location Check");
        ((TextView) rootView.findViewById(android.R.id.text1)).setText(mStatus);

        ((EditText) rootView.findViewById(R.id.region)).setText(mRegion);


        mProgressBar = (ProgressBar) rootView.findViewById(R.id.progress_bar);
        mFetchAddressButton = (Button) rootView.findViewById(R.id.fetch_address_button);


        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((LocationListenerType) getActivity()).onCheckLocation();

    }
}
