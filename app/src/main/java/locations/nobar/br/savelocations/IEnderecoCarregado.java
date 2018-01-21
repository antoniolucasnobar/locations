package locations.nobar.br.savelocations;

import android.app.Activity;
import android.location.Location;

/**
 * Created by lucas on 05/01/18.
 */

public interface IEnderecoCarregado {
    public void onEnderecoCarregado(Location location);
    public Activity getActivity();
}
