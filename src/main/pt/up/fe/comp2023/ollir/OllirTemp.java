package pt.up.fe.comp2023.ollir;

public class OllirTemp {
    private String type;
    private boolean temp;

    OllirTemp(String type, boolean temp) {
        this.type = type;
        this.temp = temp;
    }

    OllirTemp() {
        this.type = null;
        this.temp = false;
    }

    public String getType() {
        return type;
    }

    public boolean isTemp() {
        return temp;
    }
}
