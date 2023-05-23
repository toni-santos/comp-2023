public class myClass {
    public int a;
    private int[] b;
    private static final String c = "";
    protected static final String d = "";

    public myClass(int var1, int var2) {
        this.a = var1;
    }

    public int get() {
        return this.a;
    }

    public void put(int var1) {
        this.a = var1;
    }

    public void m1() {
        this.a = 2;
    }

    public static void main(String[] param0) {
        System.out.println("Hello World!");
    }
}
