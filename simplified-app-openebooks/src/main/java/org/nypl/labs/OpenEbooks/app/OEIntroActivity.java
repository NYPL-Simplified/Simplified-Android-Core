package org.nypl.labs.OpenEbooks.app;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;

import com.viewpagerindicator.CirclePageIndicator;

import org.nypl.simplified.app.LoginActivity;
import org.nypl.simplified.books.core.LogUtilities;
import org.slf4j.Logger;

public class OEIntroActivity extends FragmentActivity
{
  private static final Logger LOG;
  private ViewPager view_pager;

  static {
    LOG = LogUtilities.getLog(OEIntroActivity.class);
  }

  @Override
  protected void onCreate(final Bundle state)
  {
    super.onCreate(state);
    this.setContentView(R.layout.oe_intro);

    this.view_pager = (ViewPager) this.findViewById(R.id.intro_pager);
    final PagerAdapter pager_adapter = new IntroPagerAdapter(getSupportFragmentManager());
    this.view_pager.setAdapter(pager_adapter);

    final CirclePageIndicator indicator = (CirclePageIndicator) this.findViewById(R.id.intro_pager_indicator);
    indicator.setViewPager(this.view_pager);

    this.view_pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {

      // This method will be invoked when a new page becomes selected.
      @Override
      public void onPageSelected(final int position) {
        if (position == 2)
        {
          final Intent i = new Intent(OEIntroActivity.this, LoginActivity.class);
          OEIntroActivity.this.startActivity(i);
          OEIntroActivity.this.overridePendingTransition(0, 0);
          final Handler handler = new Handler();
          final Runnable r = new Runnable() {
            public void run() {
              OEIntroActivity.this.view_pager.setCurrentItem(1);
            }
          };
          handler.postDelayed(r, 500);
        }
      }

    });

  }

  private class IntroPagerAdapter extends FragmentPagerAdapter
  {

    IntroPagerAdapter(final FragmentManager fm)
    {
      super(fm);
    }

    @Override
    public Fragment getItem(
      final int position)
    {
      return IntroSlideFragment.create(position);
    }

    @Override
    public int getCount()
    {
      return 3;
    }
  }
}
