package com.example.healthtracker.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.example.healthtracker.R;
import com.example.healthtracker.activities.MainActivity;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MenuAccountFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MenuAccountFragment extends DialogFragment {

    public MenuAccountFragment() {
        // Required empty public constructor
    }

    public static MenuAccountFragment newInstance(String param1, String param2) {
        MenuAccountFragment fragment = new MenuAccountFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
//            mParam1 = getArguments().getString(ARG_PARAM1);
//            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        // Set dialog style
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.DialogFragmentStyle);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_menu_account, container, false);
        
        // Find the sign out option
        LinearLayout signOutOption = view.findViewById(R.id.signOutOption);
        signOutOption.setOnClickListener(v -> {
            // Get the MainActivity instance and call signOut
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).signOut();
            }
            dismiss(); // Close the dialog
        });

        return view;
    }
}