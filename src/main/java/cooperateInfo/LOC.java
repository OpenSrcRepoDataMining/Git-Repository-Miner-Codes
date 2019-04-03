package cooperateInfo;

public class LOC {
    int addition;
    int deletion;

    LOC(){
        addition = 0;
        deletion = 0;
    }

    LOC(int addition, int deletion){
        this.addition = addition;
        this.deletion = deletion;
    }

    public void setAddition(int addition) {
        this.addition = addition;
    }

    public void setDeletion(int deletion) {
        this.deletion = deletion;
    }

    public int getAddition() {
        return addition;
    }

    public int getDeletion() {
        return deletion;
    }
}
