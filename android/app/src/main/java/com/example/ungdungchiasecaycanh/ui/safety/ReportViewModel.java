package com.example.ungdungchiasecaycanh.ui.safety;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.ungdungchiasecaycanh.data.repository.SafetyRepository;
import com.example.ungdungchiasecaycanh.model.ReportReason;
import com.example.ungdungchiasecaycanh.model.ReportTargetType;

public class ReportViewModel extends ViewModel {
    public static final class State {
        public final boolean submitting;
        public final int successVersion,errorVersion;
        State(boolean submitting,int successVersion,int errorVersion){this.submitting=submitting;this.successVersion=successVersion;this.errorVersion=errorVersion;}
    }
    private final SafetyRepository repository=new SafetyRepository();
    private final MutableLiveData<State> state=new MutableLiveData<>(new State(false,0,0));
    private boolean submitting;
    private int successVersion,errorVersion;
    public LiveData<State> getState(){return state;}
    public void submit(ReportTargetType type,String targetId,String parentId,ReportReason reason,String details){
        if(submitting)return;
        submitting=true;publish();
        repository.submitReport(type,targetId,parentId,reason,details)
                .addOnSuccessListener(ignored->{submitting=false;successVersion++;publish();})
                .addOnFailureListener(error->{submitting=false;errorVersion++;publish();});
    }
    private void publish(){state.setValue(new State(submitting,successVersion,errorVersion));}
}
