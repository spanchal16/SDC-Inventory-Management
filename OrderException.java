// This is the order exception class.
public class OrderException extends Exception {

	int reference;
	String message;

	public OrderException(String message, int reference) {
		this.message = message;
		this.reference = reference;
	}

	public int getReference() {
		return reference;
	}

	public void setReference(int reference) {
		this.reference = reference;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return "OrderException [reference=" + reference + ", Message=" + message + "]";
	}

}
