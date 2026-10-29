package com.example.ungdungchiasecaycanh.ui.safety;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.model.ReportReason;
import com.example.ungdungchiasecaycanh.model.ReportTargetType;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ReportBottomSheet extends BottomSheetDialogFragment {
    private static final String TYPE="type", TARGET="target", PARENT="parent";
    private ReportViewModel viewModel;
    private int shownSuccess,shownError;
    public static ReportBottomSheet newInstance(ReportTargetType type,String targetId,String parentId){
        ReportBottomSheet sheet=new ReportBottomSheet(); Bundle args=new Bundle();
        args.putString(TYPE,type.name());args.putString(TARGET,targetId);args.putString(PARENT,parentId);
        sheet.setArguments(args);return sheet;
    }
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle saved){
        return inflater.inflate(R.layout.bottom_sheet_report,container,false);
    }
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle saved){
        viewModel=new ViewModelProvider(this).get(ReportViewModel.class);
        RadioGroup reasons=view.findViewById(R.id.report_reasons);
        TextInputEditText details=view.findViewById(R.id.report_details);
        MaterialButton submit=view.findViewById(R.id.report_submit);
        MaterialButton cancel=view.findViewById(R.id.report_cancel);
        cancel.setOnClickListener(v->dismiss());
        submit.setOnClickListener(v->{
            ReportReason reason=selectedReason(reasons.getCheckedRadioButtonId());
            String text=details.getText()==null?"":details.getText().toString();
            if(reason==null){Toast.makeText(requireContext(),R.string.report_choose_reason,Toast.LENGTH_SHORT).show();return;}
            if(text.trim().length()>500||(reason==ReportReason.OTHER&&text.trim().isEmpty())){
                details.setError(getString(R.string.report_invalid_details));return;
            }
            Bundle args=requireArguments();
            viewModel.submit(ReportTargetType.valueOf(args.getString(TYPE)),args.getString(TARGET),
                    args.getString(PARENT),reason,text);
        });
        viewModel.getState().observe(getViewLifecycleOwner(),state->{
            submit.setEnabled(!state.submitting);
            cancel.setEnabled(!state.submitting);
            setCancelable(!state.submitting);
            if(state.successVersion>shownSuccess){shownSuccess=state.successVersion;
                Toast.makeText(requireContext(),R.string.report_submitted,Toast.LENGTH_SHORT).show();dismiss();}
            if(state.errorVersion>shownError){shownError=state.errorVersion;
                Toast.makeText(requireContext(),R.string.report_failed,Toast.LENGTH_SHORT).show();}
        });
    }
    private static ReportReason selectedReason(int id){
        if(id==R.id.report_harassment)return ReportReason.HARASSMENT;
        if(id==R.id.report_spam)return ReportReason.SPAM;
        if(id==R.id.report_scam)return ReportReason.SCAM;
        if(id==R.id.report_inappropriate)return ReportReason.INAPPROPRIATE;
        if(id==R.id.report_other)return ReportReason.OTHER;
        return null;
    }
}
