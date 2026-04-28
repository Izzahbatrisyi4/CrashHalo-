package com.example.crashhaloapp.repository;

import android.content.Context;
import com.example.crashhaloapp.models.User;
import com.example.crashhaloapp.utils.LocalDatabase;

public class AuthRepository {

    private final LocalDatabase localDatabase;
    private static User loggedInUser = null;

    public AuthRepository(Context context) {
        this.localDatabase = new LocalDatabase(context);
    }

    public User getCurrentUser() {
        if (loggedInUser == null) {
            loggedInUser = localDatabase.getUser();
        }
        return loggedInUser;
    }

    public boolean signIn(String email, String password) {
        User user = localDatabase.getUser();
        if (user != null && user.getEmail().equals(email)) {
            // In a real app, you'd check the password, but for this local demo:
            loggedInUser = user;
            return true;
        }
        return false;
    }

    public void signUp(User user) {
        localDatabase.saveUser(user);
        loggedInUser = user;
    }

    public void signOut() {
        loggedInUser = null;
        // In a real local app, you might want to clear a "session" flag in SharedPreferences
    }

    public boolean isUserLoggedIn() {
        return getCurrentUser() != null;
    }
}
