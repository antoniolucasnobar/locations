package locations.nobar.br.savelocations.model;

import android.support.annotation.NonNull;

/**
 * Created by lucas on 26/12/17.
 */

public class Cidade implements Comparable<Cidade>{
    int id;
    String sigla;
    String nome;
    Estado UF;

    public Cidade(){}

    public Cidade(String nome) {
        this.nome = nome;
    }

    public String toString(){
        return nome;
    }

    @Override
    public int compareTo(@NonNull Cidade o) {
        return nome.compareTo(o.nome);
    }

    @Override
    public boolean equals(Object obj) {
        Cidade o = (Cidade) obj;
        return nome.equals(o.nome);
    }

    public int getId() {
        return id;
    }
}
