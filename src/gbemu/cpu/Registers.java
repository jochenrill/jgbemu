package gbemu.cpu;

public class Registers {

	public enum REGISTERS {
		A, B, C, D, E, H, L, BC, HL, DE;
	}

	private int[] registers;

	protected Registers() {
		registers = new int[7];
	}

	public void reset() {
		registers = new int[7];
	}

	public void set(REGISTERS reg, int value)
			throws IllegalRegisterAccessException {
		if (value > 0xFF) {
			throw new IllegalRegisterAccessException("Value "
					+ Integer.toHexString(value) + " is too big for register "
					+ reg);
		}
		registers[reg.ordinal()] = value;
	}

	public int get(REGISTERS reg) {
		return registers[reg.ordinal()];
	}

	public void setWord(REGISTERS reg, int value)
			throws IllegalRegisterAccessException {
		if (value > 0xFFFF) {
			throw new IllegalRegisterAccessException("Value "
					+ Integer.toHexString(value) + " is too big for register "
					+ reg);
		}
		REGISTERS reg1;
		REGISTERS reg2;
		if (reg == REGISTERS.BC) {
			reg1 = REGISTERS.B;
			reg2 = REGISTERS.C;
		} else if (reg == REGISTERS.HL) {
			reg1 = REGISTERS.H;
			reg2 = REGISTERS.L;
		} else if (reg == REGISTERS.DE) {
			reg1 = REGISTERS.D;
			reg2 = REGISTERS.E;
		} else {
			throw new UnsupportedOperationException("Register " + reg
					+ " not a valid 16 bit register.");
		}
		set(reg1, ((value & 0xFF00) >>> 8));
		set(reg2, (value & 0x00FF));

	}

	public int getWord(REGISTERS reg) {
		REGISTERS reg1;
		REGISTERS reg2;
		if (reg == REGISTERS.BC) {
			reg1 = REGISTERS.B;
			reg2 = REGISTERS.C;
		} else if (reg == REGISTERS.HL) {
			reg1 = REGISTERS.H;
			reg2 = REGISTERS.L;
		} else if (reg == REGISTERS.DE) {
			reg1 = REGISTERS.D;
			reg2 = REGISTERS.E;
		} else {
			throw new UnsupportedOperationException("Register " + reg
					+ " not a valid 16 bit register.");
		}
		return (registers[reg1.ordinal()] << 8 | registers[reg2.ordinal()]);
	}
}
