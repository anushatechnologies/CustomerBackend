package com.example.project.customer.service;

import com.example.project.customer.entity.Role;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;

import java.util.Map;

public interface FirebaseAuthService {

    FirebaseToken verifyIdToken(String idToken);

    void setUserRoleClaim(String firebaseUid, Role role);

    void setCustomUserClaims(String firebaseUid, Map<String, Object> claims);

    UserRecord getFirebaseUser(String firebaseUid);

    void deleteFirebaseUser(String firebaseUid);
}
