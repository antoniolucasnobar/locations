package locations.nobar.br.savelocations.model;

import android.support.annotation.NonNull;

/**
 * Created by lucas on 26/12/17.
 */

public class Estado implements Comparable<Estado>{
    int id;
    String sigla;
    String nome;
    Regiao regiao;

    public Estado(){}

    public Estado(String nome) {
        this.nome = nome;
    }

    public String toString(){
        return nome;
    }

    @Override
    public int compareTo(@NonNull Estado o) {
        return nome.compareTo(o.nome);
    }

    @Override
    public boolean equals(Object obj) {
        Estado o = (Estado) obj;
        return nome.equals(o.nome);
    }

    public int getId() {
        return id;
    }
}
