package org.nypl.simplified.cardcreator.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.json.JSONException;
import org.nypl.simplified.prefs.Prefs;
import org.nypl.simplified.cardcreator.R;
import org.nypl.simplified.cardcreator.model.Address;
import org.nypl.simplified.cardcreator.model.AddressResponse;

/**
 * A simple {@link Fragment} subclass.
 */
public class WorkAddressConfirmFragment extends Fragment {

    private static final String ARG_PARAM2 = "response";
    private AddressResponse response;
    private Prefs prefs;

    /**
     *
     */
    public WorkAddressConfirmFragment() {
        // Required empty public constructor
    }


    /**
     * @param in_response address response
     * @return address confirmation fragment
     */
    public  WorkAddressConfirmFragment newInstance(final AddressResponse in_response) {
        final WorkAddressConfirmFragment fragment = new WorkAddressConfirmFragment();
        final Bundle args = new Bundle();
        args.putSerializable(ARG_PARAM2, in_response);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(final Bundle state) {
        super.onCreate(state);
        if (getArguments() != null) {
            this.response = (AddressResponse) getArguments().getSerializable(ARG_PARAM2);
        }
        this.prefs = new Prefs(getContext());
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle state) {
        // Inflate the layout for this fragment
        final View root_view = inflater.inflate(R.layout.fragment_work_address_confirm, container, false);

        if (this.prefs.getBoolean(getResources().getString(R.string.WORK_IN_NY_DATA_KEY))) {
            ((TextView) root_view.findViewById(android.R.id.title)).setText("Confirm Work Address");
        } else {
            ((TextView) root_view.findViewById(android.R.id.title)).setText("Confirm School Address");
        }

        ((TextView) root_view.findViewById(android.R.id.text1)).setText("Tap confirm if this is your correct address or previous to change your entry");

        final LinearLayout linear_layout = (LinearLayout) root_view.findViewById(R.id.address_list);

        int addresslength = 1;
        if (this.response.getAddresses() != null)
        {
            addresslength = this.response.getAddresses().length();
        }

        final RadioButton[] rb = new RadioButton[addresslength];
        final TextView title = new TextView(getContext());
        final TextView space = new TextView(getContext());
        space.setText("\n");

        final RadioGroup rg = new RadioGroup(getContext());
        rg.setOrientation(RadioGroup.VERTICAL);
        rg.addView(title);

        {
            if (this.response.getType().equals("valid-address"))
            {
                title.setText("\nYour Address:\n");
                final Address alternate_address = this.response.getAddress();

                final int final_index = 0;

                final StringBuilder work = new StringBuilder();
                work.append(alternate_address.getLine_1() + "\n");
                if (!alternate_address.getLine_2().isEmpty()) {
                    work.append(alternate_address.getLine_2() + "\n");
                }
                work.append(alternate_address.getCity() + "\n");
                work.append(alternate_address.getState() + " ");
                work.append(alternate_address.getZip());

                rb[final_index] = new RadioButton(getContext());
                rg.addView(rb[final_index]);
                rb[final_index].setText(work.toString());
                if (this.prefs.getInt(getResources().getString(R.string.SELECTED_WORK_ADDRESS)) == final_index) {
                    rb[final_index].setChecked(true);
                    this.prefs.putString(getResources().getString(R.string.STREET1_W_DATA_KEY), alternate_address.getLine_1());
                    this.prefs.putString(getResources().getString(R.string.STREET2_W_DATA_KEY), alternate_address.getLine_2());
                    this.prefs.putString(getResources().getString(R.string.CITY_W_DATA_KEY), alternate_address.getCity());
                    this.prefs.putString(getResources().getString(R.string.STATE_W_DATA_KEY), alternate_address.getState());
                    this.prefs.putString(getResources().getString(R.string.ZIP_W_DATA_KEY), alternate_address.getZip());
                }

                rb[final_index].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {

                        WorkAddressConfirmFragment.this.prefs.putInt(getResources().getString(R.string.SELECTED_WORK_ADDRESS), final_index);
                        WorkAddressConfirmFragment.this.prefs.putString(getResources().getString(R.string.STREET1_W_DATA_KEY), alternate_address.getLine_1());
                        WorkAddressConfirmFragment.this.prefs.putString(getResources().getString(R.string.STREET2_W_DATA_KEY), alternate_address.getLine_2());
                        WorkAddressConfirmFragment.this.prefs.putString(getResources().getString(R.string.CITY_W_DATA_KEY), alternate_address.getCity());
                        WorkAddressConfirmFragment.this.prefs.putString(getResources().getString(R.string.STATE_W_DATA_KEY), alternate_address.getState());
                        WorkAddressConfirmFragment.this.prefs.putString(getResources().getString(R.string.ZIP_W_DATA_KEY), alternate_address.getZip());
                    }
                });

            }

            if (this.response.getType().equals("alternate-addresses")) {
                title.setText("\nAlternate Addresses:\n");
                for (int index = 0; index < this.response.getAddresses().length(); ++index) {

                    try {
                        final Address alternate_address = new Address(this.response.getAddresses().getJSONObject(index).getJSONObject("address"));

                        final int final_index = index;

                        final StringBuilder work = new StringBuilder();
                        work.append(alternate_address.getLine_1() + "\n");
                        if (!alternate_address.getLine_2().isEmpty()) {
                            work.append(alternate_address.getLine_2() + "\n");
                        }
                        work.append(alternate_address.getCity() + "\n");
                        work.append(alternate_address.getState() + " ");
                        work.append(alternate_address.getZip());

                        rb[final_index] = new RadioButton(getContext());
                        rg.addView(rb[final_index]);
                        rb[final_index].setText(work.toString());
                        if (this.prefs.getInt(getResources().getString(R.string.SELECTED_WORK_ADDRESS)) == final_index) {
                            rb[final_index].setChecked(true);
                            this.prefs.putString(getResources().getString(R.string.STREET1_W_DATA_KEY), alternate_address.getLine_1());
                            this.prefs.putString(getResources().getString(R.string.STREET2_W_DATA_KEY), alternate_address.getLine_2());
                            this.prefs.putString(getResources().getString(R.string.CITY_W_DATA_KEY), alternate_address.getCity());
                            this.prefs.putString(getResources().getString(R.string.STATE_W_DATA_KEY), alternate_address.getState());
                            this.prefs.putString(getResources().getString(R.string.ZIP_W_DATA_KEY), alternate_address.getZip());

                        }

                        rb[final_index].setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(final View v) {

                                WorkAddressConfirmFragment.this.prefs.putInt(getResources().getString(R.string.SELECTED_WORK_ADDRESS), final_index);
                                WorkAddressConfirmFragment.this.prefs.putString(getResources().getString(R.string.STREET1_W_DATA_KEY), alternate_address.getLine_1());
                                WorkAddressConfirmFragment.this.prefs.putString(getResources().getString(R.string.STREET2_W_DATA_KEY), alternate_address.getLine_2());
                                WorkAddressConfirmFragment.this.prefs.putString(getResources().getString(R.string.CITY_W_DATA_KEY), alternate_address.getCity());
                                WorkAddressConfirmFragment.this.prefs.putString(getResources().getString(R.string.STATE_W_DATA_KEY), alternate_address.getState());
                                WorkAddressConfirmFragment.this.prefs.putString(getResources().getString(R.string.ZIP_W_DATA_KEY), alternate_address.getZip());

                            }
                        });


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }

        }

        linear_layout.addView(rg);

        return root_view;
    }

}
