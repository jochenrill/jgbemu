package gbemu.cpu;

import java.util.BitSet;
import java.util.LinkedList;

import gbemu.cpu.Registers.REGISTERS;
import gbemu.graphics.GPU;
import gbemu.memory.IllegalMemoryAccessException;
import gbemu.memory.Memory;

public class CPU implements Runnable {

	private Registers r;
	private BitSet flags;
	private int sp;
	private int pc;
	private MMU m;
	private Clock clock;
	private ALU alu;
	private GPU gpu;
	private boolean interrupts;

	private int currentAddress;
	private boolean running;

	private LinkedList<Integer> instructionStack;
	private LinkedList<Integer> addressStack;
	private int opcode = -1;

	public CPU(Memory mem, GPU gpu) {

		instructionStack = new LinkedList<Integer>();
		addressStack = new LinkedList<Integer>();
		r = new Registers();
		m = new MMU(mem);
		clock = new Clock();
		alu = new ALU();
		flags = new BitSet(8);
		interrupts = false;
		this.gpu = gpu;
	}

	private void reset() {
		r.reset();
		flags.clear();
		sp = 0;
		pc = 0;
		interrupts = false;
	}

	@Override
	public void run() {
		running = true;

		while (running) {
			try {
				if (pc == 0x100) {
					m.leaveBios();
				}

				currentAddress = pc;
				opcode = nextInstruction();

				if (opcode != 0) {
					instructionStack.add(opcode);
					addressStack.add(currentAddress);
				}
				if (instructionStack.size() > 10) {
					instructionStack.remove();
				}
				if (addressStack.size() > 10) {
					addressStack.remove();
				}
				if (currentAddress == 0x284D) {
					// System.out.print("Stop.");
				}
				decode(opcode);
				if (interrupts) {
					handleInterrupts();
				}
			} catch (UnsupportedOpcodeException e) {
				System.out.println("Error while executing opcode "
						+ Integer.toHexString(opcode) + " at address "
						+ Integer.toHexString(currentAddress));
				System.out.println(e.getMessage());
				System.out.println("Previous instructions were:");
				for (int i = 0; i < instructionStack.size(); i++) {
					System.out.println("0x"
							+ Integer.toHexString(addressStack.get(i)) + ": "
							+ Integer.toHexString(instructionStack.get(i)));
				}
			} catch (IllegalMemoryAccessException
					| IllegalRegisterAccessException e) {
				System.out.println("Memory violation while executing opcode "
						+ Integer.toHexString(opcode) + " at address "
						+ Integer.toHexString(currentAddress));
				System.out.println(e.getMessage());
				System.out.println("Previous instructions were:");
				for (int i = 0; i < instructionStack.size(); i++) {
					System.out.println("0x"
							+ Integer.toHexString(addressStack.get(i)) + ": "
							+ Integer.toHexString(instructionStack.get(i)));
				}
			}
		}

	}

	private void handleInterrupts() throws IllegalMemoryAccessException {
		int a = m.readByte(0xFF0F);
	}

	private void decode(int opcode) throws UnsupportedOpcodeException,
			IllegalMemoryAccessException, IllegalRegisterAccessException {
		switch (opcode) {
		case 0x00:
			// NOP
			break;
		case 0x01:
			// LD BC nn
			r.setWord(REGISTERS.BC, m.readWord(pc));
			pc += 2;
			tick(3);
			break;
		case 0x02:
			// LD (BC) A
			m.writeByte(r.getWord(REGISTERS.BC), r.get(REGISTERS.A));
			tick(2);
			break;
		case 0x03:
			// INC BC
			r.setWord(REGISTERS.BC, alu.incWord(r.getWord(REGISTERS.BC)));
			tick(1);
			break;
		case 0x04:
			// INC B
			r.set(REGISTERS.B, alu.incByte(r.get(REGISTERS.B)));
			tick(1);
			break;
		case 0x05:
			// DEC B
			r.set(REGISTERS.B, alu.decByte(r.get(REGISTERS.B)));
			tick(1);
			break;
		case 0x06:
			// LD B n
			r.set(REGISTERS.B, m.readByte(pc++));
			tick(2);
			break;
		case 0x07:
			// RLC A
			r.set(REGISTERS.A, alu.rotateLeftCarry(r.get(REGISTERS.A)));
			tick(1);
			break;
		case 0x08:
			// LD (nn) SP
			int address = m.readWord(pc);
			pc += 2;
			m.writeWord(address, sp);
			tick(3);
			break;
		case 0x09:
			// ADD HL BC
			r.setWord(
					REGISTERS.HL,
					alu.addWords(r.getWord(REGISTERS.HL),
							r.getWord(REGISTERS.BC)));
			tick(3);
			break;
		case 0x0A:
			// LD A (BC)
			r.set(REGISTERS.A, m.readByte(r.getWord(REGISTERS.BC)));
			tick(2);
			break;
		case 0x0B:
			// DEC BC
			r.setWord(REGISTERS.BC, alu.decWord(r.getWord(REGISTERS.BC)));
			tick(1);
			break;
		case 0x0C:
			// INC C
			r.set(REGISTERS.C, alu.incByte(r.get(REGISTERS.C)));
			tick(1);
			break;
		case 0x0D:
			// DEC C
			r.set(REGISTERS.C, alu.decByte(r.get(REGISTERS.C)));
			tick(1);
			break;
		case 0x0E:
			// LD C n
			r.set(REGISTERS.C, m.readByte(pc++));
			tick(2);
			break;
		case 0x0F:
			// RRC A
			r.set(REGISTERS.A, alu.rotateRightCarry(r.get(REGISTERS.A)));
			tick(1);
			break;
		case 0x10:
			// STOP
			running = false;
			break;
		case 0x11:
			// LD DE nn
			r.setWord(REGISTERS.DE, m.readWord(pc));
			pc += 2;
			tick(3);
			break;
		case 0x12:
			// LD (DE) A
			m.writeByte(r.getWord(REGISTERS.DE), r.get(REGISTERS.A));
			tick(2);
			break;
		case 0x13:
			// INC DE
			r.setWord(REGISTERS.DE, alu.incWord(r.getWord(REGISTERS.DE)));
			tick(1);
			break;
		case 0x14:
			// INC D
			r.set(REGISTERS.D, alu.incByte(r.get(REGISTERS.D)));
			tick(1);
			break;
		case 0x15:
			// DEC D
			r.set(REGISTERS.D, alu.decByte(r.get(REGISTERS.D)));
			tick(1);
			break;
		case 0x16:
			// LD D n
			r.set(REGISTERS.D, m.readByte(pc++));
			tick(2);
			break;
		case 0x17:
			// RL A
			r.set(REGISTERS.A, alu.rotateLeft(r.get(REGISTERS.A)));
			tick(1);
			break;
		case 0x18:
			// JR n (signed)
			int foo = (byte) m.readByte(pc++);
			pc += foo;
			tick(1);
			break;
		case 0x19:
			// ADD HL DE
			r.setWord(
					REGISTERS.HL,
					alu.addWords(r.getWord(REGISTERS.HL),
							r.getWord(REGISTERS.DE)));
			tick(3);
			break;
		case 0x1A:
			// LD A (DE)
			r.set(REGISTERS.A, m.readByte(r.getWord(REGISTERS.DE)));
			tick(2);
			break;
		case 0x1B:
			// DEC DE
			r.setWord(REGISTERS.DE, alu.decWord(r.get(REGISTERS.DE)));
			tick(1);
			break;
		case 0x1C:
			// INC E
			r.set(REGISTERS.E, alu.incByte(r.get(REGISTERS.E)));
			tick(1);
			break;
		case 0x1D:
			// DEC E
			r.set(REGISTERS.E, alu.decByte(r.get(REGISTERS.E)));
			tick(1);
			break;
		case 0x1E:
			// LD E n
			r.set(REGISTERS.E, m.readByte(pc++));
			tick(2);
			break;
		case 0x1F:
			// RR A
			r.set(REGISTERS.A, alu.rotateRight(r.get(REGISTERS.A)));
			tick(1);
			break;
		case 0x20:
			// JR NZ n
			int j = (byte) m.readByte(pc++);
			if (!alu.wasZero()) {
				pc += j;
			}
			tick(1);
			break;
		case 0x21:
			// LD HL nn
			r.setWord(REGISTERS.HL, m.readWord(pc));
			pc += 2;
			tick(3);
			break;
		case 0x22:
			// LDI (HL) A
			m.writeByte(r.getWord(REGISTERS.HL), r.get(REGISTERS.A));
			r.setWord(REGISTERS.HL, alu.incWord(r.getWord(REGISTERS.HL)));
			tick(2);
			break;
		case 0x23:
			// INC HL
			r.setWord(REGISTERS.HL, alu.incWord(r.getWord(REGISTERS.HL)));
			tick(1);
			break;
		case 0x24:
			// INC H
			r.set(REGISTERS.H, alu.incByte(r.get(REGISTERS.H)));
			tick(1);
			break;
		case 0x25:
			// DEC H
			r.set(REGISTERS.H, alu.decByte(r.get(REGISTERS.H)));
			tick(1);
			break;
		case 0x26:
			// LD H n
			r.set(REGISTERS.H, m.readByte(pc++));
			tick(2);
			break;
		case 0x27:
			// DAA
			r.set(REGISTERS.A, alu.BCCTransform(r.get(REGISTERS.A)));
			tick(1);
			break;
		case 0x28:
			// JR Z n
			int value = (byte) m.readByte(pc++);
			if (alu.wasZero()) {
				pc += value;
			}
			tick(2);
			break;
		case 0x29:
			// ADD HL HL
			r.setWord(
					REGISTERS.HL,
					alu.addWords(r.getWord(REGISTERS.HL),
							r.getWord(REGISTERS.HL)));
			tick(2);
			break;
		case 0x2A:
			// LDI A (HL)
			r.set(REGISTERS.A, m.readByte(r.getWord(REGISTERS.HL)));
			r.setWord(REGISTERS.HL, alu.incWord(r.getWord(REGISTERS.HL)));
			tick(2);
			break;
		case 0x2B:
			// DEC HL
			r.setWord(REGISTERS.HL, alu.decWord(r.getWord(REGISTERS.HL)));
			tick(1);
			break;
		case 0x2C:
			// INC L
			r.set(REGISTERS.L, alu.incByte(r.get(REGISTERS.L)));
			tick(1);
			break;
		case 0x2D:
			// DEC L
			r.set(REGISTERS.L, alu.decByte(r.get(REGISTERS.L)));
			tick(1);
			break;
		case 0x2E:
			// LD L n
			r.set(REGISTERS.L, m.readByte(pc++));
			tick(2);
			break;
		case 0x2F:
			// CPL
			r.set(REGISTERS.L, alu.complement(r.get(REGISTERS.L)));
			tick(1);
			break;
		case 0x30:
			// JR NC n
			value = (byte) m.readByte(pc++);
			if (alu.wasCarry()) {
				pc += value;
			}
			break;
		case 0x31:
			// LD SP nn
			sp = m.readWord(pc);
			pc += 2;
			tick(3);
			break;
		case 0x32:
			// LDD (HL) A
			m.writeByte(r.getWord(REGISTERS.HL), r.get(REGISTERS.A));
			r.setWord(REGISTERS.HL, alu.decWord(r.getWord(REGISTERS.HL)));
			tick(2);
			break;
		case 0x33:
			sp++;
			tick(1);
			break;
		case 0x34:
			// INC (HL)
			m.writeByte(r.getWord(REGISTERS.HL),
					alu.incByte(m.readByte(r.getWord(REGISTERS.HL))));
			tick(3);
			break;
		case 0x35:
			// DEC (HL)
			m.writeByte(r.getWord(REGISTERS.HL),
					alu.decByte(m.readByte(r.getWord(REGISTERS.HL))));
			tick(3);
			break;
		case 0x36:
			// LD (HL) n
			m.writeByte(r.getWord(REGISTERS.HL), m.readByte(pc++));
			tick(2);
			break;
		case 0x37:
			// SCF
			alu.setCarry();
			tick(1);
			break;
		case 0x38:
			// JR C n
			value = (byte) m.readByte(pc++);
			if (alu.wasCarry()) {
				pc += value;
			}
			tick(2);
			break;
		case 0x39:
			// ADD HL SP
			r.setWord(REGISTERS.HL, alu.addWords(r.getWord(REGISTERS.HL), sp));
			tick(12);
			break;
		case 0x3A:
			// LDD A (HL)
			r.set(REGISTERS.A, m.readByte(r.getWord(REGISTERS.HL)));
			r.setWord(REGISTERS.HL, alu.decWord(r.getWord(REGISTERS.HL)));
			tick(2);
			break;
		case 0x3B:
			// DEC SP
			sp--;
			tick(1);
			break;
		case 0x3C:
			// INC A
			r.set(REGISTERS.A, alu.incByte(r.get(REGISTERS.A)));
			tick(1);
			break;
		case 0x3D:
			// DEC A
			r.set(REGISTERS.A, alu.decByte(r.get(REGISTERS.A)));
			tick(1);
			break;
		case 0x3E:
			// LD A n
			r.set(REGISTERS.A, m.readByte(pc++));
			tick(2);
			break;
		case 0x3F:
			// CCF
			alu.clearCarry();
			tick(1);
			break;
		case 0x40:
			// LD B B
			r.set(REGISTERS.B, r.get(REGISTERS.B));
			tick(2);
			break;
		case 0x41:
			// LD B C
			r.set(REGISTERS.B, r.get(REGISTERS.C));
			tick(2);
			break;
		case 0x42:
			// LD B D
			r.set(REGISTERS.B, r.get(REGISTERS.D));
			tick(2);
			break;
		case 0x43:
			// LD B E
			r.set(REGISTERS.B, r.get(REGISTERS.E));
			tick(2);
			break;
		case 0x44:
			// LD B H
			r.set(REGISTERS.B, r.get(REGISTERS.H));
			tick(2);
			break;
		case 0x45:
			// LD B L
			r.set(REGISTERS.B, r.get(REGISTERS.L));
			tick(2);
			break;
		case 0x46:
			// LD B (HL)
			r.set(REGISTERS.B, m.readByte(r.getWord(REGISTERS.HL)));
			tick(2);
			break;
		case 0x47:
			// LD B A
			r.set(REGISTERS.B, r.get(REGISTERS.A));
			tick(2);
			break;
		case 0x48:
			// LD C B
			r.set(REGISTERS.C, r.get(REGISTERS.B));
			tick(2);
			break;
		case 0x49:
			// LD C C
			r.set(REGISTERS.C, r.get(REGISTERS.C));
			tick(2);
			break;
		case 0x4A:
			// LD C D
			r.set(REGISTERS.C, r.get(REGISTERS.D));
			tick(2);
			break;
		case 0x4B:
			// LD C E
			r.set(REGISTERS.C, r.get(REGISTERS.E));
			tick(2);
			break;
		case 0x4C:
			// LD C H
			r.set(REGISTERS.C, r.get(REGISTERS.H));
			tick(2);
			break;
		case 0x4D:
			// LD C L
			r.set(REGISTERS.C, r.get(REGISTERS.L));
			tick(2);
			break;
		case 0x4E:
			// LD C (HL)
			r.set(REGISTERS.C, m.readByte(r.getWord(REGISTERS.HL)));
			tick(2);
			break;
		case 0x4F:
			// LD C A
			r.set(REGISTERS.C, r.get(REGISTERS.A));
			tick(2);
			break;
		case 0x50:
			// LD D B
			r.set(REGISTERS.D, r.get(REGISTERS.B));
			tick(2);
			break;
		case 0x51:
			// LD D C
			r.set(REGISTERS.D, r.get(REGISTERS.C));
			tick(2);
			break;
		case 0x52:
			// LD D D
			r.set(REGISTERS.D, r.get(REGISTERS.D));
			tick(2);
			break;
		case 0x53:
			// LD D E
			r.set(REGISTERS.D, r.get(REGISTERS.E));
			tick(2);
			break;
		case 0x54:
			// LD D H
			r.set(REGISTERS.D, r.get(REGISTERS.H));
			tick(2);
			break;
		case 0x55:
			// LD D L
			r.set(REGISTERS.D, r.get(REGISTERS.L));
			tick(2);
			break;
		case 0x56:
			// LD D (HL)
			r.set(REGISTERS.D, m.readByte(r.getWord(REGISTERS.HL)));
			tick(2);
			break;
		case 0x57:
			// LD D A
			r.set(REGISTERS.D, r.get(REGISTERS.A));
			tick(2);
			break;
		case 0x58:
			// LD E B
			r.set(REGISTERS.E, r.get(REGISTERS.B));
			tick(2);
			break;
		case 0x59:
			// LD E C
			r.set(REGISTERS.E, r.get(REGISTERS.C));
			tick(2);
			break;
		case 0x5A:
			// LD E D
			r.set(REGISTERS.E, r.get(REGISTERS.D));
			tick(2);
			break;
		case 0x5B:
			// LD E E
			r.set(REGISTERS.E, r.get(REGISTERS.E));
			tick(2);
			break;
		case 0x5C:
			// LD E H
			r.set(REGISTERS.E, r.get(REGISTERS.H));
			tick(2);
			break;
		case 0x5D:
			// LD E L
			r.set(REGISTERS.E, r.get(REGISTERS.L));
			tick(2);
			break;
		case 0x5E:
			// LD E (HL)
			r.set(REGISTERS.E, m.readByte(r.getWord(REGISTERS.HL)));
			tick(2);
			break;
		case 0x5F:
			// LD E A
			r.set(REGISTERS.E, r.get(REGISTERS.A));
			tick(2);
			break;
		case 0x60:
			// LD H B
			r.set(REGISTERS.H, r.get(REGISTERS.B));
			tick(2);
			break;
		case 0x61:
			// LD H C
			r.set(REGISTERS.H, r.get(REGISTERS.C));
			tick(2);
			break;
		case 0x62:
			// LD H D
			r.set(REGISTERS.H, r.get(REGISTERS.D));
			tick(2);
			break;
		case 0x63:
			// LD H E
			r.set(REGISTERS.H, r.get(REGISTERS.E));
			tick(2);
			break;
		case 0x64:
			// LD H H
			r.set(REGISTERS.H, r.get(REGISTERS.H));
			tick(2);
			break;
		case 0x65:
			// LD H L
			r.set(REGISTERS.H, r.get(REGISTERS.L));
			tick(2);
			break;
		case 0x66:
			// LD H (HL)
			r.set(REGISTERS.H, m.readByte(r.getWord(REGISTERS.HL)));
			tick(2);
			break;
		case 0x67:
			// LD H A
			r.set(REGISTERS.H, r.get(REGISTERS.A));
			tick(2);
			break;
		case 0x68:
			// LD L B
			r.set(REGISTERS.L, r.get(REGISTERS.B));
			tick(2);
			break;
		case 0x69:
			// LD L C
			r.set(REGISTERS.L, r.get(REGISTERS.C));
			tick(2);
			break;
		case 0x6A:
			// LD L D
			r.set(REGISTERS.L, r.get(REGISTERS.D));
			tick(2);
			break;
		case 0x6B:
			// LD L E
			r.set(REGISTERS.L, r.get(REGISTERS.E));
			tick(2);
			break;
		case 0x6C:
			// LD L H
			r.set(REGISTERS.L, r.get(REGISTERS.H));
			tick(2);
			break;
		case 0x6D:
			// LD L B
			r.set(REGISTERS.L, r.get(REGISTERS.L));
			tick(2);
			break;
		case 0x6E:
			// LD L (HL)
			r.set(REGISTERS.L, m.readByte(r.getWord(REGISTERS.HL)));
			tick(2);
			break;
		case 0x6F:
			// LD L A
			r.set(REGISTERS.L, r.get(REGISTERS.A));
			tick(2);
			break;
		case 0x70:
			// LD (HL) B
			m.writeByte(r.getWord(REGISTERS.HL), r.get(REGISTERS.B));
			tick(2);
			break;
		case 0x71:
			// LD (HL) C
			m.writeByte(r.getWord(REGISTERS.HL), r.get(REGISTERS.C));
			tick(2);
			break;
		case 0x72:
			// LD (HL) D
			m.writeByte(r.getWord(REGISTERS.HL), r.get(REGISTERS.D));
			tick(2);
			break;
		case 0x73:
			// LD (HL) E
			m.writeByte(r.getWord(REGISTERS.HL), r.get(REGISTERS.E));
			tick(2);
			break;
		case 0x74:
			// LD (HL) H
			m.writeByte(r.getWord(REGISTERS.HL), r.get(REGISTERS.H));
			tick(2);
			break;
		case 0x75:
			// LD (HL) L
			m.writeByte(r.getWord(REGISTERS.HL), r.get(REGISTERS.L));
			tick(2);
			break;
		case 0x76:
			// HALT
			pc--;
			break;
		case 0x77:
			// LD (HL) A
			m.writeByte(r.getWord(REGISTERS.HL), r.get(REGISTERS.A));
			tick(2);
			break;
		case 0x78:
			// LD A B
			r.set(REGISTERS.A, r.get(REGISTERS.B));
			tick(2);
			break;
		case 0x79:
			// LD A C
			r.set(REGISTERS.A, r.get(REGISTERS.C));
			tick(2);
			break;
		case 0x7A:
			// LD A D
			r.set(REGISTERS.A, r.get(REGISTERS.D));
			tick(2);
			break;
		case 0x7B:
			// LD A E
			r.set(REGISTERS.A, r.get(REGISTERS.E));
			tick(2);
			break;
		case 0x7C:
			// LD A H
			r.set(REGISTERS.A, r.get(REGISTERS.H));
			tick(2);
			break;
		case 0x7D:
			// LD A L
			r.set(REGISTERS.A, r.get(REGISTERS.L));
			tick(2);
			break;
		case 0x7E:
			// LD A (HL)
			r.set(REGISTERS.A, m.readByte(r.getWord(REGISTERS.HL)));
			tick(2);
			break;
		case 0x7F:
			// LD A A
			r.set(REGISTERS.A, r.get(REGISTERS.A));
			tick(2);
			break;
		case 0x80:
			// ADD A B
			r.set(REGISTERS.A,
					alu.addByte(r.get(REGISTERS.A), r.get(REGISTERS.B)));
			tick(2);
			break;
		case 0x81:
			// ADD A C
			r.set(REGISTERS.A,
					alu.addByte(r.get(REGISTERS.A), r.get(REGISTERS.C)));
			tick(2);
			break;
		case 0x82:
			// ADD A D
			r.set(REGISTERS.A,
					alu.addByte(r.get(REGISTERS.A), r.get(REGISTERS.D)));
			tick(2);
			break;
		case 0x83:
			// ADD A E
			r.set(REGISTERS.A,
					alu.addByte(r.get(REGISTERS.A), r.get(REGISTERS.E)));
			tick(1);
			break;
		case 0x84:
			// ADD A H
			r.set(REGISTERS.A,
					alu.addByte(r.get(REGISTERS.A), r.get(REGISTERS.H)));
			tick(2);
			break;
		case 0x85:
			// ADD A L
			r.set(REGISTERS.A,
					alu.addByte(r.get(REGISTERS.A), r.get(REGISTERS.L)));
			tick(2);
			break;
		case 0x86:
			// ADD A (HL)
			r.set(REGISTERS.A,
					alu.addByte(r.get(REGISTERS.A),
							m.readByte(r.getWord(REGISTERS.HL))));
			tick(2);
			break;
		case 0x87:
			// ADD A A
			r.set(REGISTERS.A,
					alu.addByte(r.get(REGISTERS.A), r.get(REGISTERS.A)));
			tick(2);
			break;
		case 0x88:
			// ADC A B
			r.set(REGISTERS.A, alu.addByteWithCarry(r.get(REGISTERS.B),
					r.get(REGISTERS.A)));
			break;
		case 0x89:
			// ADC A C
			r.set(REGISTERS.A, alu.addByteWithCarry(r.get(REGISTERS.C),
					r.get(REGISTERS.A)));
			break;
		case 0x8A:
			// ADC A D
			r.set(REGISTERS.A, alu.addByteWithCarry(r.get(REGISTERS.D),
					r.get(REGISTERS.A)));
			break;
		case 0x8B:
			// ADC A E
			r.set(REGISTERS.A, alu.addByteWithCarry(r.get(REGISTERS.E),
					r.get(REGISTERS.A)));
			break;
		case 0x8C:
			// ADC A H
			r.set(REGISTERS.A, alu.addByteWithCarry(r.get(REGISTERS.H),
					r.get(REGISTERS.A)));
			break;
		case 0x8D:
			// ADC A L
			r.set(REGISTERS.A, alu.addByteWithCarry(r.get(REGISTERS.L),
					r.get(REGISTERS.A)));
			break;
		case 0x8E:
			// ADC A (HL)
			r.set(REGISTERS.A,
					alu.addByteWithCarry(m.readByte(r.getWord(REGISTERS.HL)),
							r.get(REGISTERS.A)));
			break;
		case 0x8F:
			// ADC A A
			r.set(REGISTERS.A, alu.addByteWithCarry(r.get(REGISTERS.A),
					r.get(REGISTERS.A)));
			break;
		case 0x90:
			// SUB A B
			r.set(REGISTERS.A,
					alu.subByte(r.get(REGISTERS.A), r.get(REGISTERS.B)));
			tick(2);
			break;
		case 0x91:
			// SUB A C
			r.set(REGISTERS.A,
					alu.subByte(r.get(REGISTERS.A), r.get(REGISTERS.C)));
			tick(2);
			break;
		case 0x92:
			// SUB A D
			r.set(REGISTERS.A,
					alu.subByte(r.get(REGISTERS.A), r.get(REGISTERS.D)));
			tick(2);
			break;
		case 0x93:
			// SUB A E
			r.set(REGISTERS.A,
					alu.subByte(r.get(REGISTERS.A), r.get(REGISTERS.E)));
			tick(2);
			break;
		case 0x94:
			// SUB A H
			r.set(REGISTERS.A,
					alu.subByte(r.get(REGISTERS.A), r.get(REGISTERS.H)));
			tick(2);
			break;
		case 0x95:
			// SUB A L
			r.set(REGISTERS.A,
					alu.subByte(r.get(REGISTERS.A), r.get(REGISTERS.L)));
			tick(2);
			break;
		case 0x96:
			// SUB A HL
			r.set(REGISTERS.A,
					alu.subByte(r.get(REGISTERS.A),
							m.readByte(r.getWord(REGISTERS.HL))));
			tick(2);
			break;
		case 0x97:
			// SUB A A
			r.set(REGISTERS.A,
					alu.subByte(r.get(REGISTERS.A), r.get(REGISTERS.A)));
			tick(2);
			break;
		case 0x98:
			// SBC A B
			r.set(REGISTERS.A, alu.subByteWithCarry(r.get(REGISTERS.A),
					r.get(REGISTERS.B)));
			tick(2);
			break;
		case 0x99:
			// SBC A C
			r.set(REGISTERS.A, alu.subByteWithCarry(r.get(REGISTERS.A),
					r.get(REGISTERS.C)));
			tick(2);
			break;
		case 0x9A:
			// SBC A D
			r.set(REGISTERS.A, alu.subByteWithCarry(r.get(REGISTERS.A),
					r.get(REGISTERS.D)));
			tick(2);
			break;
		case 0x9B:
			// SBC A E
			r.set(REGISTERS.A, alu.subByteWithCarry(r.get(REGISTERS.A),
					r.get(REGISTERS.E)));
			tick(2);
			break;
		case 0x9C:
			// SBC A H
			r.set(REGISTERS.A, alu.subByteWithCarry(r.get(REGISTERS.A),
					r.get(REGISTERS.H)));
			tick(2);
			break;
		case 0x9D:
			// SBC A L
			r.set(REGISTERS.A, alu.subByteWithCarry(r.get(REGISTERS.A),
					r.get(REGISTERS.L)));
			tick(2);
			break;
		case 0x9E:
			// SBC A (HL)
			r.set(REGISTERS.A,
					alu.subByteWithCarry(r.get(REGISTERS.A),
							m.readByte(r.getWord(REGISTERS.HL))));
			break;
		case 0x9F:
			// SBC A A
			r.set(REGISTERS.A, alu.subByteWithCarry(r.get(REGISTERS.A),
					r.get(REGISTERS.A)));
			tick(2);
			break;
		case 0xA0:
			// AND B
			r.set(REGISTERS.A, alu.and(r.get(REGISTERS.A), r.get(REGISTERS.B)));
			tick(1);
			break;
		case 0xA1:
			// AND C
			r.set(REGISTERS.A, alu.and(r.get(REGISTERS.A), r.get(REGISTERS.C)));
			tick(1);
			break;
		case 0xA2:
			// AND D
			r.set(REGISTERS.A, alu.and(r.get(REGISTERS.A), r.get(REGISTERS.D)));
			tick(1);
			break;
		case 0xA3:
			// AND E
			r.set(REGISTERS.A, alu.and(r.get(REGISTERS.A), r.get(REGISTERS.E)));
			tick(1);
			break;
		case 0xA4:
			// AND H
			r.set(REGISTERS.A, alu.and(r.get(REGISTERS.A), r.get(REGISTERS.H)));
			tick(1);
			break;
		case 0xA5:
			// AND L
			r.set(REGISTERS.A, alu.and(r.get(REGISTERS.A), r.get(REGISTERS.L)));
			tick(1);
			break;
		case 0xA6:
			// AND (HL)
			r.set(REGISTERS.A,
					alu.and(r.get(REGISTERS.A),
							m.readByte(r.getWord(REGISTERS.HL))));
			tick(2);
			break;
		case 0xA7:
			// AND A
			r.set(REGISTERS.A, alu.and(r.get(REGISTERS.A), r.get(REGISTERS.A)));
			tick(1);
			break;
		case 0xA8:
			// XOR B
			r.set(REGISTERS.A, alu.xor(r.get(REGISTERS.A), r.get(REGISTERS.B)));
			tick(1);
			break;
		case 0xA9:
			// XOR C
			r.set(REGISTERS.A, alu.xor(r.get(REGISTERS.A), r.get(REGISTERS.C)));
			tick(1);
			break;
		case 0xAA:
			// XOR D
			r.set(REGISTERS.A, alu.xor(r.get(REGISTERS.A), r.get(REGISTERS.D)));
			tick(1);
			break;
		case 0xAB:
			// XOR E
			r.set(REGISTERS.A, alu.xor(r.get(REGISTERS.A), r.get(REGISTERS.E)));
			tick(1);
			break;
		case 0xAC:
			// XOR B
			r.set(REGISTERS.A, alu.xor(r.get(REGISTERS.A), r.get(REGISTERS.H)));
			tick(1);
			break;
		case 0xAD:
			// XOR B
			r.set(REGISTERS.A, alu.xor(r.get(REGISTERS.A), r.get(REGISTERS.L)));
			tick(1);
			break;
		case 0xAE:
			// XOR (HL)
			r.set(REGISTERS.A,
					alu.xor(r.get(REGISTERS.A),
							m.readByte(r.getWord(REGISTERS.HL))));
			tick(2);
			break;
		case 0xAF:
			// XOR A
			r.set(REGISTERS.A, alu.xor(r.get(REGISTERS.A), r.get(REGISTERS.A)));
			tick(1);
			break;
		case 0xB0:
			// OR B
			r.set(REGISTERS.A, alu.or(r.get(REGISTERS.A), r.get(REGISTERS.B)));
			tick(1);
			break;
		case 0xB1:
			// OR C
			r.set(REGISTERS.A, alu.or(r.get(REGISTERS.A), r.get(REGISTERS.C)));
			tick(1);
			break;
		case 0xB2:
			// OR D
			r.set(REGISTERS.A, alu.or(r.get(REGISTERS.A), r.get(REGISTERS.D)));
			tick(1);
			break;
		case 0xB3:
			// OR E
			r.set(REGISTERS.A, alu.or(r.get(REGISTERS.A), r.get(REGISTERS.E)));
			tick(1);
			break;
		case 0xB4:
			// OR H
			r.set(REGISTERS.A, alu.or(r.get(REGISTERS.A), r.get(REGISTERS.H)));
			tick(1);
			break;
		case 0xB5:
			// OR L
			r.set(REGISTERS.A, alu.or(r.get(REGISTERS.A), r.get(REGISTERS.L)));
			tick(1);
			break;
		case 0xB6:
			// OR (HL)
			r.set(REGISTERS.A,
					alu.or(r.get(REGISTERS.A),
							m.readByte(r.getWord(REGISTERS.HL))));
			tick(2);
			break;
		case 0xB7:
			// OR A
			r.set(REGISTERS.A, alu.or(r.get(REGISTERS.A), r.get(REGISTERS.A)));
			tick(1);
			break;
		case 0xB8:
			// CP B
			alu.compare(r.get(REGISTERS.A), r.get(REGISTERS.B));
			tick(1);
			break;
		case 0xB9:
			// CP C
			alu.compare(r.get(REGISTERS.A), r.get(REGISTERS.C));
			tick(1);
			break;
		case 0xBA:
			// CP D
			alu.compare(r.get(REGISTERS.A), r.get(REGISTERS.D));
			tick(1);
			break;
		case 0xBB:
			// CP E
			alu.compare(r.get(REGISTERS.A), r.get(REGISTERS.E));
			tick(1);
			break;
		case 0xBC:
			// CP H
			alu.compare(r.get(REGISTERS.A), r.get(REGISTERS.H));
			tick(1);
			break;
		case 0xBD:
			// CP L
			alu.compare(r.get(REGISTERS.A), r.get(REGISTERS.L));
			tick(1);
			break;
		case 0xBE:
			// CP (HL)

			alu.compare(r.get(REGISTERS.A), m.readByte(r.getWord(REGISTERS.HL)));
			tick(1);
			break;
		case 0xBF:
			// CP A

			alu.compare(r.get(REGISTERS.A), r.get(REGISTERS.A));
			tick(1);
			break;
		case 0xC0:
			// RET NZ
			if (!alu.wasZero()) {
				pc = m.readWord(sp);
				sp += 2;
			}
			tick(3);
			break;
		case 0xC1:
			// POP BC
			r.setWord(REGISTERS.BC, m.readWord(sp));
			sp += 2;
			tick(3);
			break;
		case 0xC2:
			// JP NZ nn
			value = m.readWord(pc);
			pc += 2;
			if (!alu.wasZero()) {
				pc = value;
			}
			tick(3);
			break;
		case 0xC3:
			// JP nn
			pc = m.readWord(pc);
			tick(3);
			break;
		case 0xC4:
			// CALL NZ nn
			value = m.readWord(pc);
			pc += 2;
			if (!alu.wasZero()) {
				// save PC
				sp -= 2;
				m.writeWord(sp, pc);
				pc = value;
			}
			tick(5);
			break;
		case 0xC5:
			// PUSH BC
			sp -= 2;
			m.writeWord(sp, r.getWord(REGISTERS.BC));
			tick(3);
			break;
		case 0xC6:
			// ADD A n
			r.set(REGISTERS.A,
					alu.addByte(r.get(REGISTERS.A), m.readByte(pc++)));
			tick(2);
			break;
		case 0xC7:
			// RST 0
			sp -= 2;
			m.writeWord(sp, pc);
			pc = 0x0000;
			tick(3);
			break;
		case 0xC8:
			// RET Z
			if (alu.wasZero()) {
				pc = m.readWord(sp);
				sp += 2;
			}
			tick(3);
			break;
		case 0xC9:
			// RET
			pc = m.readWord(sp);
			sp += 2;
			tick(3);
			break;
		case 0xCA:
			// JP Z nn
			if (alu.wasZero()) {
				pc = m.readWord(pc);
			} else {
				pc += 2;
			}
			tick(3);
			break;
		case 0xCB:
			// EXT OP
			decodeExtendedOperation(nextInstruction());
			break;
		case 0xCC:
			// CALL Z nn
			value = m.readWord(pc);
			pc += 2;
			if (alu.wasZero()) {
				// save PC
				sp -= 2;
				m.writeWord(sp, pc);
				pc = value;
			}
			tick(5);
			break;
		case 0xCD:
			// CALL nn
			value = m.readWord(pc);
			pc += 2;
			sp -= 2;
			m.writeWord(sp, pc);
			pc = value;
			tick(5);
			break;
		case 0xCE:
			// ADC A n
			r.set(REGISTERS.A,
					alu.addByteWithCarry(r.get(REGISTERS.A), m.readByte(pc++)));
			tick(2);
			break;
		case 0xCF:
			// RST 8
			sp -= 2;
			m.writeWord(sp, pc);
			pc = 0x0008;
			tick(3);
			break;
		case 0xD0:
			// RET NC
			if (!alu.wasCarry()) {
				pc = m.readWord(sp);
				sp += 2;
			}
			tick(3);
			break;
		case 0xD1:
			// POP DE
			r.setWord(REGISTERS.DE, m.readWord(sp));
			sp += 2;
			tick(2);
			break;
		case 0xD2:
			// JP NC nn
			if (!alu.wasCarry()) {
				pc = m.readWord(pc);
			} else {
				pc += 2;
			}
			tick(3);
			break;
		case 0xD3:
			throw new UnsupportedOpcodeException("Operation with opcode "
					+ opcode + " is removed. ");
		case 0xD4:
			// CALL NC nn
			value = m.readWord(pc);
			pc += 2;
			if (!alu.wasCarry()) {
				// save PC
				sp -= 2;
				m.writeWord(sp, pc);
				pc = value;
			}
			tick(5);
			break;
		case 0xD5:
			// PUSH DE
			sp -= 2;
			m.writeWord(sp, r.getWord(REGISTERS.DE));
			tick(2);
			break;
		case 0xD6:
			// SUB A n
			r.set(REGISTERS.A,
					alu.subByte(r.get(REGISTERS.A), m.readByte(pc++)));
			tick(2);
			break;
		case 0xD7:
			// RST 10
			sp -= 2;
			m.writeWord(sp, pc);
			pc = 0x0010;
			tick(3);
			break;
		case 0xD8:
			// RET C
			if (alu.wasCarry()) {
				pc = m.readWord(sp);
				sp += 2;
			}
			tick(3);
			break;
		case 0xD9:
			// RETI
			interrupts = true;
			pc = m.readWord(sp);
			sp += 2;
			tick(3);
			break;
		case 0xDA:
			// JP C nn
			if (alu.wasCarry()) {
				pc = m.readWord(pc);
			} else {
				pc += 2;
			}
			tick(3);
			break;
		case 0xDB:
			throw new UnsupportedOpcodeException("Operation with opcode "
					+ opcode + " is removed. ");
		case 0xDC:
			// CALL C nn
			value = m.readWord(pc);
			pc += 2;
			if (alu.wasCarry()) {
				// save PC
				sp -= 2;
				m.writeWord(sp, pc);
				pc = value;
			}
			tick(5);
			break;
		case 0xDD:
			throw new UnsupportedOpcodeException("Operation with opcode "
					+ opcode + " is removed. ");
		case 0xDE:
			// SBC A n
			r.set(REGISTERS.A,
					alu.subByteWithCarry(r.get(REGISTERS.A), m.readByte(pc++)));
			tick(2);
			break;
		case 0xDF:
			// RST 18
			sp -= 2;
			m.writeWord(sp, pc);
			pc = 0x0018;
			tick(3);
			break;
		case 0xE0:
			// LDH (n) A
			m.writeByte(m.readByte(pc++) + 0xFF00, r.get(REGISTERS.A));
			tick(2);
			break;
		case 0xE1:
			// POP HL
			r.setWord(REGISTERS.HL, m.readWord(sp));
			sp += 2;
			tick(2);
			break;
		case 0xE2:
			// LDH (C) A
			m.writeByte(r.get(REGISTERS.C) + 0xFF00, r.get(REGISTERS.A));
			tick(2);
			break;
		case 0xE3:
		case 0xE4:
			throw new UnsupportedOpcodeException("Operation with opcode "
					+ opcode + " is removed. ");
		case 0xE5:
			// PUSH HL
			sp -= 2;
			m.writeWord(sp, r.getWord(REGISTERS.HL));
			tick(2);
			break;
		case 0xE6:
			// AND n
			r.set(REGISTERS.A, alu.and(r.get(REGISTERS.A), m.readByte(pc++)));
			tick(2);
			break;
		case 0xE7:
			// RST 20
			sp -= 2;
			m.writeWord(sp, pc);
			pc = 0x0020;
			tick(3);
			break;
		case 0xE8:
			// ADD SP d
			sp += (byte) m.readByte(pc++);
			tick(2);
			break;
		case 0xE9:
			// JP HL
			pc = r.getWord(REGISTERS.HL);
			tick(2);
			break;
		case 0xEA:
			// LD (nn) A
			m.writeByte(m.readWord(pc), r.get(REGISTERS.A));
			pc += 2;
			tick(3);
			break;
		case 0xEB:
		case 0xEC:
		case 0xED:
			throw new UnsupportedOpcodeException("Operation with opcode "
					+ opcode + " is removed. ");
		case 0xEE:
			// XOR n
			r.set(REGISTERS.A, alu.xor(r.get(REGISTERS.A), m.readByte(pc++)));
			tick(2);
			break;
		case 0xEF:
			// RST 28
			sp -= 2;
			m.writeWord(sp, pc);
			pc = 0x0028;
			tick(3);
			break;
		case 0xF0:
			// LDH A (n)
			r.set(REGISTERS.A, m.readByte(m.readByte(pc++) + 0xFF00));
			tick(3);
			break;
		case 0xF1:
			// POP AF
			alu.setFlagRegister(m.readByte(sp++));
			r.set(REGISTERS.A, m.readByte(sp++));
			tick(2);
			break;
		case 0xF2:
		case 0xF4:
			throw new UnsupportedOpcodeException("Operation with opcode "
					+ opcode + " is removed. ");
		case 0xF3:
			// DI
			interrupts = false;
			tick(1);
			break;
		case 0xF5:
			// PUSH AF
			sp -= 2;
			m.writeByte(sp, alu.getFlagRegister());
			m.writeByte(sp + 1, r.get(REGISTERS.A));
			tick(2);
			break;
		case 0xF6:
			// OR n
			r.set(REGISTERS.A, alu.or(r.get(REGISTERS.A), m.readByte(pc++)));
			tick(2);
			break;
		case 0xF7:
			// RST 30
			sp -= 2;
			m.writeWord(sp, pc);
			pc = 0x0030;
			tick(3);
			break;
		case 0xF8:
			// LDHL SP d
			r.setWord(REGISTERS.HL, sp + ((byte) m.readByte(pc++)));
			tick(3);
			break;
		case 0xF9:
			// LD SP HL
			sp = r.getWord(REGISTERS.HL);
			tick(1);
			break;
		case 0xFA:
			// LD A (nn)
			value = m.readWord(pc);
			pc += 2;
			r.set(REGISTERS.A, m.readByte(value));
			tick(3);
			break;
		case 0xFB:
			// EI
			interrupts = true;
			tick(1);
			break;
		case 0xFC:
		case 0xFD:
			throw new UnsupportedOpcodeException("Operation with opcode "
					+ opcode + " is removed. ");
		case 0xFE:
			// CP n
			alu.compare(r.get(REGISTERS.A), m.readByte(pc++));
			tick(2);
			break;
		case 0xFF:
			// RST 38
			sp -= 2;
			m.writeWord(sp, pc);
			pc = 0x0038;
			tick(3);
			break;
		default:
			throw new UnsupportedOpcodeException("Operation with opcode "
					+ Integer.toHexString(opcode) + " is not supported.");
		}
	}

	private void decodeExtendedOperation(int opcode)
			throws UnsupportedOpcodeException, IllegalMemoryAccessException,
			IllegalRegisterAccessException {
		switch (opcode) {
		case 0x00:
			// RLC B
			r.set(REGISTERS.B, alu.rotateLeftCarry(r.get(REGISTERS.B)));
			break;
		case 0x01:
			// RLC C
			r.set(REGISTERS.C, alu.rotateLeftCarry(r.get(REGISTERS.C)));
			break;
		case 0x02:
			// RLC D
			r.set(REGISTERS.D, alu.rotateLeftCarry(r.get(REGISTERS.D)));
			break;
		case 0x03:
			// RLC E
			r.set(REGISTERS.E, alu.rotateLeftCarry(r.get(REGISTERS.E)));
			break;
		case 0x04:
			// RLC H
			r.set(REGISTERS.H, alu.rotateLeftCarry(r.get(REGISTERS.H)));
			break;
		case 0x05:
			// RLC L
			r.set(REGISTERS.L, alu.rotateLeftCarry(r.get(REGISTERS.L)));
			break;
		case 0x06:
			// RLC (HL)
			m.writeByte(r.getWord(REGISTERS.HL),
					alu.rotateLeftCarry(m.readByte(r.getWord(REGISTERS.HL))));
			break;
		case 0x07:
			// RLC A
			r.set(REGISTERS.A, alu.rotateLeftCarry(r.get(REGISTERS.A)));
			break;
		case 0x08:
			// RRC B
			r.set(REGISTERS.B, alu.rotateRightCarry(r.get(REGISTERS.B)));
			break;
		case 0x09:
			// RRC C
			r.set(REGISTERS.C, alu.rotateRightCarry(r.get(REGISTERS.C)));
			break;
		case 0x0A:
			// RRC D
			r.set(REGISTERS.D, alu.rotateRightCarry(r.get(REGISTERS.D)));
			break;
		case 0x0B:
			// RRC E
			r.set(REGISTERS.E, alu.rotateRightCarry(r.get(REGISTERS.E)));
			break;
		case 0x0C:
			// RLC H
			r.set(REGISTERS.H, alu.rotateLeftCarry(r.get(REGISTERS.H)));
			break;
		case 0x0D:
			// RRC L
			r.set(REGISTERS.L, alu.rotateRightCarry(r.get(REGISTERS.L)));
			break;
		case 0x0E:
			// RRC (HL)
			m.writeByte(r.getWord(REGISTERS.HL),
					alu.rotateRightCarry(m.readByte(r.getWord(REGISTERS.HL))));
			break;
		case 0x0F:
			// RRC A
			r.set(REGISTERS.A, alu.rotateRightCarry(r.get(REGISTERS.A)));
			break;
		case 0x10:
			// RL B
			r.set(REGISTERS.B, alu.rotateLeft(r.get(REGISTERS.B)));
			break;
		case 0x11:
			// RL C
			r.set(REGISTERS.C, alu.rotateLeft(r.get(REGISTERS.C)));
			break;
		case 0x12:
			// RL D
			r.set(REGISTERS.D, alu.rotateLeft(r.get(REGISTERS.D)));
			break;
		case 0x13:
			// RL E
			r.set(REGISTERS.E, alu.rotateLeft(r.get(REGISTERS.E)));
			break;
		case 0x14:
			// RL H
			r.set(REGISTERS.H, alu.rotateLeft(r.get(REGISTERS.H)));
			break;
		case 0x15:
			// RL L
			r.set(REGISTERS.L, alu.rotateLeft(r.get(REGISTERS.L)));
			break;
		case 0x16:
			// RL (HL)
			m.writeByte(r.getWord(REGISTERS.HL),
					alu.rotateLeft(m.readByte(r.getWord(REGISTERS.HL))));
			break;
		case 0x17:
			// RL A
			r.set(REGISTERS.A, alu.rotateLeft(r.get(REGISTERS.A)));
			break;
		case 0x18:
			// RR B
			r.set(REGISTERS.B, alu.rotateRight(r.get(REGISTERS.B)));
			break;
		case 0x19:
			// RR C
			r.set(REGISTERS.C, alu.rotateRight(r.get(REGISTERS.C)));
			break;
		case 0x1A:
			// RR D
			r.set(REGISTERS.D, alu.rotateRight(r.get(REGISTERS.D)));
			break;
		case 0x1B:
			// RR E
			r.set(REGISTERS.E, alu.rotateRight(r.get(REGISTERS.E)));
			break;
		case 0x1C:
			// RL H
			r.set(REGISTERS.H, alu.rotateLeft(r.get(REGISTERS.H)));
			break;
		case 0x1D:
			// RR L
			r.set(REGISTERS.L, alu.rotateRight(r.get(REGISTERS.L)));
			break;
		case 0x1E:
			// RR (HL)
			m.writeByte(r.getWord(REGISTERS.HL),
					alu.rotateRight(m.readByte(r.getWord(REGISTERS.HL))));
			break;
		case 0x1F:
			// RR A
			r.set(REGISTERS.A, alu.rotateRight(r.get(REGISTERS.A)));
			break;
		case 0x20:
			// SLA B
			r.set(REGISTERS.B, alu.shiftLeftSigned(r.get(REGISTERS.B)));
			break;
		case 0x21:
			// SLA C
			r.set(REGISTERS.C, alu.shiftLeftSigned(r.get(REGISTERS.C)));
			break;
		case 0x22:
			// SLA D
			r.set(REGISTERS.D, alu.shiftLeftSigned(r.get(REGISTERS.D)));
			break;
		case 0x23:
			// SLA E
			r.set(REGISTERS.E, alu.shiftLeftSigned(r.get(REGISTERS.E)));
			break;
		case 0x24:
			// SLA H
			r.set(REGISTERS.H, alu.shiftLeftSigned(r.get(REGISTERS.H)));
			break;
		case 0x25:
			// SLA L
			r.set(REGISTERS.L, alu.shiftLeftSigned(r.get(REGISTERS.L)));
			break;
		case 0x26:
			// SLA (HL)
			m.writeByte(r.getWord(REGISTERS.HL),
					alu.shiftLeftSigned(m.readByte(r.getWord(REGISTERS.HL))));
			break;
		case 0x27:
			// SLA A
			r.set(REGISTERS.A, alu.shiftLeftSigned(r.get(REGISTERS.A)));
			break;
		case 0x28:
			// SRA B
			r.set(REGISTERS.B, alu.shiftRightSigned(r.get(REGISTERS.B)));
			break;
		case 0x29:
			// SRA C
			r.set(REGISTERS.C, alu.shiftRightSigned(r.get(REGISTERS.C)));
			break;
		case 0x2A:
			// SRA D
			r.set(REGISTERS.D, alu.shiftRightSigned(r.get(REGISTERS.D)));
			break;
		case 0x2B:
			// SRA E
			r.set(REGISTERS.E, alu.shiftRightSigned(r.get(REGISTERS.E)));
			break;
		case 0x2C:
			// SRA H
			r.set(REGISTERS.H, alu.shiftRightSigned(r.get(REGISTERS.H)));
			break;
		case 0x2D:
			// SRA L
			r.set(REGISTERS.L, alu.shiftRightSigned(r.get(REGISTERS.L)));
			break;
		case 0x2E:
			// SRA (HL)
			m.writeByte(r.getWord(REGISTERS.HL),
					alu.shiftRightSigned(m.readByte(r.getWord(REGISTERS.HL))));
			break;
		case 0x2F:
			// SRA A
			r.set(REGISTERS.A, alu.shiftRightSigned(r.get(REGISTERS.A)));
			break;
		case 0x30:
			// SWAP B
			r.set(REGISTERS.B, alu.swap(r.get(REGISTERS.B)));
			break;
		case 0x31:
			// SWAP C
			r.set(REGISTERS.C, alu.swap(r.get(REGISTERS.C)));
			break;
		case 0x32:
			// SWAP D
			r.set(REGISTERS.D, alu.swap(r.get(REGISTERS.D)));
			break;
		case 0x33:
			// SWAP E
			r.set(REGISTERS.E, alu.swap(r.get(REGISTERS.E)));
			break;
		case 0x34:
			// SWAP H
			r.set(REGISTERS.H, alu.swap(r.get(REGISTERS.H)));
			break;
		case 0x35:
			// SWAP L
			r.set(REGISTERS.L, alu.swap(r.get(REGISTERS.L)));
			break;
		case 0x36:
			// SWAP (HL)
			m.writeByte(r.getWord(REGISTERS.HL),
					alu.swap(m.readByte(r.getWord(REGISTERS.HL))));
			break;
		case 0x37:
			// SWAP A
			r.set(REGISTERS.A, alu.swap(r.get(REGISTERS.A)));
			break;
		case 0x38:
			// SRL B
			r.set(REGISTERS.B, alu.shiftRight(r.get(REGISTERS.B)));
			break;
		case 0x39:
			// SRL C
			r.set(REGISTERS.C, alu.shiftRight(r.get(REGISTERS.C)));
			break;
		case 0x3A:
			// SRL D
			r.set(REGISTERS.D, alu.shiftRight(r.get(REGISTERS.D)));
			break;
		case 0x3B:
			// SRL E
			r.set(REGISTERS.E, alu.shiftRight(r.get(REGISTERS.E)));
			break;
		case 0x3C:
			// SRL H
			r.set(REGISTERS.H, alu.shiftRight(r.get(REGISTERS.H)));
			break;
		case 0x3D:
			// SRL L
			r.set(REGISTERS.L, alu.shiftRight(r.get(REGISTERS.L)));
			break;
		case 0x3E:
			// SRL (HL)
			m.writeByte(r.getWord(REGISTERS.HL),
					alu.shiftRight(m.readByte(r.getWord(REGISTERS.HL))));
			break;
		case 0x3F:
			// SRL A
			r.set(REGISTERS.A, alu.shiftRight(r.get(REGISTERS.A)));
			break;
		case 0x40:
		case 0x41:
		case 0x42:
		case 0x43:
		case 0x44:
		case 0x45:
		case 0x46:
		case 0x47:
		case 0x48:
		case 0x49:
		case 0x4A:
		case 0x4B:
		case 0x4C:
		case 0x4D:
		case 0x4E:
		case 0x4F:
		case 0x50:
		case 0x51:
		case 0x52:
		case 0x53:
		case 0x54:
		case 0x55:
		case 0x56:
		case 0x57:
		case 0x58:
		case 0x59:
		case 0x5A:
		case 0x5B:
		case 0x5C:
		case 0x5D:
		case 0x5E:
		case 0x5F:
		case 0x60:
		case 0x61:
		case 0x62:
		case 0x63:
		case 0x64:
		case 0x65:
		case 0x66:
		case 0x67:
		case 0x68:
		case 0x69:
		case 0x6A:
		case 0x6B:
		case 0x6C:
		case 0x6D:
		case 0x6E:
		case 0x6F:
		case 0x70:
		case 0x71:
		case 0x72:
		case 0x73:
		case 0x74:
		case 0x75:
		case 0x76:
		case 0x77:
		case 0x78:
		case 0x79:
		case 0x7A:
		case 0x7B:
		case 0x7C:
		case 0x7D:
		case 0x7E:
		case 0x7F:
			bitTest(opcode);
			break;
		case 0x80:
		case 0x81:
		case 0x82:
		case 0x83:
		case 0x84:
		case 0x85:
		case 0x86:
		case 0x87:
		case 0x88:
		case 0x89:
		case 0x8A:
		case 0x8B:
		case 0x8C:
		case 0x8D:
		case 0x8E:
		case 0x8F:
		case 0x90:
		case 0x91:
		case 0x92:
		case 0x93:
		case 0x94:
		case 0x95:
		case 0x96:
		case 0x97:
		case 0x98:
		case 0x99:
		case 0x9A:
		case 0x9B:
		case 0x9C:
		case 0x9D:
		case 0x9E:
		case 0x9F:
		case 0xA0:
		case 0xA1:
		case 0xA2:
		case 0xA3:
		case 0xA4:
		case 0xA5:
		case 0xA6:
		case 0xA7:
		case 0xA8:
		case 0xA9:
		case 0xAA:
		case 0xAB:
		case 0xAC:
		case 0xAD:
		case 0xAE:
		case 0xAF:
		case 0xB0:
		case 0xB1:
		case 0xB2:
		case 0xB3:
		case 0xB4:
		case 0xB5:
		case 0xB6:
		case 0xB7:
		case 0xB8:
		case 0xB9:
		case 0xBA:
		case 0xBB:
		case 0xBC:
		case 0xBD:
		case 0xBE:
		case 0xBF:
			resetBit(opcode);
			break;
		case 0xC0:
		case 0xC1:
		case 0xC2:
		case 0xC3:
		case 0xC4:
		case 0xC5:
		case 0xC6:
		case 0xC7:
		case 0xC8:
		case 0xC9:
		case 0xCA:
		case 0xCB:
		case 0xCC:
		case 0xCD:
		case 0xCE:
		case 0xCF:
		case 0xD0:
		case 0xD1:
		case 0xD2:
		case 0xD3:
		case 0xD4:
		case 0xD5:
		case 0xD6:
		case 0xD7:
		case 0xD8:
		case 0xD9:
		case 0xDA:
		case 0xDB:
		case 0xDC:
		case 0xDD:
		case 0xDE:
		case 0xDF:
		case 0xE0:
		case 0xE1:
		case 0xE2:
		case 0xE3:
		case 0xE4:
		case 0xE5:
		case 0xE6:
		case 0xE7:
		case 0xE8:
		case 0xE9:
		case 0xEA:
		case 0xEB:
		case 0xEC:
		case 0xED:
		case 0xEE:
		case 0xEF:
		case 0xF0:
		case 0xF1:
		case 0xF2:
		case 0xF3:
		case 0xF4:
		case 0xF5:
		case 0xF6:
		case 0xF7:
		case 0xF8:
		case 0xF9:
		case 0xFA:
		case 0xFB:
		case 0xFC:
		case 0xFD:
		case 0xFE:
		case 0xFF:
			setBit(opcode);
			break;
		default:
			throw new UnsupportedOpcodeException("Opcode "
					+ Integer.toHexString(opcode) + " not supported.");
		}
	}

	private int nextInstruction() throws IllegalMemoryAccessException {

		return m.readByte(pc++);
	}

	private void bitTest(int opcode) throws IllegalMemoryAccessException {
		REGISTERS register = REGISTERS.A;
		switch (opcode & 0x0F) {
		case 0x00:
		case 0x08:
			register = REGISTERS.B;
			break;
		case 0x01:
		case 0x09:
			register = REGISTERS.C;
			break;
		case 0x02:
		case 0x0A:
			register = REGISTERS.D;
			break;
		case 0x03:
		case 0x0B:
			register = REGISTERS.E;
			break;
		case 0x04:
		case 0x0C:
			register = REGISTERS.H;
			break;
		case 0x05:
		case 0x0D:
			register = REGISTERS.L;
			break;
		case 0x06:
		case 0x0E:
			register = REGISTERS.HL;
			break;
		case 0x07:
		case 0x0F:
			register = REGISTERS.A;
			break;
		}
		int bit = 0;
		switch (opcode & 0xF0) {
		case 0x40:
			bit = 0;
			break;
		case 0x50:
			bit = 2;
			break;
		case 0x60:
			bit = 4;
			break;
		case 0x70:
			bit = 6;
			break;
		}
		if ((opcode & 0x0F) > 0x07) {
			bit++;
		}
		if (register != REGISTERS.HL) {
			alu.bitTest(bit, r.get(register));
		} else {
			alu.bitTest(bit, m.readByte(r.getWord(REGISTERS.HL)));
		}
	}

	private void resetBit(int opcode) throws IllegalMemoryAccessException,
			IllegalRegisterAccessException {
		REGISTERS register = REGISTERS.A;
		switch (opcode & 0x0F) {
		case 0x00:
		case 0x08:
			register = REGISTERS.B;
			break;
		case 0x01:
		case 0x09:
			register = REGISTERS.C;
			break;
		case 0x02:
		case 0x0A:
			register = REGISTERS.D;
			break;
		case 0x03:
		case 0x0B:
			register = REGISTERS.E;
			break;
		case 0x04:
		case 0x0C:
			register = REGISTERS.H;
			break;
		case 0x05:
		case 0x0D:
			register = REGISTERS.L;
			break;
		case 0x06:
		case 0x0E:
			register = REGISTERS.HL;
			break;
		case 0x07:
		case 0x0F:
			register = REGISTERS.A;
			break;
		}
		int bit = 0;
		switch (opcode & 0xF0) {
		case 0x40:
			bit = 0;
			break;
		case 0x50:
			bit = 2;
			break;
		case 0x60:
			bit = 4;
			break;
		case 0x70:
			bit = 6;
			break;
		}
		if ((opcode & 0x0F) > 0x07) {
			bit++;
		}
		if (register != REGISTERS.HL) {
			r.set(register, alu.resetBit(bit, r.get(register)));
		} else {
			r.set(register,
					alu.resetBit(bit, m.readByte(r.getWord(REGISTERS.HL))));
		}
	}

	private void setBit(int opcode) throws IllegalMemoryAccessException,
			IllegalRegisterAccessException {
		REGISTERS register = REGISTERS.A;
		switch (opcode & 0x0F) {
		case 0x00:
		case 0x08:
			register = REGISTERS.B;
			break;
		case 0x01:
		case 0x09:
			register = REGISTERS.C;
			break;
		case 0x02:
		case 0x0A:
			register = REGISTERS.D;
			break;
		case 0x03:
		case 0x0B:
			register = REGISTERS.E;
			break;
		case 0x04:
		case 0x0C:
			register = REGISTERS.H;
			break;
		case 0x05:
		case 0x0D:
			register = REGISTERS.L;
			break;
		case 0x06:
		case 0x0E:
			register = REGISTERS.HL;
			break;
		case 0x07:
		case 0x0F:
			register = REGISTERS.A;
			break;
		}
		int bit = 0;
		switch (opcode & 0xF0) {
		case 0x40:
			bit = 0;
			break;
		case 0x50:
			bit = 2;
			break;
		case 0x60:
			bit = 4;
			break;
		case 0x70:
			bit = 6;
			break;
		}
		if ((opcode & 0x0F) > 0x07) {
			bit++;
		}
		if (register != REGISTERS.HL) {
			r.set(register, alu.setBit(bit, r.get(register)));
		} else {
			r.set(register,
					alu.setBit(bit, m.readByte(r.getWord(REGISTERS.HL))));
		}
	}

	private void tick(int m) {
		clock.m += m;
		clock.t += m * 4;
		gpu.tick(m);
	}

}
