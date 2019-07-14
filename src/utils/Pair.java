package utils;

public class Pair<T, K> {
    public T first;
    public K second;
    public Pair(T f, K s) {
        first = f;
        second = s;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Pair)) return false;
        return first.equals(((Pair) obj).first) && second.equals(((Pair) obj).second);
    }

    @Override
    public int hashCode() {
        return (first == null ? 0 : first.hashCode() * 13) + (second == null ? 0 : second.hashCode());
    }

    @Override
    public String toString() {
        return "(" + first.toString() + ";" + second.toString() + ")";
    }
}
