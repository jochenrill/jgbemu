package gbemu.cpu;

public class UnsupportedOpcodeException extends Exception {

	String message;

	public UnsupportedOpcodeException(String string) {
		message = string;

	}

	@Override
	public String getMessage() {
		return message;
	}

}
