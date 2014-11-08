package com.gear2cam.official;



import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gear2cam.official.R;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link GearAbsentFragmentPortrait#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class GearAbsentFragmentPortrait extends Fragment {
    public static GearAbsentFragmentPortrait newInstance() {
        GearAbsentFragmentPortrait fragment = new GearAbsentFragmentPortrait();
        return fragment;
    }
    public GearAbsentFragmentPortrait() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_gear_absent_fragment_portrait, container, false);
    }
}
