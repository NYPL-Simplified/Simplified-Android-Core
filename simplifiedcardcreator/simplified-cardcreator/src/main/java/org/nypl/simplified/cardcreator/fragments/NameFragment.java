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

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NameFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NameFragment extends Fragment {

    private EditText firstname;
    private EditText middlename;
    private EditText lastname;
    private EditText email;
    private Prefs prefs;

    /**
     * @return name
     */
    public EditText getFirstName() {
        return this.firstname;
    }

    /**
     * @return
     */
    public EditText getMiddleName() {
        return this.middlename;
    }

    /**
     * @return
     */
    public EditText getLastName() {
        return this.lastname;
    }

    /**
     * @return email
     */
    public EditText getEmail() {
        return this.email;
    }

    /**
     *
     */
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
        final NameFragment fragment = new NameFragment();
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
        final View root_view = inflater.inflate(R.layout.fragment_name, container, false);
        ((TextView) root_view.findViewById(android.R.id.title)).setText("Personal Information");

        this.firstname = (EditText) root_view.findViewById(R.id.firstname);
        this.middlename = (EditText) root_view.findViewById(R.id.middlename);
        this.lastname = (EditText) root_view.findViewById(R.id.lastname);
        this.email = (EditText) root_view.findViewById(R.id.email);

        this.firstname.setText(this.prefs.getString(getResources().getString(R.string.FIRST_NAME_DATA_KEY)));
        this.middlename.setText(this.prefs.getString(getResources().getString(R.string.MIDDLE_NAME_DATA_KEY)));
        this.lastname.setText(this.prefs.getString(getResources().getString(R.string.LAST_NAME_DATA_KEY)));
        this.email.setText(this.prefs.getString(getResources().getString(R.string.EMAIL_DATA_KEY)));

        if (this.isCompleted()) {
            ((InputListenerType) getActivity()).onInputComplete();
        }
        else {
            ((InputListenerType) getActivity()).onInputInComplete();
        }

        this.firstname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
            }

            @Override
            public void afterTextChanged(final Editable s) {
                NameFragment.this.prefs.putString(getResources().getString(R.string.FIRST_NAME_DATA_KEY), (s != null) ? s.toString() : null);

                if (NameFragment.this.isCompleted()) {
                    ((InputListenerType) getActivity()).onInputComplete();
                }
                else {
                    ((InputListenerType) getActivity()).onInputInComplete();
                }
            }
        });
        this.middlename.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
            }

            @Override
            public void afterTextChanged(final Editable s) {
                NameFragment.this.prefs.putString(getResources().getString(R.string.MIDDLE_NAME_DATA_KEY), (s != null) ? s.toString() : null);

                if (NameFragment.this.isCompleted()) {
                    ((InputListenerType) getActivity()).onInputComplete();
                }
                else {
                    ((InputListenerType) getActivity()).onInputInComplete();
                }
            }
        });
        this.lastname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
            }

            @Override
            public void afterTextChanged(final Editable s) {
                NameFragment.this.prefs.putString(getResources().getString(R.string.LAST_NAME_DATA_KEY), (s != null) ? s.toString() : null);

                if (NameFragment.this.isCompleted()) {
                    ((InputListenerType) getActivity()).onInputComplete();
                }
                else {
                    ((InputListenerType) getActivity()).onInputInComplete();
                }
            }
        });
        this.email.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
            }

            @Override
            public void afterTextChanged(final Editable s) {
                NameFragment.this.prefs.putString(getResources().getString(R.string.EMAIL_DATA_KEY), (s != null) ? s.toString() : null);

                if (NameFragment.this.isCompleted()) {
                    if (NameFragment.this.isEmailAddress()) {
                        ((InputListenerType) getActivity()).onInputComplete();
                        NameFragment.this.email.setTextAppearance(getContext(), R.style.WizardPageSuccess);

                    }
                    else {
                        ((InputListenerType) getActivity()).onInputInComplete();
                        NameFragment.this.email.setTextAppearance(getContext(), R.style.WizardPageError);
                    }
                }
                else {
                    ((InputListenerType) getActivity()).onInputInComplete();
                }
            }
        });

        return root_view;
    }

    /**
     * @return validate if email
     */
    public boolean isEmailAddress() {

        final Pattern p = Pattern.compile(".+@.+\\.[a-z]+");
        final Matcher m = p.matcher(this.email.getText());
        return m.matches();

    }

    /**
     * @return validate if all required field are completed
     */
    public boolean isCompleted() {
        return !TextUtils.isEmpty(this.prefs.getString(getResources().getString(R.string.FIRST_NAME_DATA_KEY)))
                && !TextUtils.isEmpty(this.prefs.getString(getResources().getString(R.string.LAST_NAME_DATA_KEY)))
                && !TextUtils.isEmpty(this.prefs.getString(getResources().getString(R.string.EMAIL_DATA_KEY)));
    }

}
