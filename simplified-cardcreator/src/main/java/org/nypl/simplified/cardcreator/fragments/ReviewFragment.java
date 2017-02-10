package org.nypl.simplified.cardcreator.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.nypl.simplified.prefs.Prefs;
import org.nypl.simplified.cardcreator.R;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ReviewFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ReviewFragment extends Fragment {


    private TextView username;
    private TextView pin;
    private TextView name;
    private TextView email;
    private TextView home_address;
    private TextView work_school_address;
    private TextView label_work_address;
    private TextView label_school_address;
    private Prefs prefs;


    /**
     *
     */
    public ReviewFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ConfimrationFragment.
     */
    public  ReviewFragment newInstance() {
        final ReviewFragment fragment = new ReviewFragment();
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
        final View root_view = inflater.inflate(R.layout.fragment_review, container, false);
        ((TextView) root_view.findViewById(android.R.id.title)).setText("Review");
        ((TextView) root_view.findViewById(android.R.id.text1)).setText(this.prefs.getString(getResources().getString(R.string.CARD_TYPE_DATA_KEY)));

        this.username = ((TextView) root_view.findViewById(R.id.username));
        this.pin = ((TextView) root_view.findViewById(R.id.pin));
        this.email = ((TextView) root_view.findViewById(R.id.email));
        this.name = ((TextView) root_view.findViewById(R.id.name));
        this.home_address = ((TextView) root_view.findViewById(R.id.address_home));
        this.work_school_address = ((TextView) root_view.findViewById(R.id.address_work_school));
        this.label_school_address = ((TextView) root_view.findViewById(R.id.Label_address_school));
        this.label_work_address = ((TextView) root_view.findViewById(R.id.label_address_work));


        if (this.prefs.getBoolean(getResources().getString(R.string.WORK_IN_NY_DATA_KEY))) {
            this.label_work_address.setVisibility(View.VISIBLE);
        } else {
            this.label_work_address.setVisibility(View.GONE);
        }
        if (this.prefs.getBoolean(getResources().getString(R.string.SCHOOL_IN_NY_DATA_KEY))) {
            this.label_school_address.setVisibility(View.VISIBLE);
        } else {
            this.label_school_address.setVisibility(View.GONE);
        }

        if (this.prefs.getBoolean(getResources().getString(R.string.SCHOOL_IN_NY_DATA_KEY)) || this.prefs.getBoolean(getResources().getString(R.string.WORK_IN_NY_DATA_KEY))) {
            final StringBuilder work = new StringBuilder();
            work.append(this.prefs.getString(getResources().getString(R.string.STREET1_W_DATA_KEY)) + "\n");
            if (this.prefs.getString(getResources().getString(R.string.STREET2_W_DATA_KEY)) != null) {
                work.append(this.prefs.getString(getResources().getString(R.string.STREET2_W_DATA_KEY)) + "\n");
            }
            work.append(this.prefs.getString(getResources().getString(R.string.CITY_W_DATA_KEY)) + "\n");
            work.append(this.prefs.getString(getResources().getString(R.string.STATE_W_DATA_KEY)) + "\n");
            work.append(this.prefs.getString(getResources().getString(R.string.ZIP_W_DATA_KEY)) + "\n");

            this.work_school_address.setText(work.toString());

        }

        this.username.setText(this.prefs.getString(getResources().getString(R.string.USERNAME_DATA_KEY)));
        this.pin.setText(this.prefs.getString(getResources().getString(R.string.PIN_DATA_KEY)));
        this.name.setText(this.prefs.getString(getResources().getString(R.string.LAST_NAME_DATA_KEY)) + ", "
          + this.prefs.getString(getResources().getString(R.string.FIRST_NAME_DATA_KEY)));
        if (!this.prefs.getString(getResources().getString(R.string.MIDDLE_NAME_DATA_KEY)).isEmpty())
        {
            this.name.setText(this.prefs.getString(getResources().getString(R.string.LAST_NAME_DATA_KEY)) + ", "
              + this.prefs.getString(getResources().getString(R.string.FIRST_NAME_DATA_KEY)) + " "
              + this.prefs.getString(getResources().getString(R.string.MIDDLE_NAME_DATA_KEY)));
        }
        this.email.setText(this.prefs.getString(getResources().getString(R.string.EMAIL_DATA_KEY)));

        final StringBuilder home = new StringBuilder();
        home.append(this.prefs.getString(getResources().getString(R.string.STREET1_H_DATA_KEY)) + "\n");
        if (this.prefs.getString(getResources().getString(R.string.STREET2_H_DATA_KEY)) != null) {
            home.append(this.prefs.getString(getResources().getString(R.string.STREET2_H_DATA_KEY)) + "\n");
        }
        home.append(this.prefs.getString(getResources().getString(R.string.CITY_H_DATA_KEY)) + "\n");
        home.append(this.prefs.getString(getResources().getString(R.string.STATE_H_DATA_KEY)) + "\n");
        home.append(this.prefs.getString(getResources().getString(R.string.ZIP_H_DATA_KEY)) + "\n");

        this.home_address.setText(home.toString());


        return root_view;
    }

}
