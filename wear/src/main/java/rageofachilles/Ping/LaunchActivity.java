package rageofachilles.Ping;

import android.app.Activity;
import android.os.Bundle;

import android.app.FragmentManager;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.support.wearable.view.WatchViewStub;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

public class LaunchActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, OpenOnPhoneFragment.OnFragmentInteractionListener, MainFragment.OnFragmentInteractionListener
{
    private static final int NUM_PAGES = 2;
    protected GridViewPager mPager;
    private GridPagerAdapter mPagerAdapter;

    protected GoogleApiClient mGoogleApiClient;
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
        this.setTitle("Ping!");

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub_activity);

        // Handle UI Elements
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener()
        {
            @Override
            public void onLayoutInflated(WatchViewStub stub)
            {
                // Instantiate a ViewPager and a PagerAdapter.
                mPager = (GridViewPager) findViewById(R.id.pager);
                mPagerAdapter = new MyPagerAdapter(getFragmentManager());

                mPager.setAdapter(mPagerAdapter);

                //final GridViewPager pager = (GridViewPager) findViewById(R.id.gridPager);
                DotsPageIndicator dots = (DotsPageIndicator) findViewById(R.id.indicator);
                dots.setPager(mPager);
                dots.setDotFadeWhenIdle(true);
                dots.setDotFadeOutDelay(3000);
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
        // Do nothing
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Do nothing
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onOpenOnPhoneFragmentInteraction(String string)
    {
        Log.v("pingTag","onFragmentInteraction: " + string);
    }

    @Override
    public void onMainFragmentInteraction(String string)
    {
        Log.v("pingTag","onFragmentInteraction: " + string);
    }

    public class MyPagerAdapter extends FragmentGridPagerAdapter
    {
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public android.app.Fragment getFragment(int i, int i1)
        {
            switch(i1) {
                case 0: // Fragment # 0 - This is the main app
                    return new MainFragment();
                case 1: // Fragment # 0 - This will show the open on phone button
                    return new OpenOnPhoneFragment();
                default:
                    return null;
            }
        }

        @Override
        public int getRowCount()
        {
            return 1; // Horizontal only
        }

        @Override
        public int getColumnCount(int i)
        {
            return NUM_PAGES;
        }
    }
}
