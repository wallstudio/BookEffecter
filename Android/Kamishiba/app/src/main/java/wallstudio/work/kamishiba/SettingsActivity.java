package wallstudio.work.kamishiba;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.TwoStatePreference;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        // アクションバーの設定
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            // リストとテキストフォームは値をViewに書き込む必要がある
            PreferenceGroup root = (PreferenceGroup) findPreference("general_prefs");
            for(int i = 0; i < root.getPreferenceCount(); i++){
                bindPreference(root.getPreference(i));
            }
        }

        private void bindPreference(final Preference preference) {
            if(preference instanceof EditTextPreference|| preference instanceof ListPreference){
                // 値が変更された時，Viewに現在の状態が刻む
                Preference.OnPreferenceChangeListener listener
                        = new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object value) {
                        if (preference instanceof ListPreference) {
                            ListPreference listPreference = (ListPreference) preference;
                            int index = listPreference.findIndexOfValue(value.toString());
                            preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
                        } else {
                            preference.setSummary(value.toString());
                        }
                        return true;
                    }
                };
                preference.setOnPreferenceChangeListener(listener);
                String defaultValue
                        = PreferenceManager.getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), "");
                listener.onPreferenceChange(preference, defaultValue);

            } else if(preference instanceof PreferenceGroup){
                final PreferenceGroup parent = (PreferenceGroup) preference;
                if(preference.getKey().equals("pref_category_adult")) {
                    // 成人確認が必要な項目
                    for (int i = 0; i < parent.getPreferenceCount(); i++) {
                        Preference.OnPreferenceChangeListener listener
                                = new Preference.OnPreferenceChangeListener() {
                            @Override
                            public boolean onPreferenceChange(final Preference checkBoxPreference, Object value) {
                                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                                final SharedPreferences.Editor editor = sharedPreferences.edit();

                                // Group全てがfalesからいずれかをtrueにする場合のみ確認を出す
                                List<Boolean> status = new ArrayList<>();
                                for(int j = 0; j < parent.getPreferenceCount(); j++){
                                    String key = parent.getPreference(j).getKey();
                                    status.add(sharedPreferences.getBoolean(key, false));
                                }
                                if (status.indexOf(true) < 0 && (Boolean) value) {
                                    showConformDialog(editor, (TwoStatePreference) checkBoxPreference);
                                }

                                return true;
                            }

                            private void showConformDialog(final SharedPreferences.Editor editor, final TwoStatePreference preference){
                                new AlertDialog.Builder(getActivity()).setTitle("年齢確認").setMessage("18歳以上ですか？")
                                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                    }
                                                }
                                        )
                                        .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        editor.putBoolean("pref_sexy", false);
                                                        editor.apply();
                                                        preference.setChecked(false);
                                                    }
                                                }
                                        )
                                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                            @Override
                                            public void onCancel(DialogInterface dialog) {
                                                editor.putBoolean("pref_sexy", false);
                                                editor.apply();
                                                preference.setChecked(false);

                                            }
                                        }).show();
                            }
                        };
                        parent.getPreference(i).setOnPreferenceChangeListener(listener);
                    }
                } else{
                    for (int i = 0; i < parent.getPreferenceCount(); i++) {
                        bindPreference(parent.getPreference(i));
                    }
                }
            }
        }
    }
}
