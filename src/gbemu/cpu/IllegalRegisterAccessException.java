package gbemu.cpu;

public class IllegalRegisterAccessException extends Exception {

	private String message;

	public IllegalRegisterAccessException(String m) {
		message = m;
	}

	@Override
	public String getMessage() {
		return message;
	}
}
