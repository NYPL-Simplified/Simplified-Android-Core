package org.nypl.simplified.cardcreator.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import org.nypl.simplified.cardcreator.Constants;
import org.nypl.simplified.cardcreator.Prefs;
import org.nypl.simplified.cardcreator.R;
import org.nypl.simplified.cardcreator.listener.InputListenerType;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WorkAddressFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WorkAddressFragment extends Fragment {


    public EditText line_1;
    public EditText line_2;
    public EditText city;
    public EditText state;
    public EditText zip;
    private Prefs mPrefs;


    public WorkAddressFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment WorkAddressFragment.
     */

    public  WorkAddressFragment newInstance() {
        WorkAddressFragment fragment = new WorkAddressFragment();
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
        View rootView = inflater.inflate(R.layout.fragment_work_address, container, false);
        if (mPrefs.getBoolean(Constants.WORK_IN_NY_DATA_KEY)) {
            ((TextView) rootView.findViewById(android.R.id.title)).setText("Work Address");
        } else {
            ((TextView) rootView.findViewById(android.R.id.title)).setText("School Address");
        }

        line_1 = ((EditText) rootView.findViewById(R.id.street1));
        line_2 = ((EditText) rootView.findViewById(R.id.street2));
        city = ((EditText) rootView.findViewById(R.id.city));
        state = ((EditText) rootView.findViewById(R.id.region));
        zip = ((EditText) rootView.findViewById(R.id.zip));


        line_1.setText(mPrefs.getString(Constants.STREET1_W_DATA_KEY));
        line_2.setText(mPrefs.getString(Constants.STREET2_W_DATA_KEY));
        city.setText(mPrefs.getString(Constants.CITY_W_DATA_KEY));
        state.setText(mPrefs.getString(Constants.STATE_W_DATA_KEY));
        zip.setText(mPrefs.getString(Constants.ZIP_W_DATA_KEY));

        if (isCompleted()) {
            ((InputListenerType) getActivity()).onInputComplete();
        }
        else {
            ((InputListenerType) getActivity()).onInputInComplete();
        }


        line_1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mPrefs.putString(Constants.STREET1_W_DATA_KEY, (s != null) ? s.toString() : null);

                if (isCompleted()) {
                    ((InputListenerType) getActivity()).onInputComplete();
                }
                else {
                    ((InputListenerType) getActivity()).onInputInComplete();
                }
            }
        });
        line_2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mPrefs.putString(Constants.STREET2_W_DATA_KEY, (s != null) ? s.toString() : null);

                if (isCompleted()) {
                    ((InputListenerType) getActivity()).onInputComplete();
                }
                else {
                    ((InputListenerType) getActivity()).onInputInComplete();
                }
            }
        });
        city.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mPrefs.putString(Constants.CITY_W_DATA_KEY, (s != null) ? s.toString() : null);

                if (isCompleted()) {
                    ((InputListenerType) getActivity()).onInputComplete();
                }
                else {
                    ((InputListenerType) getActivity()).onInputInComplete();
                }
            }
        });
        state.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mPrefs.putString(Constants.STATE_W_DATA_KEY, (s != null) ? s.toString() : null);

                if (isCompleted()) {
                    ((InputListenerType) getActivity()).onInputComplete();
                }
                else {
                    ((InputListenerType) getActivity()).onInputInComplete();
                }
            }
        });
        zip.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mPrefs.putString(Constants.ZIP_W_DATA_KEY, (s != null) ? s.toString() : null);

                if (isCompleted()) {
                    ((InputListenerType) getActivity()).onInputComplete();
                }
                else {
                    ((InputListenerType) getActivity()).onInputInComplete();
                }
            }
        });
        return rootView;
    }

    public boolean isCompleted() {
        return !TextUtils.isEmpty(mPrefs.getString(Constants.STREET1_W_DATA_KEY))
                && !TextUtils.isEmpty(mPrefs.getString(Constants.CITY_W_DATA_KEY))
                && !TextUtils.isEmpty(mPrefs.getString(Constants.STATE_W_DATA_KEY))
                && !TextUtils.isEmpty(mPrefs.getString(Constants.ZIP_W_DATA_KEY));
    }


}
