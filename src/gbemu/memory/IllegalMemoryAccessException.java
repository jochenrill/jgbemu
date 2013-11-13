package gbemu.memory;

public class IllegalMemoryAccessException extends Exception {

	String message;

	public IllegalMemoryAccessException(String string) {
		message = string;

	}

	@Override
	public String getMessage() {
		return message;
	}

}
