package com.example.ungdungchiasecaycanh.ui.safety;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.ungdungchiasecaycanh.data.repository.SafetyRepository;
import com.example.ungdungchiasecaycanh.data.repository.UserRepository;
import com.example.ungdungchiasecaycanh.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlockedUsersViewModel extends ViewModel {
    public enum Status { LOADING, CONTENT, EMPTY, ERROR }
    public static final class Item {
        public final String uid,displayName;
        Item(String uid,String displayName){this.uid=uid;this.displayName=displayName;}
    }
    public static final class State {
        public final Status status;
        public final List<Item> items;
        public final String busyUid;
        public final int errorVersion;
        State(Status status,List<Item> items,String busyUid,int errorVersion){
            this.status=status;this.items=items;this.busyUid=busyUid;this.errorVersion=errorVersion;
        }
    }
    private final SafetyRepository safety=new SafetyRepository();
    private final UserRepository users=new UserRepository();
    private final MutableLiveData<State> state=new MutableLiveData<>();
    private List<Item> items=Collections.emptyList();
    private String busyUid;
    private int errorVersion,generation;
    public LiveData<State> getState(){return state;}
    public void load(){
        int request=++generation;publish(Status.LOADING);
        safety.getBlockedUids().addOnSuccessListener(ids->{
            if(request!=generation)return;
            if(ids.isEmpty()){items=Collections.emptyList();publish(Status.EMPTY);return;}
            List<String> ordered=new ArrayList<>(ids);Collections.sort(ordered);
            List<Task<User>> tasks=new ArrayList<>();for(String uid:ordered)tasks.add(users.getProfile(uid));
            Tasks.whenAllComplete(new ArrayList<Task<?>>(tasks)).addOnCompleteListener(done->{
                if(request!=generation)return;
                List<Item> resolved=new ArrayList<>();
                for(int i=0;i<ordered.size();i++){
                    User user=tasks.get(i).isSuccessful()?tasks.get(i).getResult():null;
                    String name=user==null||user.getDisplayName()==null||user.getDisplayName().trim().isEmpty()
                            ?"PlantMate member":user.getDisplayName();
                    resolved.add(new Item(ordered.get(i),name));
                }
                items=resolved;publish(Status.CONTENT);
            });
        }).addOnFailureListener(error->{if(request==generation)publish(Status.ERROR);});
    }
    public void unblock(String uid){
        if(busyUid!=null||uid==null)return;
        busyUid=uid;publish(Status.CONTENT);
        safety.unblockUser(uid).addOnSuccessListener(ignored->{busyUid=null;load();})
                .addOnFailureListener(error->{busyUid=null;errorVersion++;publish(Status.CONTENT);});
    }
    private void publish(Status status){state.setValue(new State(status,new ArrayList<>(items),busyUid,errorVersion));}
}
