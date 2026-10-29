package com.example.ungdungchiasecaycanh.ui.safety;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.ungdungchiasecaycanh.data.repository.SafetyRepository;

public class SafetyActionViewModel extends ViewModel {
    public static final class State {
        public final String targetUid;
        public final boolean known, blocked, busy;
        public final int changedVersion, errorVersion;
        State(String targetUid, boolean known, boolean blocked, boolean busy,
              int changedVersion, int errorVersion) {
            this.targetUid=targetUid; this.known=known; this.blocked=blocked; this.busy=busy;
            this.changedVersion=changedVersion; this.errorVersion=errorVersion;
        }
    }
    private final SafetyRepository repository=new SafetyRepository();
    private final MutableLiveData<State> state=new MutableLiveData<>();
    private String targetUid;
    private boolean known, blocked, busy;
    private int changedVersion, errorVersion, generation;
    public LiveData<State> getState(){return state;}
    public void load(String uid){
        if(uid==null||uid.trim().isEmpty())return;
        String account=repository.getCurrentUserId();
        targetUid=uid; known=false; busy=false; int request=++generation; publish();
        repository.isBlockedByCurrentUser(uid).addOnSuccessListener(value->{
            if(request!=generation)return;
            if(account==null||!account.equals(repository.getCurrentUserId())){known=false;errorVersion++;publish();return;}
            known=true; blocked=value; publish();
        }).addOnFailureListener(error->{if(request!=generation)return;known=false;errorVersion++;publish();});
    }
    public void toggle(){if(!known||busy||targetUid==null)return;setBlocked(targetUid,!blocked);}
    public void block(String uid){setBlocked(uid,true);}
    public void unblock(String uid){setBlocked(uid,false);}
    private void setBlocked(String uid,boolean desired){
        if(busy||uid==null||uid.trim().isEmpty())return;
        String account=repository.getCurrentUserId();
        busy=true; int request=++generation; publish();
        (desired?repository.blockUser(uid):repository.unblockUser(uid))
                .addOnSuccessListener(ignored->{
                    if(request!=generation)return;
                    if(account==null||!account.equals(repository.getCurrentUserId())){
                        busy=false;known=false;errorVersion++;publish();return;
                    }
                    targetUid=uid;blocked=desired;known=true;busy=false;changedVersion++;publish();
                }).addOnFailureListener(error->{if(request!=generation)return;busy=false;errorVersion++;publish();});
    }
    private void publish(){state.setValue(new State(targetUid,known,blocked,busy,changedVersion,errorVersion));}
}
