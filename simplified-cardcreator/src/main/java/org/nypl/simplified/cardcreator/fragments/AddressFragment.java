package org.nypl.simplified.cardcreator.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import org.nypl.simplified.prefs.Prefs;
import org.nypl.simplified.cardcreator.R;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AddressFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AddressFragment extends Fragment {


    private Prefs prefs;

    /**
     *
     */
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
        final AddressFragment fragment = new AddressFragment();
        final Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(final Bundle state) {
        super.onCreate(state);

        this.prefs = new Prefs(getContext());
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle state) {
        // Inflate the layout for this fragment

        final View root_view = inflater.inflate(R.layout.fragment_address, container, false);
        ((TextView) root_view.findViewById(android.R.id.title)).setText("Out-of-State Address");
        ((TextView) root_view.findViewById(android.R.id.text1)).setText("Since you do not live in New York, "
          + "you must work or attend school in New York to qualify for a library card.");

        ((RadioButton) root_view.findViewById(R.id.liveInNYC)).setChecked(this.prefs.getBoolean(getResources().getString(R.string.LIVE_IN_NY_DATA_KEY)));
        ((RadioButton) root_view.findViewById(R.id.workInNYC)).setChecked(this.prefs.getBoolean(getResources().getString(R.string.WORK_IN_NY_DATA_KEY)));
        ((RadioButton) root_view.findViewById(R.id.goToSchoolInNYC)).setChecked(this.prefs.getBoolean(getResources().getString(R.string.SCHOOL_IN_NY_DATA_KEY)));


        return root_view;
    }


}
