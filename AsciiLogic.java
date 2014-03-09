import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

public class AsciiLogic {
	public static void main(String[] args) {
		(new AsciiLogic()).start();
	}

	public void start() {
		System.out.print("Sum of products (A*B+B'*C) equation to draw?");
		Scanner input = new Scanner(System.in);

		String in = input.nextLine();
		Gate tree = parse(in);

		outputTree(tree, in);
	}

	public Gate parse(String sopString) {
		StringIterator sop = new StringIterator(sopString);
		Gate finalGate = resolveSum(sop);

		return finalGate;
	}

	public void outputTree(Gate topNode, String sopString) {
		byte[] sop = sopString.getBytes();
		List<Input> inputs = new LinkedList<Input>();
		Map<String, Integer> info = new HashMap<String, Integer>();
		info.put("Depth", 1);
		info.put("SymLength", 0);
		info.put("AndCount", 0);
		info.put("NotCount", 0);
		unwrap(topNode, inputs, info, 1);
		System.out.println("Max Depth: " + info.get("Depth"));
		System.out.println("Max SymLength: " + info.get("SymLength"));
		System.out.println("Num AndGates: " + info.get("AndCount"));
		System.out.println("Num NotGates: " + info.get("NotCount"));
		if (inputs.size() < 1) {
			System.out.println("No inputs, invalid structure!");
		} else {
			int nlines = inputs.size() * 2 - 1;
			int lineSize = info.get("SymLength") + info.get("Depth") * 7 + sop.length;
			byte[][] output = new byte[nlines][lineSize];
			int inSize = 0;
			int maxLine = 0;
			Input in = null;
			// 
			for (int i = 0; i < nlines; i++) {
				int j = 0; // line pointer.
				if (i % 2 == 0) {
					in = inputs.get(i/2);
					byte[] symbol = in.getSymbol();
					System.arraycopy(symbol, 0, output[i], j, symbol.length);
					j+=symbol.length;
					Arrays.fill(output[i], j, j+info.get("SymLength") - symbol.length + 4, (byte)196);
					j+= info.get("SymLength") - symbol.length + 4;
					if (info.get("NotCount") > 0) {
						if (in.getNext() != null && in.getNext() instanceof NotGate) {
							symbol = in.getNext().getSymbol();
							System.arraycopy(symbol, 0, output[i], j, symbol.length);
							j += symbol.length;
							Arrays.fill(output[i], j, j+4, (byte)196);
							j += 4;
						} else {
							Arrays.fill(output[i], j, j+7, (byte)196);
							j += 7;
						}
					}
					if (j >maxLine) {
						maxLine = j;
					}
				}
				if (info.get("AndCount") > 0){
					// do stuff
				}
				try {
					System.out.write(output[i]);
					System.out.write(new byte[]{(byte)0x0a, (byte)0x0d});
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		}
	}

	private void unwrap(Gate node, List<Input> list, Map<String, Integer> info, int depth) {
		int maxdepth = info.get("Depth");
		if (depth > maxdepth) {
			info.put("Depth", depth);
		}
		if (node instanceof Input) {
			list.add((Input)node);
			int maxSymLength = info.get("SymLength");
			if (node.getSymbol().length > maxSymLength) {
				info.put("SymLength",node.getSymbol().length);
			}
		} else {
			if (node instanceof AndGate) {
				info.put("AndCount", info.get("AndCount") + 1);
			} else if (node instanceof NotGate) {
				info.put("NotCount", info.get("NotCount") + 1);
			}
			for (Gate child : node.getPriors()) {
				unwrap(child, list, info, depth+1);
			}
		}
	}

	public void debugTree(Gate topNode) {
		// simple for testing!
		LinkedList<Gate> queue = new LinkedList<Gate>();
		queue.addLast(topNode);
		while (!queue.isEmpty()) {
			Gate curGate = queue.pollLast();
			try {
				System.out.write(curGate.getSymbol());
			} catch (IOException ioe) {
				System.err.println("Can't print symbol for " + curGate.getClass().getName() + ": " + ioe.getMessage());
			}
			System.out.println();
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
		public byte[] getSymbol();
		public void setSymbol(byte[] symbol);
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
		public byte[] getSymbol() {
			return name.getBytes();
		}
		public void setSymbol(byte[] symbol) {
			this.name = new String(symbol);
		}
	}

	class NotGate implements Gate {
		private final byte[] SYMBOL=new byte[]{(byte)0xB3,0x3E,0x6F};

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
		public void setSymbol(byte[] symbol) {
			return;
		}
		public byte[] getSymbol() {
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
		public void setSymbol(byte[] symbol) {
			return;
		}
	}

	class AndGate extends MultiGate {
		private final byte[] SYMBOL=new byte[]{(byte)0xB3,0x26,0x26};
		public AndGate() {
			super();
		}
		public byte[] getSymbol() {
			return SYMBOL;
		}
	}

	class OrGate extends MultiGate {
		private final byte[] SYMBOL=new byte[]{(byte)0xB3,0x4F,0x52};
		public OrGate() {
			super();
		}
		public byte[] getSymbol() {
			return SYMBOL;
		}
	}
}
