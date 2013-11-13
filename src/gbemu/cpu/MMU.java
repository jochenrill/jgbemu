package gbemu.cpu;

import gbemu.memory.IllegalMemoryAccessException;
import gbemu.memory.Memory;

public class MMU {

	private Memory memory;

	protected MMU(Memory m) {
		memory = m;
	}

	public void leaveBios() {
		memory.leaveBios();
	}

	public int readByte(int addr) throws IllegalMemoryAccessException {
		int value = memory.readByte(addr);
		//System.out.println("Read byte " + Integer.toHexString(value)
			//	+ " from address " + Integer.toHexString(addr));
		return value;
	}

	public int readWord(int addr) throws IllegalMemoryAccessException {
		return (readByte(addr + 1) << 8 | readByte(addr));
	}

	public void writeByte(int addr, int value)
			throws IllegalMemoryAccessException {
		memory.writeByte(addr, value);
	}

	public void writeWord(int addr, int value)
			throws IllegalMemoryAccessException {
	//	System.out.println("Will write word " + Integer.toHexString(value)
			//	+ " to address " + Integer.toHexString(addr));
		writeByte(addr, value & 0xFF);
		writeByte(addr + 1, value >> 8);
	}

}
