package com.example.ungdungchiasecaycanh.ui.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.ungdungchiasecaycanh.data.repository.UserRepository;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * Demo checks for the phone number and VNeID. No SMS is sent: the code is shown on screen and only
 * "verified" is published on the profile, never the number.
 */
public class VerifyViewModel extends ViewModel {

    public enum Status { IDLE, CODE_SENT, WRONG_PHONE, WRONG_CODE, SAVING, DONE, FAILED }

    private final UserRepository users = new UserRepository();
    private final MutableLiveData<Status> status = new MutableLiveData<>(Status.IDLE);
    private String code;

    public LiveData<Status> getStatus() { return status; }

    /** The demo code to show on screen, or null before one was asked for. */
    public String getCode() { return code; }

    public void sendCode(String phone) {
        if (!isPhone(phone)) {
            status.setValue(Status.WRONG_PHONE);
            return;
        }
        code = String.format(Locale.US, "%06d", new SecureRandom().nextInt(1_000_000));
        status.setValue(Status.CODE_SENT);
    }

    public void verify(String input) {
        if (status.getValue() == Status.SAVING) return;
        if (code == null || input == null || !code.equals(input.trim())) {
            status.setValue(Status.WRONG_CODE);
            return;
        }
        status.setValue(Status.SAVING);
        users.markPhoneVerified()
                .addOnSuccessListener(ignored -> status.postValue(Status.DONE))
                .addOnFailureListener(error -> status.postValue(Status.FAILED));
    }

    /** The VNeID demo step finished; publish it so selling is allowed. */
    public void markVneid() {
        users.markVneidVerified();
    }

    /** A Vietnamese mobile number: 0 or +84, then 9 digits starting with 3, 5, 7, 8 or 9. */
    static boolean isPhone(String raw) {
        if (raw == null) return false;
        return raw.replaceAll("[\\s.\\-]", "").matches("(\\+84|0)[35789]\\d{8}");
    }
}
