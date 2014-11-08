package com.gear2cam.official;

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gear2cam.official.R;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * Use the {@link GearAbsentFragmentLandscape#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class GearAbsentFragmentLandscape extends Fragment {

    public static GearAbsentFragmentLandscape newInstance() {
        GearAbsentFragmentLandscape fragment = new GearAbsentFragmentLandscape();
        return fragment;
    }
    public GearAbsentFragmentLandscape() {
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
        return inflater.inflate(R.layout.fragment_gear_absent_fragment_landscape, container, false);
    }
}
