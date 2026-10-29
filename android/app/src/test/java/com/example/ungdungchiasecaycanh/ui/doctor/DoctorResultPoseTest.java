package com.example.ungdungchiasecaycanh.ui.doctor;

import static org.junit.Assert.assertEquals;

import com.example.ungdungchiasecaycanh.R;

import org.junit.Test;

public class DoctorResultPoseTest {
    @Test public void poseFollowsHealthAndConfidence() {
        assertEquals(R.drawable.ic_mam_happy, DoctorResultActivity.mamPose(true, 0.9));
        assertEquals(R.drawable.ic_mam_worry, DoctorResultActivity.mamPose(false, 0.9));
        assertEquals(R.drawable.ic_mam_look, DoctorResultActivity.mamPose(null, null));
        // Low confidence wins over the health verdict: Mầm says she is not sure.
        assertEquals(R.drawable.ic_mam_unsure, DoctorResultActivity.mamPose(true, 0.4));
    }
}
