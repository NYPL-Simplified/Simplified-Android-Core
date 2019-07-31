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

import org.nypl.simplified.prefs.Prefs;
import org.nypl.simplified.cardcreator.R;
import org.nypl.simplified.cardcreator.listener.InputListenerType;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CredentialsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CredentialsFragment extends Fragment {


    private EditText username;
    private EditText pin;
    private Prefs prefs;

    /**
     * @return username
     */
    public EditText getUsername() {
        return this.username;
    }

    /**
     * @return pin
     */
    public EditText getPin() {
        return this.pin;
    }

    /**
     *
     */
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
        final CredentialsFragment fragment = new CredentialsFragment();
        final Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(final Bundle state) {
        super.onCreate(state);
        this.prefs = new Prefs(getActivity());

    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle state) {
        // Inflate the layout for this fragment
        final View root_view = inflater.inflate(R.layout.fragment_credentials, container, false);

        ((TextView) root_view.findViewById(android.R.id.title)).setText("Account Information");
        ((TextView) root_view.findViewById(android.R.id.text2)).setText("Usernames must be 5–25 letters or numbers only. PINs must be four digits.");


        this.username = ((EditText) root_view.findViewById(R.id.username));
        this.pin = ((EditText) root_view.findViewById(R.id.pin));

        this.username.setText(this.prefs.getString(getResources().getString(R.string.USERNAME_DATA_KEY)));
        this.pin.setText(this.prefs.getString(getResources().getString(R.string.PIN_DATA_KEY)));


        if (this.isCompleted()) {
            ((InputListenerType) getActivity()).onInputComplete();
        }
        else {
            ((InputListenerType) getActivity()).onInputInComplete();
        }

        this.username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
            }

            @Override
            public void afterTextChanged(final Editable s) {
                CredentialsFragment.this.prefs.putString(getResources().getString(R.string.USERNAME_DATA_KEY), (s != null) ? s.toString() : null);

                if (CredentialsFragment.this.isCompleted()) {
                    ((InputListenerType) getActivity()).onInputComplete();
                }
                else {
                    ((InputListenerType) getActivity()).onInputInComplete();
                }
            }
        });

        this.pin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
            }

            @Override
            public void afterTextChanged(final Editable s) {
                CredentialsFragment.this.prefs.putString(getResources().getString(R.string.PIN_DATA_KEY), (s != null) ? s.toString() : null);

                if (CredentialsFragment.this.isCompleted()) {
                    ((InputListenerType) getActivity()).onInputComplete();
                }
                else {
                    ((InputListenerType) getActivity()).onInputInComplete();
                }
            }
        });

        return root_view;
    }

    /**
     * @return all required fields completed
     */
    public boolean isCompleted() {
        return !TextUtils.isEmpty(this.prefs.getString(getResources().getString(R.string.USERNAME_DATA_KEY)))
                && !TextUtils.isEmpty(this.prefs.getString(getResources().getString(R.string.PIN_DATA_KEY)));
    }


}

