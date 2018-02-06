package locations.nobar.br.savelocations;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

/**
 * Created by lucas on 06/02/18.
 */

public class UIUtils {
    public static void fullScreenImage(Activity currentActivity, String imagePath){
        final Dialog nagDialog = new Dialog(currentActivity,android.R.style.Theme_Light_NoTitleBar_Fullscreen);
        nagDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        nagDialog.setCancelable(false);
        nagDialog.setContentView(R.layout.image_preview);
        ImageButton btnClose = (ImageButton)nagDialog.findViewById(R.id.btnIvClose);
        ImageView ivPreview = (ImageView)nagDialog.findViewById(R.id.iv_preview_image);
        ivPreview.setImageBitmap(BitmapFactory.decodeFile(imagePath));
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {

                nagDialog.dismiss();
            }
        });
        nagDialog.show();
    }

}
