package com.example.ungdungchiasecaycanh.ui.safety;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.util.Motion;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;

public class BlockedUsersActivity extends AppCompatActivity {
    private BlockedUsersViewModel viewModel;
    private BlockedUserAdapter adapter;
    private int shownError;
    @Override protected void onCreate(Bundle saved){super.onCreate(saved);
        setContentView(R.layout.activity_blocked_users);
        SecondaryChrome.apply(this, findViewById(R.id.blocked_users_root), findViewById(R.id.blocked_users_hero));
        Motion.start(findViewById(R.id.blocked_users_mam));
        viewModel=new ViewModelProvider(this).get(BlockedUsersViewModel.class);
        adapter=new BlockedUserAdapter(uid->viewModel.unblock(uid));
        RecyclerView list=findViewById(R.id.blocked_users_list);
        list.setLayoutManager(new LinearLayoutManager(this));list.setAdapter(adapter);
        findViewById(R.id.blocked_users_back).setOnClickListener(v->finish());
        findViewById(R.id.blocked_users_retry).setOnClickListener(v->viewModel.load());
        viewModel.getState().observe(this,state->{
            findViewById(R.id.blocked_users_loading).setVisibility(state.status==BlockedUsersViewModel.Status.LOADING?View.VISIBLE:View.GONE);
            list.setVisibility(state.status==BlockedUsersViewModel.Status.CONTENT?View.VISIBLE:View.GONE);
            findViewById(R.id.blocked_users_empty).setVisibility(state.status==BlockedUsersViewModel.Status.EMPTY?View.VISIBLE:View.GONE);
            findViewById(R.id.blocked_users_error).setVisibility(state.status==BlockedUsersViewModel.Status.ERROR?View.VISIBLE:View.GONE);
            adapter.submit(state.items,state.busyUid);
            if(state.errorVersion>shownError){shownError=state.errorVersion;
                Toast.makeText(this,R.string.safety_action_failed,Toast.LENGTH_SHORT).show();}
        });
        viewModel.load();
    }
}
