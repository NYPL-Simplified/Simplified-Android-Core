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
import org.nypl.simplified.cardcreator.Constants;
import org.nypl.simplified.cardcreator.Prefs;
import org.nypl.simplified.cardcreator.R;
import org.nypl.simplified.cardcreator.model.Address;
import org.nypl.simplified.cardcreator.model.AddressResponse;

/**
 * A simple {@link Fragment} subclass.
 */
public class WorkAddressConfirmFragment extends Fragment {

    private static final String ARG_PARAM2 = "response";
    public AddressResponse mResponse;
    private Prefs mPrefs;

    public WorkAddressConfirmFragment() {
        // Required empty public constructor
    }


    public  WorkAddressConfirmFragment newInstance(AddressResponse response) {
        WorkAddressConfirmFragment fragment = new WorkAddressConfirmFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PARAM2, response);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mResponse = (AddressResponse) getArguments().getSerializable(ARG_PARAM2);
        }
        mPrefs = new Prefs(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_work_address_confirm, container, false);

        if (mPrefs.getBoolean(Constants.WORK_IN_NY_DATA_KEY)) {
            ((TextView) rootView.findViewById(android.R.id.title)).setText("Confirm Work Address");
        } else {
            ((TextView) rootView.findViewById(android.R.id.title)).setText("Confirm School Address");
        }

        ((TextView) rootView.findViewById(android.R.id.text1)).setText("Tap confirm if this is your correct address or previous to change your entry");

        LinearLayout mLinearLayout = (LinearLayout) rootView.findViewById(R.id.address_list);

        int addresslength = 1;
        if (mResponse.addresses != null)
        {
            addresslength = mResponse.addresses.length();
        }

        final RadioButton[] rb = new RadioButton[addresslength];

        TextView title = new TextView(getContext());

        TextView space = new TextView(getContext());
        space.setText("\n");

        RadioGroup rg = new RadioGroup(getContext());

        rg.setOrientation(RadioGroup.VERTICAL);

        rg.addView(title);


        {
            if (mResponse.type.equals("valid-address"))
            {
                title.setText("\nYour Address:\n");
                final Address alternate_address = mResponse.address;

                final int finalIndex = 0;


                StringBuilder work = new StringBuilder();
                work.append(alternate_address.line_1 + "\n");
                if (!alternate_address.line_2.isEmpty()) {
                    work.append(alternate_address.line_2 + "\n");
                }
                work.append(alternate_address.city + "\n");
                work.append(alternate_address.state + " ");
                work.append(alternate_address.zip);

                rb[finalIndex] = new RadioButton(getContext());
                rg.addView(rb[finalIndex]);
                rb[finalIndex].setText(work.toString());
                if (mPrefs.getInt(Constants.SELECTED_WORK_ADDRESS) == finalIndex) {
                    rb[finalIndex].setChecked(true);
                    mPrefs.putString(Constants.STREET1_W_DATA_KEY, alternate_address.line_1);
                    mPrefs.putString(Constants.STREET2_W_DATA_KEY, alternate_address.line_2);
                    mPrefs.putString(Constants.CITY_W_DATA_KEY, alternate_address.city);
                    mPrefs.putString(Constants.STATE_W_DATA_KEY, alternate_address.state);
                    mPrefs.putString(Constants.ZIP_W_DATA_KEY, alternate_address.zip);

                }


                rb[finalIndex].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        mPrefs.putInt(Constants.SELECTED_WORK_ADDRESS, finalIndex);
                        mPrefs.putString(Constants.STREET1_W_DATA_KEY, alternate_address.line_1);
                        mPrefs.putString(Constants.STREET2_W_DATA_KEY, alternate_address.line_2);
                        mPrefs.putString(Constants.CITY_W_DATA_KEY, alternate_address.city);
                        mPrefs.putString(Constants.STATE_W_DATA_KEY, alternate_address.state);
                        mPrefs.putString(Constants.ZIP_W_DATA_KEY, alternate_address.zip);
                    }
                });


            }
            if (mResponse.type.equals("alternate-addresses")) {
                title.setText("\nAlternate Addresses:\n");
                for (int index = 0; index < mResponse.addresses.length(); ++index) {

                    try {
                        final Address alternate_address = new Address(mResponse.addresses.getJSONObject(index).getJSONObject("address"));

                        final int finalIndex = index;


                        StringBuilder work = new StringBuilder();
                        work.append(alternate_address.line_1 + "\n");
                        if (!alternate_address.line_2.isEmpty()) {
                            work.append(alternate_address.line_2 + "\n");
                        }
                        work.append(alternate_address.city + "\n");
                        work.append(alternate_address.state + " ");
                        work.append(alternate_address.zip);

                        rb[finalIndex] = new RadioButton(getContext());
                        rg.addView(rb[finalIndex]);
                        rb[finalIndex].setText(work.toString());
                        if (mPrefs.getInt(Constants.SELECTED_WORK_ADDRESS) == finalIndex) {
                            rb[finalIndex].setChecked(true);
                            mPrefs.putString(Constants.STREET1_W_DATA_KEY, alternate_address.line_1);
                            mPrefs.putString(Constants.STREET2_W_DATA_KEY, alternate_address.line_2);
                            mPrefs.putString(Constants.CITY_W_DATA_KEY, alternate_address.city);
                            mPrefs.putString(Constants.STATE_W_DATA_KEY, alternate_address.state);
                            mPrefs.putString(Constants.ZIP_W_DATA_KEY, alternate_address.zip);

                        }


                        rb[finalIndex].setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                mPrefs.putInt(Constants.SELECTED_WORK_ADDRESS, finalIndex);
                                mPrefs.putString(Constants.STREET1_W_DATA_KEY, alternate_address.line_1);
                                mPrefs.putString(Constants.STREET2_W_DATA_KEY, alternate_address.line_2);
                                mPrefs.putString(Constants.CITY_W_DATA_KEY, alternate_address.city);
                                mPrefs.putString(Constants.STATE_W_DATA_KEY, alternate_address.state);
                                mPrefs.putString(Constants.ZIP_W_DATA_KEY, alternate_address.zip);
                            }
                        });


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

//                    rg.addView(space);

                }
            }



        }


        mLinearLayout.addView(rg);

        return rootView;
    }

}
