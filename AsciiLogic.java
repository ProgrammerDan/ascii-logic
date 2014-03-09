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
		if (topNode instanceOf InputGate) {
			System.out.print(topNode.getSymbol());
			System.out.print(AsciiLogic.
	}
	public void debugTree(Gate topNode) {
		// simple for testing!
		LinkedList<Gate> queue = new LinkedList<Gate>();
		queue.addLast(topNode);
		while (!queue.isEmpty()) {
			Gate curGate = queue.peekLast();
			System.out.println(curGate.getSymbol());
			for (Gate prior : curGate.getPriors()) {
				queue.addLast(prior);
			}
		}
	}

	private Gate resolveSymbol(StringIterator sop) {
		Gate curGate = null;
		StringBuilder symbol = new StringBuilder();
		while (sop.hasNext()) {
			switch(sop.peekNext()) { // look ahead
				case '*':
				case '+': // sum/product/not terminators for symbols
					System.out.println("Found symbol: " + symbol.toString());
					curGate = new Input(symbol.toString());
					break;
				case '\'': // Handle NOT. Given sum-of-product, NOT only exists after inputs.
					System.out.println("Found symbol NOT: " + symbol.toString());
					Input term = new Input(symbol.toString());
					NotGate not = new NotGate();
					term.setNext(not);
					not.addPrior(term);
					sop.step(); // move "iterator" forward
					curGate = not;
					break;
				default:
					System.out.println("Adding symbol: " +sop.peekNext());
					symbol.append(sop.next());
					break;
			}
			if (curGate!=null) break;
		}
		if (curGate == null && symbol.length() > 0) { // end case, last symbol in string
			System.out.println("Found symbol after running out of string: " + symbol.toString());
			curGate = new Input(symbol.toString());
		} else if (curGate == null && symbol.length() == 0) {
			throw new RuntimeException("No valid symbol discovered!");
		}
		return curGate;
	}

	private Gate resolveProduct(StringIterator sop) {
		Gate curGate = null;
		while (sop.hasNext()) {
			switch(sop.peekNext()) { // look ahead
				case '*': // product symbol, indicates we have another term ahead.
					System.out.println("Finished one term of product, on to next");
					sop.step(); // so move forward and let the other case handle it.
					break;
				case '+': // done with this product
					System.out.println("Done with product, on to sum");
					if (curGate == null ) {
						throw new RuntimeException("No term available for OR gate");
					}
					return curGate;
				case '\'': // unprocessed NOT
					throw new RuntimeException("NOT encountered unexpectedly.");
				default: // find the next term for this AND
					Gate term = resolveSymbol(sop);
					if (curGate == null) { // first term?
						System.out.println("Found first term of a product");
						curGate = term;
					} else if (!(curGate instanceof AndGate)) {
						System.out.println("Had one term, found another of a product");
						// have one term, let's add to it
						AndGate and = new AndGate();
						and.addPrior(curGate);
						and.addPrior(term);
						curGate = and;
					} else { // already have a valid And gate.
						System.out.println("Had multiple terms, found another of a product");
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
					System.out.println("Done one term of Sum, on to next");
					sop.step();
					break;
				default: // next term
					Gate term = resolveProduct(sop);
					if (curGate == null) { // first term?
						System.out.println("Found first term of Sum");
						curGate = term;
					} else if (!(curGate instanceof OrGate)) {
						System.out.println("Had one term, found another of a sum");
						// have one term, add to new OrGate
						OrGate sum = new OrGate();
						sum.addPrior( curGate );
						sum.addPrior( term );
						curGate = sum;
					} else {
						System.out.println("Had multiple terms, found another of a sum");
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

	public static final String makeSymbol(char[] symbol) {
		return new String(symbol);
	}

	public static final String makeSymbol(byte[] symbol) {
		try {
			return new String(symbol, "UTF-8");//US-ASCII");
		} catch(java.io.UnsupportedEncodingException uee) {
			uee.printStackTrace();
			throw new RuntimeException("Unexpected encoding error, check your compilation version.",uee);
		}
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
			if (curLocation+1 < interior.length()) {
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
				return interior.charAt(++curLocation);
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

	class ExpressionException extends RuntimeException {
		public ExpressionException(String problem) {
			super(problem);
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
			return new ArrayList<Gate>();
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
		private final String SYMBOL=AsciiLogic.makeSymbol(new byte[]{(byte)0xB3,0x3E,0x6F});

		private Gate next = null;
		private List<Gate> prior = null;
		public NotGate() {
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
		private final String SYMBOL=AsciiLogic.makeSymbol(new byte[]{(byte)0xB3,0x26,0x26});
		public AndGate() {
			super();
		}
		public String getSymbol() {
			return SYMBOL;
		}
	}

	class OrGate extends MultiGate {
		private final String SYMBOL=AsciiLogic.makeSymbol(new byte[]{(byte)0xB3,0x4F,0x52});
		public OrGate() {
			super();
		}
		public String getSymbol() {
			return SYMBOL;
		}
	}
}
