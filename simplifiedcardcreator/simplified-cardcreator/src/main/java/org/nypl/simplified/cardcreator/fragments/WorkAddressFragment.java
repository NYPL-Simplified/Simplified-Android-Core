package org.nypl.simplified.cardcreator.fragments;


import android.os.Bundle;
import androidx.core.app.Fragment;
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
 * Use the {@link WorkAddressFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WorkAddressFragment extends Fragment {


  private EditText line_1;
  private EditText line_2;
  private EditText city;
  private EditText state;
  private EditText zip;
  private Prefs prefs;

  /**
   * @return address line 1
   */
  public EditText getLine_1() {
    return this.line_1;
  }

  /**
   * @return address line 2
   */
  public EditText getLine_2() {
    return this.line_2;
  }

  /**
   * @return address city
   */
  public EditText getCity() {
    return this.city;
  }

  /**
   * @return address state
   */
  public EditText getState() {
    return this.state;
  }

  /**
   * @return address zip
   */
  public EditText getZip() {
    return this.zip;
  }

  /**
   *
   */
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
    final WorkAddressFragment fragment = new WorkAddressFragment();
    final Bundle args = new Bundle();
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onCreate(final Bundle instance_state) {
    super.onCreate(instance_state);
    this.prefs = new Prefs(getActivity());
  }

  @Override
  public View onCreateView(final LayoutInflater inflater,
                           final ViewGroup container,
                           final Bundle instance_state) {
    // Inflate the layout for this fragment
    final View root_view = inflater.inflate(R.layout.fragment_work_address, container, false);
    if (this.prefs.getBoolean(getResources().getString(R.string.WORK_IN_NY_DATA_KEY))) {
      ((TextView) root_view.findViewById(android.R.id.title)).setText("Work Address");
    } else {
      ((TextView) root_view.findViewById(android.R.id.title)).setText("School Address");
    }

    this.line_1 = ((EditText) root_view.findViewById(R.id.street1));
    this.line_2 = ((EditText) root_view.findViewById(R.id.street2));
    this.city = ((EditText) root_view.findViewById(R.id.city));
    this.state = ((EditText) root_view.findViewById(R.id.region));
    this.zip = ((EditText) root_view.findViewById(R.id.zip));


    this.line_1.setText(this.prefs.getString(getResources().getString(R.string.STREET1_W_DATA_KEY)));
    this.line_2.setText(this.prefs.getString(getResources().getString(R.string.STREET2_W_DATA_KEY)));
    this.city.setText(this.prefs.getString(getResources().getString(R.string.CITY_W_DATA_KEY)));
    this.state.setText(this.prefs.getString(getResources().getString(R.string.STATE_W_DATA_KEY)));
    this.zip.setText(this.prefs.getString(getResources().getString(R.string.ZIP_W_DATA_KEY)));

    if (this.isCompleted()) {
      ((InputListenerType) getActivity()).onInputComplete();
    }
    else {
      ((InputListenerType) getActivity()).onInputInComplete();
    }


    this.line_1.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
      }

      @Override
      public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
      }

      @Override
      public void afterTextChanged(final Editable s) {
        WorkAddressFragment.this.prefs.putString(getResources().getString(R.string.STREET1_W_DATA_KEY), (s != null) ? s.toString() : null);

        if (WorkAddressFragment.this.isCompleted()) {
          ((InputListenerType) getActivity()).onInputComplete();
        }
        else {
          ((InputListenerType) getActivity()).onInputInComplete();
        }
      }
    });
    this.line_2.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
      }

      @Override
      public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
      }

      @Override
      public void afterTextChanged(final Editable s) {
        WorkAddressFragment.this.prefs.putString(getResources().getString(R.string.STREET2_W_DATA_KEY), (s != null) ? s.toString() : null);

        if (WorkAddressFragment.this.isCompleted()) {
          ((InputListenerType) getActivity()).onInputComplete();
        }
        else {
          ((InputListenerType) getActivity()).onInputInComplete();
        }
      }
    });
    this.city.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
      }

      @Override
      public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
      }

      @Override
      public void afterTextChanged(final Editable s) {
        WorkAddressFragment.this.prefs.putString(getResources().getString(R.string.CITY_W_DATA_KEY), (s != null) ? s.toString() : null);

        if (WorkAddressFragment.this.isCompleted()) {
          ((InputListenerType) getActivity()).onInputComplete();
        }
        else {
          ((InputListenerType) getActivity()).onInputInComplete();
        }
      }
    });
    this.state.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
      }

      @Override
      public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
      }

      @Override
      public void afterTextChanged(final Editable s) {
        WorkAddressFragment.this.prefs.putString(getResources().getString(R.string.STATE_W_DATA_KEY), (s != null) ? s.toString() : null);

        if (WorkAddressFragment.this.isCompleted()) {
          ((InputListenerType) getActivity()).onInputComplete();
        }
        else {
          ((InputListenerType) getActivity()).onInputInComplete();
        }
      }
    });
    this.zip.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
      }

      @Override
      public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
      }

      @Override
      public void afterTextChanged(final Editable s) {
        WorkAddressFragment.this.prefs.putString(getResources().getString(R.string.ZIP_W_DATA_KEY), (s != null) ? s.toString() : null);

        if (WorkAddressFragment.this.isCompleted()) {
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
   * @return validate if all required fields are completed
   */
  public boolean isCompleted() {
    return !TextUtils.isEmpty(this.prefs.getString(getResources().getString(R.string.STREET1_W_DATA_KEY)))
      && !TextUtils.isEmpty(this.prefs.getString(getResources().getString(R.string.CITY_W_DATA_KEY)))
      && !TextUtils.isEmpty(this.prefs.getString(getResources().getString(R.string.STATE_W_DATA_KEY)))
      && !TextUtils.isEmpty(this.prefs.getString(getResources().getString(R.string.ZIP_W_DATA_KEY)));
  }


}
