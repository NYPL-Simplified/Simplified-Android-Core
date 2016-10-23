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
 * Use the {@link CredentialsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CredentialsFragment extends Fragment {


    public EditText username;
    public EditText pin;
    private Prefs mPrefs;

    public CredentialsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment CredentialsFragment.
     */
    public  CredentialsFragment newInstance() {
        CredentialsFragment fragment = new CredentialsFragment();
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
        View rootView = inflater.inflate(R.layout.fragment_credentials, container, false);

        ((TextView) rootView.findViewById(android.R.id.title)).setText("Account Information");
        ((TextView) rootView.findViewById(android.R.id.text2)).setText("Usernames must be 5–25 letters or numbers only. PINs must be four digits.");


        username = ((EditText) rootView.findViewById(R.id.username));
        pin = ((EditText) rootView.findViewById(R.id.pin));

        username.setText(mPrefs.getString(Constants.USERNAME_DATA_KEY));
        pin.setText(mPrefs.getString(Constants.PIN_DATA_KEY));


        if (isCompleted()) {
            ((InputListenerType) getActivity()).onInputComplete();
        }
        else {
            ((InputListenerType) getActivity()).onInputInComplete();
        }

        username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mPrefs.putString(Constants.USERNAME_DATA_KEY, (s != null) ? s.toString() : null);

                if (isCompleted()) {
                    ((InputListenerType) getActivity()).onInputComplete();
                }
                else {
                    ((InputListenerType) getActivity()).onInputInComplete();
                }
            }
        });

        pin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mPrefs.putString(Constants.PIN_DATA_KEY, (s != null) ? s.toString() : null);

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
        return !TextUtils.isEmpty(mPrefs.getString(Constants.USERNAME_DATA_KEY))
                && !TextUtils.isEmpty(mPrefs.getString(Constants.PIN_DATA_KEY));
    }


}

