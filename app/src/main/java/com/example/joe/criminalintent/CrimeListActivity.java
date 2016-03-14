package com.example.joe.criminalintent;

import android.content.Intent;
import android.support.v4.app.Fragment;

/**
 * Created by Joe on 10/10/2015.
 */
public class CrimeListActivity extends SingleFragmentActivity
    implements  CrimeListFragment.Callacks,
    CrimeFragment.Callbacks{

    @Override
    protected Fragment createFragment() {
        return new CrimeListFragment();
    }

    @Override
    protected int getLayoutResId(){
        return R.layout.activity_masterdetail;
    }

    @Override
    public void onCrimeSelected(Crime crime, int request) {
        if (findViewById(R.id.detail_fragment_container) == null) {
            Intent intent = CrimePagerActivity.newIntent(this, crime.getID());
            startActivityForResult(intent, request);
        }else {
            Fragment newDetail = CrimeFragment.newInstance(crime.getID());

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.detail_fragment_container, newDetail)
                    .commit();
        }
    }

    @Override
    public void onCrimeUpdated(Crime crime) {
        CrimeListFragment listFragment = (CrimeListFragment)
                getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_container);
        listFragment.updateUI();
    }
}
