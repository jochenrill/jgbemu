package gbemu.cpu;

import java.util.BitSet;

public class ALU {

	private final int ZERO = 7;
	private final int SUB = 6;
	private final int HC = 5;
	private final int C = 4;

	private BitSet flags;

	protected ALU() {
		flags = new BitSet(7);
	}

	public int getFlagRegister() {
		int zero = (flags.get(ZERO) ? 1 : 0);
		int sub = (flags.get(SUB) ? 1 : 0);
		int hc = (flags.get(SUB) ? 1 : 0);
		int c = (flags.get(C) ? 1 : 0);
		return zero << 7 | sub << 6 | hc << 5 | c << 4;
	}

	public void setFlagRegister(int value) {
		flags.set(ZERO, ((value & 0x80) == 0x80));
		flags.set(SUB, ((value & 0x40) == 0x40));
		flags.set(HC, ((value & 0x20) == 0x20));
		flags.set(C, ((value & 0x10) == 0x10));

	}

	public int incByte(int value) {
		flags.set(SUB, false);
		int result = (value + 1) & 0xFF;
		flags.set(ZERO, result == 0);
		int hc = ((value & 0x0F) + 1) & 0x10;
		flags.set(HC, hc == 0x10);
		return result;
	}

	public int incWord(int value) {
		flags.set(SUB, false);
		int result = (value + 1) & 0xFFFF;
		flags.set(ZERO, result == 0);
		int hc = ((value & 0x0FFF) + 1) & 0x1000;
		flags.set(HC, hc == 0x1000);
		return result;
	}

	public int decByte(int value) {
		flags.set(SUB, true);

		int result = (value - 1) & 0xFF;
		flags.set(ZERO, result == 0);
		int hc = ((value & 0x0F) - 1) & 0x10;
		flags.set(HC, hc == 0x10);
		return result;
	}

	public int decWord(int value) {
		flags.set(SUB, true);
		int result = (value - 1) & 0xFFFF;
		flags.set(ZERO, result == 0);
		int hc = ((value & 0x0FFF) - 1) & 0x1000;
		flags.set(HC, hc == 0x1000);
		return result;
	}

	// 1001 1010 << 0001 0011 0100
	// 0010 1011 << 0 0101 0110
	public int rotateLeftCarry(int value) {
		flags.set(C, (value & 0x80) == 0x80);
		int result = value << 1;
		if (wasCarry()) {
			result |= 0x01;
		}
		result &= 0xFF;
		flags.set(ZERO, result == 0);
		flags.set(SUB, false);
		flags.set(HC, false);
		return result;
	}

	public int rotateRightCarry(int value) {
		int bit = (value & 0x01);
		flags.set(C, bit == 1);
		int result = value >>> 1;
		if (bit == 1) {
			result = result | 0x80;
		}
		flags.set(ZERO, result == 0);
		flags.set(SUB, false);
		flags.set(HC, false);
		return result;
	}

	public int rotateLeft(int value) {
		boolean carry = wasCarry();
		flags.set(C, (value & 0x80) == 0x80);
		int tmp = value << 1;
		int result = tmp & 0xFF;
		if (carry) {
			result |= 0x01;
		}
		flags.set(ZERO, result == 0);
		flags.set(SUB, false);
		flags.set(HC, false);
		return result;
	}

	public int rotateRight(int value) {
		boolean carry = wasCarry();
		int bit = (value & 0x01);
		flags.set(C, bit == 1);
		int result = value >>> 1;
		if (carry) {
			result = result | 0x80;
		}
		flags.set(ZERO, result == 0);
		flags.set(SUB, false);
		flags.set(HC, false);
		return result;
	}

	public int addWords(int value1, int value2) {
		flags.set(SUB, false);
		int result = value1 + value2;
		if (result > 0xFFFF) {
			flags.set(C, true);
		}
		int hc = ((value1 & 0x0FFF) + (value2 & 0x0FFF)) & 0x1000;
		flags.set(HC, hc == 0x1000);
		return result & 0xFFFF;
	}

	public int addByteWithCarry(int value1, int value2) {
		int carry = 0;
		if (wasCarry()) {
			carry = 1;
		}
		int result = (value1 + value2 + carry) & 0xFF;
		flags.set(SUB, false);
		flags.set(ZERO, result == 0);
		int hc = (value1 & 0x0F + value2 & 0x0F + carry) & 0x10;
		flags.set(HC, hc == 0x10);
		return result;
	}

	public int subByteWithCarry(int value1, int value2) {
		int carry = 0;
		if (wasCarry()) {
			carry = 1;
		}
		int result = (value1 - value2 - carry) & 0xFF;
		flags.set(SUB, true);
		flags.set(ZERO, result == 0);
		int hc = (value1 & 0x0F - value2 & 0x0F - carry) & 0x10;
		flags.set(HC, hc == 0x10);
		return result;

	}

	public int addByte(int value1, int value2) {
		int result = (value1 + value2) & 0xFF;
		flags.set(SUB, false);
		flags.set(ZERO, result == 0);
		int hc = (value1 & 0x0F + value2 & 0x0F) & 0x10;
		flags.set(HC, hc == 0x10);
		return result;
	}

	public int subByte(int value1, int value2) {
		int result = (value1 - value2) & 0xFF;
		flags.set(SUB, true);
		flags.set(ZERO, result == 0);
		int hc = (value1 & 0x0F - value2 & 0x0F) & 0x10;
		flags.set(HC, hc == 0x10);
		return result;
	}

	public void compare(int value1, int value2) {
		int result = (value1 - value2) & 0xFF;
		flags.set(SUB, true);
		flags.set(ZERO, result == 0);
		int hc = (value1 & 0x0F - value2 & 0x0F) & 0x10;
		flags.set(HC, hc == 0x10);
	}

	public boolean wasZero() {
		return flags.get(ZERO);
	}

	public boolean wasCarry() {
		return flags.get(C);
	}

	public int BCCTransform(int value) {
		// TODO: implement
		return 0;
	}

	public int complement(int value) {
		flags.set(SUB, true);
		flags.set(HC, true);
		return (~value) & 0xFF;
	}

	public int and(int value1, int value2) {
		flags.set(C, false);
		flags.set(SUB, false);
		flags.set(HC, true);
		int result = value1 & value2;
		flags.set(ZERO, result == 0);
		return result;
	}

	public int xor(int value1, int value2) {
		flags.clear();
		int result = value1 ^ value2;
		flags.set(ZERO, result == 0);
		return result;
	}

	public int or(int value1, int value2) {
		flags.clear();
		int result = value1 | value2;
		flags.set(ZERO, result == 0);
		return result;
	}

	public int shiftLeftSigned(int value) {
		int sign = value & 0x80;
		flags.set(C, sign == 0x80);
		flags.set(SUB, false);
		flags.set(HC, false);
		int result = (value << 1) & 0xFF;
		flags.set(ZERO, result == 0);
		return result;
	}

	public int shiftRightSigned(int value) {
		boolean sign = (value & 0x80) == 0x80;
		int zero = value & 0x01;
		flags.set(C, zero == 1);
		flags.set(HC, false);
		flags.set(SUB, false);
		int result = (value >>> 1);
		if(sign){
			result |= 0x80;
		}
		flags.set(ZERO, result == 0);
		return result;

	}

	public int shiftRight(int value) {
		flags.set(HC, false);
		flags.set(SUB, false);
		int zero = value & 0x01;
		flags.set(C, zero == 1);
		int result = value >>> 1;
		flags.set(ZERO, result == 0);
		return result;
	}

	public int swap(int value) {
		int n1 = value & 0xF0;
		int n2 = value & 0x0F;
		flags.clear();
		int result = n2 << 8 | n1 >>> 8;
		flags.set(ZERO, result == 0);
		return result;

	}

	public void setCarry() {
		flags.set(C);
	}

	public void clearCarry() {
		flags.set(C, false);
	}

	public void bitTest(int bit, int value) {
		flags.set(HC, true);
		flags.set(SUB, false);
		flags.set(ZERO, (value & (1 << bit)) == 0);
	}

	public int resetBit(int bit, int value) {
		return (value) & ~(0x01 << bit);
	}

	public int setBit(int bit, int value) {
		return value | (0x01 << bit);
	}

	public void reset() {
		flags.clear();
	}
	
}
