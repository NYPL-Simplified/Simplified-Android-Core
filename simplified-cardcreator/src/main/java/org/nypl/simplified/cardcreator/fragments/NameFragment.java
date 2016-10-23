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

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NameFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NameFragment extends Fragment {

    public EditText name;
    public EditText email;
    private Prefs mPrefs;


    public NameFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment NameFragment.
     */
    public  NameFragment newInstance() {
        NameFragment fragment = new NameFragment();
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
        View rootView = inflater.inflate(R.layout.fragment_name, container, false);
        ((TextView) rootView.findViewById(android.R.id.title)).setText("Personal Information");

        name = (EditText) rootView.findViewById(R.id.name);
        email = (EditText) rootView.findViewById(R.id.email);
        name.setText(mPrefs.getString(Constants.NAME_DATA_KEY));
        email.setText(mPrefs.getString(Constants.EMAIL_DATA_KEY));

        if (isCompleted()) {
            ((InputListenerType) getActivity()).onInputComplete();
        }
        else {
            ((InputListenerType) getActivity()).onInputInComplete();
        }

        name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mPrefs.putString(Constants.NAME_DATA_KEY, (s != null) ? s.toString() : null);

                if (isCompleted()) {
                    ((InputListenerType) getActivity()).onInputComplete();
                }
                else {
                    ((InputListenerType) getActivity()).onInputInComplete();
                }
            }
        });
        email.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mPrefs.putString(Constants.EMAIL_DATA_KEY, (s != null) ? s.toString() : null);

                if (isCompleted()) {
                    if (isEmailAddress()) {
                        ((InputListenerType) getActivity()).onInputComplete();
                        email.setTextAppearance(getContext(), R.style.WizardPageSuccess);

                    }
                    else {
                        ((InputListenerType) getActivity()).onInputInComplete();
                        email.setTextAppearance(getContext(), R.style.WizardPageError);
                    }
                }
                else {
                    ((InputListenerType) getActivity()).onInputInComplete();
                }
            }
        });

        return rootView;
    }

    public boolean isEmailAddress() {

        final Pattern p = Pattern.compile(".+@.+\\.[a-z]+");
//        this.email.setText(this.email.getText().toString().toLowerCase());
        final Matcher m = p.matcher(this.email.getText());
        return m.matches();

    }

    public boolean isCompleted() {
        return !TextUtils.isEmpty(mPrefs.getString(Constants.NAME_DATA_KEY))
                && !TextUtils.isEmpty(mPrefs.getString(Constants.EMAIL_DATA_KEY));
    }

}
