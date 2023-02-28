package graph;

import java.util.Objects;

/** A Pair&lt;X,Y&gt; represents an immutable ordered pair of two Objects of<br>
 * types X and Y respectively.
 *
 * @author eperdew */
public final class Pair<X, Y> {
	X first;
	Y second;

	/** Constructor: a pair (x, y). */
	public Pair(X x, Y y) {
		first= x;
		second= y;
	}

	/** Return the first object in this Pair. */
	public X getX() {
		return first;
	}

	/** Return the second object in this Pair. */
	public Y getY() {
		return second;
	}

	/** Return true iff if ob is a Pair with elements equal to this one. <br>
	 * We don't have to be concerned with ob being a subclass of Pair <br>
	 * because this class is final */
	public @Override boolean equals(Object ob) {
		if (!(ob instanceof Pair<?, ?>)) return false;
		Pair<?, ?> p= (Pair<?, ?>) ob;
		return first.equals(p.first) && second.equals(p.second);
	}

	public @Override int hashCode() {
		return Objects.hash(first, second);
	}

}
