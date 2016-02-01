package org.nypl.labs.OpenEbooks.app;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.io7m.jnull.NullCheck;

/**
 * Represents a page on the intro screen.
 */
public class IntroSlideFragment extends Fragment
{
  public IntroListenerType listener;
  private int page_number;

  public static final String PAGE = "page";


  public static IntroSlideFragment create(
    final int in_page_number)
  {
    final IntroSlideFragment fragment = new IntroSlideFragment();
    final Bundle args = new Bundle();
    args.putInt(PAGE, in_page_number);
    fragment.setArguments(args);
    return fragment;
  }

  public IntroSlideFragment()
  {
  }

  @Override
  public void onCreate(
    final Bundle bundle)
  {
    super.onCreate(bundle);
    this.page_number = getArguments().getInt(PAGE);
  }

  @Override
  public View onCreateView(
  final LayoutInflater inflater,
  final ViewGroup container,
  final Bundle bundle)
  {
    ViewGroup root_view;
    switch (this.page_number) {
      default: root_view = (ViewGroup) inflater.inflate(R.layout.intro_slide_1, container, false); break;
      case 1: root_view = (ViewGroup) inflater.inflate(R.layout.intro_slide_2, container, false); break;
      case 2: root_view = (ViewGroup) inflater.inflate(R.layout.intro_slide_3, container, false); break;
    }

    if (this.page_number == 2) {
      Button existing = NullCheck.notNull((Button) root_view.findViewById(R.id.use_existing_code));
      Button request = NullCheck.notNull((Button) root_view.findViewById(R.id.request_new_codes));
      existing.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View view)
        {
          if (IntroSlideFragment.this.listener != null) {
            IntroSlideFragment.this.listener.onUseExistingCodes();
          }
        }
      });
      request.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View view)
        {
          if (IntroSlideFragment.this.listener != null) {
            IntroSlideFragment.this.listener.onRequestNewCodes();
          }
        }
      });
    }
    return root_view;
  }
}
