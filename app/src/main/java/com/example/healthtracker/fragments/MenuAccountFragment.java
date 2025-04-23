package com.example.healthtracker.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.healthtracker.R;
import com.example.healthtracker.activities.MainActivity;
import com.example.healthtracker.activities.ProfileActivity;
import com.example.healthtracker.activities.AppGuideActivity;
import com.example.healthtracker.utils.LanguageUtils;

public class MenuAccountFragment extends DialogFragment {

    private static final String PREF_NAME = "LanguagePrefs";
    private static final String LANGUAGE_KEY = "language";

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

        LinearLayout profileOption = view.findViewById(R.id.profileOption);
        profileOption.setOnClickListener(v -> {
            // Navigate to ProfileActivity
            Intent intent = new Intent(getActivity(), ProfileActivity.class);
            startActivity(intent);
            dismiss(); // Close the dialog
        });

        // Language option click listener
        LinearLayout languageOption = view.findViewById(R.id.languageOption);
        updateLanguageUI(view);
        languageOption.setOnClickListener(v -> {
            showLanguageSelectionDialog();
        });

        // App Guide option click listener
        LinearLayout appGuideOption = view.findViewById(R.id.appGuideOption);
        appGuideOption.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AppGuideActivity.class);
            startActivity(intent);
            dismiss(); // Close the dialog
        });

        return view;
    }

    private void updateLanguageUI(View view) {
        if (getContext() == null) return;
        
        // Get current language from preferences
        String currentLanguage = LanguageUtils.getSavedLanguage(getContext());
        
        // Get language UI elements
        ImageView languageIcon = view.findViewById(R.id.languageIcon);
        TextView languageText = view.findViewById(R.id.languageText);
        
        // Update icon and text based on selected language
        switch (currentLanguage) {
            case "vi":
                languageIcon.setImageResource(R.drawable.vietnam);
                languageText.setText(R.string.language_vietnamese);
                break;
            case "id":
                languageIcon.setImageResource(R.drawable.indonesia);
                languageText.setText(R.string.language_indonesian);
                break;
            case "en":
            default:
                languageIcon.setImageResource(R.drawable.usa);
                languageText.setText(R.string.language_english);
                break;
        }
    }

    private void showLanguageSelectionDialog() {
        if (getContext() == null) return;

        final String[] languages = new String[]{
                getString(R.string.language_english),
                getString(R.string.language_vietnamese),
                getString(R.string.language_indonesian)
        };

        final String[] languageCodes = new String[]{"en", "vi", "id"};

        // Get current language
        String currentLanguage = LanguageUtils.getSavedLanguage(getContext());
        int selectedIndex = 0;
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(currentLanguage)) {
                selectedIndex = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.select_language)
                .setSingleChoiceItems(languages, selectedIndex, (dialog, which) -> {
                    // Change language
                    LanguageUtils.setLocale(getContext().getApplicationContext(), languageCodes[which]);
                    
                    // Restart activity to apply changes
                    if (getActivity() != null) {
                        Intent intent = new Intent(getActivity(), MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        getActivity().finishAffinity();
                        startActivity(intent);
                    }
                    dialog.dismiss();
                    dismiss(); // Close the menu dialog
                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
        
        builder.create().show();
    }
}