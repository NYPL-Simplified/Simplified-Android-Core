package org.nypl.simplified.cardcreator.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TextView;

import org.nypl.simplified.prefs.Prefs;
import org.nypl.simplified.cardcreator.R;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AgeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AgeFragment extends Fragment {


  private Prefs prefs;

  /**
   *
   */
  public AgeFragment() {
    // Required empty public constructor
  }

  /**
   * Use this factory method to create a new instance of
   * this fragment using the provided parameters.
   *
   * @return A new instance of fragment AgeFragment.
   */

  public AgeFragment newInstance() {
    final AgeFragment fragment = new AgeFragment();
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
  public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                           final Bundle state) {
    // Inflate the layout for this fragment

    final View root_view = inflater.inflate(R.layout.fragment_age, container, false);

    ((TextView) root_view.findViewById(android.R.id.text1)).setText(R.string.age_verification_challenge);

    root_view.findViewById(android.R.id.text1).setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

    ((RadioButton) root_view.findViewById(R.id.under13)).setChecked(this.prefs.getBoolean(getResources().getString(R.string.UNDER_13)));
    ((RadioButton) root_view.findViewById(R.id.equalOrOlder)).setChecked(this.prefs.getBoolean(getResources().getString(R.string.EQUAL_OR_OLDER_13)));

    ((CheckBox) root_view.findViewById(R.id.eula_checkbox)).setChecked(this.prefs.getBoolean(getResources().getString(R.string.EULA_ACCEPTED)));

    return root_view;
  }


}
