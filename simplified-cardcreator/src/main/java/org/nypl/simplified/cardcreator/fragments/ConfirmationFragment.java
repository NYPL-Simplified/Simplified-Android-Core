package org.nypl.simplified.cardcreator.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.nypl.simplified.cardcreator.R;
import org.nypl.simplified.prefs.Prefs;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ConfirmationFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ConfirmationFragment extends Fragment {


    private TextView barcode;
    private TextView username;
    private TextView pin;
    private Prefs prefs;

    /**
     * @return barcode
     */
    public TextView getBarcode() {
        return this.barcode;
    }

    /**
     * @return username
     */
    public TextView getUsername() {
        return this.username;
    }

    /**
     * @return pin
     */
    public TextView getPin() {
        return this.pin;
    }

    /**
     *
     */
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
        final ConfirmationFragment fragment = new ConfirmationFragment();
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
        final View root_view = inflater.inflate(R.layout.fragment_confimration, container, false);
        ((TextView) root_view.findViewById(android.R.id.title)).setText("Your Card Information");
        ((TextView) root_view.findViewById(android.R.id.text1)).setText(this.prefs.getString(getResources().getString(R.string.MESSAGE_DATA_KEY)));

        this.barcode = ((TextView) root_view.findViewById(R.id.barcode));
        this.username = ((TextView) root_view.findViewById(R.id.username));
        this.pin = ((TextView) root_view.findViewById(R.id.pin));

        this.barcode.setText(this.prefs.getString(getResources().getString(R.string.BARCODE_DATA_KEY)));
        this.username.setText(this.prefs.getString(getResources().getString(R.string.USERNAME_DATA_KEY)));
        this.pin.setText(this.prefs.getString(getResources().getString(R.string.PIN_DATA_KEY)));

        return root_view;
    }

}
