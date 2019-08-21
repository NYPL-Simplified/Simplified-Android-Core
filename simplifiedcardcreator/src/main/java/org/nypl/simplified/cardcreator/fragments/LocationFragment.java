package org.nypl.simplified.cardcreator.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import org.nypl.simplified.cardcreator.R;
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
    private String region;
    private String status;

    /**
     *
     */
    public LocationFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param in_region region param
     * @param in_status status param
     * @return A new instance of fragment LocationFragment.
     */

    public  LocationFragment newInstance(final String in_region, final String in_status) {
        final LocationFragment fragment = new LocationFragment();
        final Bundle args = new Bundle();
        args.putString(ARG_PARAM2, in_region);
        args.putString(ARG_PARAM3, in_status);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(final Bundle state) {
        super.onCreate(state);
        if (getArguments() != null) {
            this.region = getArguments().getString(ARG_PARAM2);
            this.status = getArguments().getString(ARG_PARAM3);
        }

    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle state) {
        // Inflate the layout for this fragment
        final View root_view = inflater.inflate(R.layout.fragment_location, container, false);
        ((TextView) root_view.findViewById(android.R.id.title)).setText("Location Check");
        ((TextView) root_view.findViewById(android.R.id.text1)).setText(this.status);
        ((EditText) root_view.findViewById(R.id.region)).setText(this.region);

        return root_view;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((LocationListenerType) getActivity()).onCheckLocation();

    }
}
