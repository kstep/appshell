package me.kstep.appshell;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.ListView;

public class MainActivity extends Activity
{

    AppListAdapter adapter;
    TextView searchView;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        ListView lv = (ListView) findViewById(R.id.search_result);
        adapter = new AppListAdapter(this);
        lv.setAdapter(adapter);

        ActionBar ab = getActionBar();
        ab.setCustomView(R.layout.actionbar);
        ab.setDisplayShowCustomEnabled(true);
        searchView = (TextView) ab.getCustomView().findViewById(R.id.search_text);
    }

    public void onSearchButtonClick(View view)
    {
        CharSequence name = searchView.getText();
        adapter.getFilter().filter(name);
    }


}
