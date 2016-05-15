package rageofachilles.textryan_wear;

import android.os.Bundle;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.wearable.view.WatchViewStub;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

public class LaunchActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks, OpenOnPhoneFragment.OnFragmentInteractionListener, MainFragment.OnFragmentInteractionListener
{
    private static final int NUM_PAGES = 2;
    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;

    GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);

        //Connect the GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
        this.setTitle("Text Ryan");

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub_activity);

        // Handle UI Elements
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener()
        {
            @Override
            public void onLayoutInflated(WatchViewStub stub)
            {

                // Instantiate a ViewPager and a PagerAdapter.
                mPager = (ViewPager) findViewById(R.id.pager);
                mPagerAdapter = new FragmentPagerAdapter(getSupportFragmentManager());
                mPager.setAdapter(mPagerAdapter);

            }
        });
    }// end OnCreate()

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onOpenOnPhoneFragmentInteraction(String string)
    {
        Log.v("myTag","onFragmentInteraction: " + string);
    }

    @Override
    public void onMainFragmentInteraction(String string)
    {
        Log.v("myTag","onFragmentInteraction: " + string);
    }

    private class FragmentPagerAdapter extends FragmentStatePagerAdapter
    {
        public FragmentPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch(position) {
                case 0: // Fragment # 0 - This is the main app
                    return new MainFragment();
                case 1: // Fragment # 0 - This will show the open on phone button
                    return new OpenOnPhoneFragment();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }
}
