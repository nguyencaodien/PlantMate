package com.example.ungdungchiasecaycanh.ui.doctor;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;

public class DoctorHistoryActivity extends AppCompatActivity {
    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_history);
        SecondaryChrome.applyPlain(this, findViewById(R.id.doctorHistoryRoot));
        findViewById(R.id.doctorHistoryBack).setOnClickListener(v -> finish());
        RecyclerView list = findViewById(R.id.doctorHistoryList);
        list.setLayoutManager(new LinearLayoutManager(this));
        DoctorHistoryAdapter adapter = new DoctorHistoryAdapter(id -> {
            Intent intent = new Intent(this, DoctorResultActivity.class);
            intent.putExtra(DoctorResultActivity.EXTRA_HISTORY_ID, id);
            startActivity(intent);
        });
        list.setAdapter(adapter);
        DoctorViewModel viewModel = new ViewModelProvider(this).get(DoctorViewModel.class);
        View empty = findViewById(R.id.doctorHistoryEmpty);
        View error = findViewById(R.id.doctorHistoryError);
        View loading = findViewById(R.id.doctorHistoryLoading);
        if (!viewModel.hasAuthenticatedUser()) {
            loading.setVisibility(View.GONE); error.setVisibility(View.VISIBLE); list.setVisibility(View.GONE); return;
        }
        viewModel.getHistory().observe(this, history -> {
            loading.setVisibility(View.GONE);
            adapter.submitList(history);
            boolean isEmpty = history == null || history.isEmpty();
            empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            list.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            error.setVisibility(View.GONE);
        });
    }
}
