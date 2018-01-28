package locations.nobar.br.savelocations;

import android.support.annotation.NonNull;
import android.view.View;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Created by lucas on 20/01/18.
 */

public class UserInstance {

    private static UserInstance userInstance;
    private UserInformation currentUserInformation;

    public static UserInstance getInstance(){
        if (userInstance == null) {
            userInstance = new UserInstance();
        }
        return userInstance;
    }

    private UserInstance() {
    }


    public void setCurrentUserInformation(UserInformation userInformation){
        this.currentUserInformation = userInformation;

    }

    public UserInformation getCurrentUserInformation(){
        return currentUserInformation;
    }

    public void logout(){
        this.currentUserInformation = null;
    }
}
