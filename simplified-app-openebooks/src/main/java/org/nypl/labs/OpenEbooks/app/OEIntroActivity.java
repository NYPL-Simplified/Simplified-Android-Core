package org.nypl.labs.OpenEbooks.app;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;

import com.viewpagerindicator.CirclePageIndicator;

import org.nypl.simplified.app.catalog.MainCatalogActivity;

public class OEIntroActivity extends Activity implements IntroListenerType
{

  @Override
  protected void onCreate(final Bundle state)
  {
    super.onCreate(state);
    this.setContentView(R.layout.oe_intro);

    final ViewPager view_pager = (ViewPager) this.findViewById(R.id.intro_pager);
    final PagerAdapter pager_adater = new IntroPagerAdapter(getFragmentManager());
    view_pager.setAdapter(pager_adater);

    final CirclePageIndicator indicator = (CirclePageIndicator) this.findViewById(R.id.intro_pager_indicator);
    indicator.setViewPager(view_pager);
  }

  private void openCatalog()
  {
    final Intent i = new Intent(this, MainCatalogActivity.class);
    this.startActivity(i);
    this.overridePendingTransition(0, 0);
    this.finish();
  }

  @Override
  public void onUseExistingCodes()
  {
    this.openCatalog();
  }

  @Override
  public void onRequestNewCodes()
  {
    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://openebooks.net/getstarted.html"));
    startActivity(browserIntent);
  }

  private class IntroPagerAdapter extends FragmentPagerAdapter
  {

    public IntroPagerAdapter(FragmentManager fm)
    {
      super(fm);
    }

    @Override
    public Fragment getItem(
      final int position)
    {
      IntroSlideFragment fragment = IntroSlideFragment.create(position);
      fragment.listener = OEIntroActivity.this;
      return fragment;
    }

    @Override
    public int getCount()
    {
      return 3;
    }
  }
}
