import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;

public class AsciiLogic {
	public static void main(String[] args) {
		(new AsciiLogic()).start();
	}

	public void start() {
		System.out.print("Sum of products (A*B+B'*C) equation to draw?");
		Scanner input = new Scanner(System.in);

		Gate tree = parse(input.nextLine());

		outputTree(tree);
	}

	public Gate parse(String sopString) {
		StringIterator sop = new StringIterator(sopString);
		Gate finalGate = resolveSum(sop);

		return finalGate;
	}

	public void outputTree(Gate topNode) {
		// simple for testing!
		LinkedList<Gate> queue = new LinkedList<Gate>();
		queue.addLast(topNode);
		while (!queue.isEmpty()) {
			Gate curGate = queue.pollLast();
			System.out.println(curGate.getSymbol());
			queue.addAll(curGate.getPriors());
		}
	}

	private Gate resolveSymbol(StringIterator sop) {
		Gate curGate = null;
		StringBuilder symbol = new StringBuilder();
		while (sop.hasNext()) {
			switch(sop.peekNext()) { // look ahead
				case '*':
				case '+': // sum/product/not terminators for symbols
					curGate = new Input(symbol.toString());
					break;
				case '\'': // Handle NOT. Given sum-of-product, NOT only exists after inputs.
					Input term = new Input(symbol.toString());
					NotGate not = new NotGate();
					term.setNext(not);
					not.addPrior(term);
					sop.step(); // move "iterator" forward
					curGate = not;
					break;
				default:
					symbol.append(sop.next());
					break;
			}
			if (curGate!=null) break;
		}
		if (curGate == null && symbol.length() > 0) { // end case, last symbol in string
			curGate = new Input(symbol.toString());
		} else {
			throw new RuntimeException("No valid symbol discovered!");
		}
		return curGate;
	}

	private Gate resolveProduct(StringIterator sop) {
		Gate curGate = null;
		while (sop.hasNext()) {
			switch(sop.peekNext()) { // look ahead
				case '*': // product symbol, indicates we have another term ahead.
					sop.step(); // so move forward and let the other case handle it.
					break;
				case '+': // done with this product
					if (curGate == null ) {
						throw new RuntimeException("No term available for OR gate");
					}
					return curGate;
				case '\'': // unprocessed NOT
					throw new RuntimeException("NOT encountered unexpectedly.");
				default: // find the next term for this AND
					Gate term = resolveSymbol(sop);
					if (curGate == null) { // first term?
						curGate = term;
					} else if (!(curGate instanceof AndGate)) {
						// have one term, let's add to it
						AndGate and = new AndGate();
						and.addPrior(curGate);
						and.addPrior(term);
						curGate = and;
					} else { // already have a valid NOT gate.
						curGate.addPrior(term);
					}
					break;
			}
		}
		if (curGate == null){
			throw new RuntimeException("Invalid SOP expression, no products discovered");
		}
		return curGate;
	}

	private Gate resolveSum(StringIterator sop) {
		Gate curGate = null;
		while (sop.hasNext()) {
			switch(sop.peekNext()) { // look ahead
				case '*':
				case '\'': // something's wrong!
					throw new RuntimeException("Invalid SOP expression, operation before term");
				case '+': // sum symbol, indicates another term ahead.
					sop.step();
					break;
				default: // next term
					Gate term = resolveProduct(sop);
					if (curGate == null) { // first term?
						curGate = term;
					} if (!(curGate instanceof OrGate)) {
						// have one term, add to new OrGate
						OrGate sum = new OrGate();
						sum.addPrior( curGate );
						sum.addPrior( term );
						curGate = sum;
					} else {
						curGate.addPrior( term );
					}
					break;
			}
		}
		if (curGate == null) {
			throw new RuntimeException("Invalid SOP expression, no sums discovered");
		}
		return curGate;
	}

	class StringIterator {
		private String interior = null;

		private int curLocation = -1;

		public StringIterator(String iter){
			interior = iter;
			if (interior == null) {
				throw new IllegalArgumentException("String to iterate must not be null");
			}
			curLocation = -1;
		}

		public boolean hasNext() {
			if (curLocation < interior.length()) {
				return true;
			}
			return false;
		}

		public char peekNext() {
			if (hasNext()) {
				return interior.charAt(curLocation+1);
			} else {
				throw new IndexOutOfBoundsException("Cannot peek beyond end of string!");
			}
		}

		public char next() {
			if (hasNext()) {
				return interior.charAt(curLocation++);
			} else {
				throw new IndexOutOfBoundsException("Cannot give a character beyond end of string!");
			}
		}

		public void reset() {
			curLocation = -1;
		}

		public boolean step() {
			if (hasNext()) {
				curLocation++;
				return true;
			}
			return false;
		}
	}

	interface Gate {
		public List<Gate> getPriors();
		public void setPriors(List<Gate> priors);
		public void addPrior(Gate prior);
		public Gate getNext();
		public void setNext(Gate next);
		public String getSymbol();
		public void setSymbol(String symbol);
	}

	class Input implements Gate {
		private Gate next = null;
		private String name = null;
		public Input(String name) {
			this.name = name;
		}
		public List<Gate> getPriors() {
			return null;
		}
		public void setPriors(List<Gate> priors) {
			return;
		}
		public void addPrior(Gate prior) {
			return;
		}
		public Gate getNext() {
			return next;
		}
		public void setNext(Gate next) {
			this.next = next;
		}
		public String getSymbol() {
			return name;
		}
		public void setSymbol(String symbol) {
			this.name = symbol;
		}
	}

	class NotGate implements Gate {
		private final String SYMBOL;//=NotGate.symbol();
		private final String symbol() {
			try {
				return new String(new byte[]{(byte)0xB3,0x3E,0x6F},"US-ASCII");
			} catch(java.io.UnsupportedEncodingException uee) {
				uee.printStackTrace();
				return "|o>";
			}
		}

		private Gate next = null;
		private List<Gate> prior = null;
		public NotGate() {
			super();
			SYMBOL = symbol();
			this.next = null;
			this.prior = new ArrayList<Gate>();
		}
		public List<Gate> getPriors() {
			return prior;
		}
		public void setPriors(List<Gate> priors) {
			if (priors != null && priors.size() > 0) {
				this.prior.clear();
				this.prior.add(priors.get(0));
			}
		}
		public void addPrior(Gate prior) {
			this.prior.clear();
			this.prior.add(prior);
		}
		public void setNext(Gate next) {
			this.next = next;
		}
		public Gate getNext() {
			return next;
		}
		public void setSymbol(String symbol) {
			return;
		}
		public String getSymbol() {
			return SYMBOL;
		}
	}

	abstract class MultiGate implements Gate {
		private Gate next = null;
		private List<Gate> priors = null;
		public MultiGate() {
			this.next = null;
			this.priors = new ArrayList<Gate>();
		}
		public List<Gate> getPriors() {
			return priors;
		}
		public void setPriors(List<Gate> priors) {
			if (priors != null) {
				this.priors = priors;
			}
		}
		public void addPrior(Gate prior) {
			priors.add(prior);
		}
		public void setNext(Gate next) {
			this.next = next;
		}
		public Gate getNext() {
			return next;
		}
		public void setSymbol(String symbol) {
			return;
		}
	}

	class AndGate extends MultiGate {
		private final String SYMBOL=new String(new byte[]{(byte)0xB3,0x26,0x26},"US-ASCII");
		public AndGate() {
			super();
		}
		public String getSymbol() {
			return SYMBOL;
		}
	}

	class OrGate extends MultiGate {
		private final String SYMBOL=new String(new byte[]{(byte)0xB3,0x4F,0x52},"US-ASCII");
		public OrGate() {
			super();
		}
		public String getSymbol() {
			return SYMBOL;
		}
	}
}
